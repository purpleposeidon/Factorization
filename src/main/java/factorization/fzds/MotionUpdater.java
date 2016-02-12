package factorization.fzds;

import com.google.common.base.Predicate;
import factorization.api.Mat;
import factorization.api.Quaternion;
import factorization.coremodhooks.IKinematicTracker;
import factorization.fzds.interfaces.DeltaCapability;
import factorization.fzds.interfaces.IDCController;
import factorization.fzds.interfaces.IDimensionSlice;
import factorization.fzds.interfaces.transform.Pure;
import factorization.fzds.interfaces.transform.TransformData;
import factorization.util.NORELEASE;
import factorization.util.SpaceUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Vec3;

import java.util.Iterator;
import java.util.List;

class MotionUpdater {
    final DimensionSliceEntity dse;

    private MotionUpdater(DimensionSliceEntity dse) {
        this.dse = dse;
    }

    public static MotionUpdater create(DimensionSliceEntity dse) {
        return new MotionUpdater(dse);
    }

    void updateMotion(TransformData<Pure> parentTransform) {
        dse.transformPrevTick = dse.transform.copy();
        if (dse.metaAABB == null) {
            // This can happen if the shadow area isn't yet loaded.
            return;
        }
        if (!dse.order.isNull()) {
            TransformData<Pure> got = dse.order.tickTransform(dse);
            if (got == null) {
                dse.order = new NullOrder();
                dse.velocity = dse.order.removed(true);
            } else {
                dse.velocity = got;
            }
        }
        dse.accumVelocity = parentTransform.copy();
        dse.accumVelocity.multiply(dse.velocity);
        dse.transform.multiply(dse.accumVelocity);
        boolean isZero = dse.accumVelocity.isZero();
        if (!isZero) {
            Mat motion = dse.accumVelocity.getMotion();
            AxisAlignedBB blurredBounds = blurArea(motion);
            if (!dse.noClip && dse.can(DeltaCapability.COLLIDE_WITH_WORLD) && !dse.worldObj.isRemote) {
                collideWithWorld(blurredBounds, dse.accumVelocity, dse.transformPrevTick);
            }
            if (dse.can(DeltaCapability.DRAG)) {
                dragEntities(dse.accumVelocity, blurredBounds, motion);
            }
            dse.transportAreaUpdater.needsRealAreaUpdate = true;
        }
        updateVanilla();
        moveChildren(dse.accumVelocity);
    }

    private AxisAlignedBB blurArea(Mat mat) {
        AxisAlignedBB start = SpaceUtil.copy(dse.metaAABB);
        Vec3[] parts = SpaceUtil.getCorners(start);
        for (int i = 0; i < parts.length; i++) {
            parts[i] = mat.mul(parts[i]);
        }
        AxisAlignedBB end = SpaceUtil.newBox(parts);
        return start.union(end);
    }

    private void updateVanilla() {
        // The vanilla values are never used.
        SpaceUtil.toEntPos(dse, dse.transform.getPos());
        SpaceUtil.toEntVel(dse, dse.velocity.getPos());
    }

    private void moveChildren(TransformData<Pure> accum) {
        if (dse.children.isEmpty()) return;
        for (Iterator<IDimensionSlice> iterator = dse.children.iterator(); iterator.hasNext();) {
            DimensionSliceEntity child = (DimensionSliceEntity) iterator.next();
            if (child.isDead) {
                iterator.remove();
                continue;
            }
            // Imagine a ruler that's got a pivot at 0.
            // All units have the same rotation.
            // The translation of the Nth unit is equal to the lever-arm action from the parent + parent's translation
            // ... probably. :D


            // Errors accumulate, mainly during turns.
            Vec3 correctPos = dse.getShadow2Real().mul(child.parentShadowOrigin);
            Vec3 childAt = child.getTransform().getPos();

            Vec3 error = childAt.subtract(correctPos);

            TransformData<Pure> correctedAccum = accum.copy();
            correctedAccum.setPos(accum.getPos().add(error));
            new MotionUpdater(child).updateMotion(correctedAccum);
        }
    }

    private void dragEntities(TransformData<Pure> trans, AxisAlignedBB blur, Mat mat) {
        Vec3 vel = trans.getPos();
        boolean movingUp = vel.yCoord > 0;
        boolean movingSideways = vel.xCoord != 0 || vel.zCoord != 0;
        double speed = vel.lengthVector();
        double expansion = 0;
        double friction_expansion = 0.05 * speed;
        if (movingSideways | movingUp) {
            expansion = friction_expansion;
        }
        if (expansion < 0.1 && expansion > 0) {
            expansion = 0.1;
        }



        blur = blur.expand(expansion, expansion, expansion);
        List<Entity> ents = dse.worldObj.getEntitiesInAABBexcluding(dse, blur, excludeDseRelatedEntities);
        blur = null; // We must use the metaAABB from now on.
        float dyaw = (float) Math.toDegrees(-trans.getRot().toRotationVector().yCoord);
        if (Float.isNaN(dyaw)) dyaw = 0;
        long now = dse.worldObj.getTotalWorldTime() + 100 /* Hack around MixinEntityKinematicsTracker.kinematics_last_change not being initialized */;

        Vec3 origin = dse.transform.getPos();

        for (Entity ent : ents) {
            AxisAlignedBB ebb = ent.getEntityBoundingBox();
            if (expansion != 0) {
                ebb = ebb.expand(expansion, expansion, expansion);
            }
            // could multiply stuff by velocity
            if (!dse.metaAABB.intersectsWith(ebb)) {
                // NORELEASE metaAABB.intersectsWith is very slow, especially with lots of entities
                continue;
            }

            Vec3 start = SpaceUtil.fromEntPos(ent).subtract(origin);
            Vec3 end = mat.mul(start);
            Vec3 impulse = end.subtract(start);
            if (dse.can(DeltaCapability.ENTITY_PHYSICS)) {
                puntEntity(dyaw, now, ent, impulse);
            } else {
                ent.moveEntity(impulse.xCoord, impulse.yCoord, impulse.zCoord);

                if (impulse.yCoord > 0 && ent.motionY < impulse.yCoord) {
                    ent.motionY = impulse.yCoord;
                    ent.fallDistance += (float) Math.abs(impulse.yCoord - ent.motionY);
                }
            }
            ent.onGround = true;
        }
    }

    private static DamageSource puntDamage = new DamageSource("dseHit");
    private void puntEntity(float dyaw, long now, Entity ent, Vec3 impulse) {
        IKinematicTracker kine = (IKinematicTracker) ent;
        kine.reset(now);
        if (dse.can(DeltaCapability.VIOLENT_COLLISIONS) && !dse.worldObj.isRemote) {
            NORELEASE.fixme("It's the sudden stop at the end, not the speed.");
            // NORELEASE: 1.9 may have running into walls at high speeds as a damage source.
            double smackSpeed = impulse.lengthVector();
            double vel_scale = 1;
            if (smackSpeed > 0.05) {
                if (ent instanceof EntityLivingBase) {
                    EntityLivingBase el = (EntityLivingBase) ent;
                    el.attackEntityFrom(puntDamage, (float) (20 * smackSpeed));
                    Vec3 emo = impulse.normalize();
                    ent.motionX += emo.xCoord * vel_scale;
                    ent.motionY += emo.yCoord * vel_scale;
                    ent.motionZ += emo.zCoord * vel_scale;
                }
            }
        }
        /*if (ent.isAirBorne) {
            // 1: This should be using clipVelocity or something
            // 2: The player is always air-born client-side. :/
            // Or maybe an 'isJumping'? We've only got EntityLivingBase.isJumping, requires AT.
            if (impulse.xCoord > 0 && ent.motionX < impulse.xCoord
                    || impulse.xCoord < 0 && ent.motionX > impulse.xCoord) {
                ent.motionX = impulse.xCoord;
            }
            if (impulse.yCoord > 0 && ent.motionY < impulse.yCoord
                    || impulse.yCoord < 0 && ent.motionY > impulse.yCoord) {
                ent.motionY = impulse.yCoord;
            }
            if (impulse.zCoord > 0 && ent.motionZ < impulse.zCoord
                    || impulse.zCoord < 0 && ent.motionZ > impulse.zCoord) {
                ent.motionZ = impulse.zCoord;
            }
        } else*/ {
            ent.moveEntity(impulse.xCoord, impulse.yCoord, impulse.zCoord);
        }
        /*
        double impulseX = clipVelocity(impulse.xCoord, ent.motionX);
        double impulseY = clipVelocity(impulse.yCoord, ent.motionY);
        double impulseZ = clipVelocity(impulse.zCoord, ent.motionZ);
        if (ent.isAirBorne) {
            ent.motionX += impulseX;
            ent.motionY += impulseY;
            ent.motionZ += impulseZ;
        } else {
            ent.moveEntity(impulseX, impulseY, impulseZ);
        }*/
        // Hrm. Is it needed or not? Seems to cause jitterings with it on
        //ent.prevPosX += impulseX;
        //ent.prevPosY += impulseY;
        //ent.prevPosZ += impulseZ;
        // TODO FIXME: Jittering rotation when the player is standing on top! Argh! PLEASE FIX!
        double origYaw = ent.rotationYaw;
        ent.rotationYaw = (float) addLimitedDelta(kine.getKinematics_yaw(), ent.rotationYaw, dyaw);
        double yd = ent.rotationYaw - origYaw;
        ent.prevRotationYaw += yd;
    }

    public boolean collideWithWorld(AxisAlignedBB blurredBounds, TransformData<Pure> accumVelocity, TransformData<Pure> transformPrevTick) {
        // TODO What if we 'rendered' all the collision boxes to a 3D boolean array? (3D BitSet)
        // Could be a low-resolution rendering.
        List<AxisAlignedBB> collisions = dse.worldObj.getCollidingBoundingBoxes(dse, blurredBounds); // FIXME: SLOW!
        NORELEASE.fixme("Possible easy fix: Don't create a list. Remember to include both entities & blocks.");
        AxisAlignedBB collision = null;
        IDCController.CollisionAction action = IDCController.CollisionAction.IGNORE;
        for (AxisAlignedBB solid : collisions) {
            // FIXME: SLOW!
            if (solid == dse.metaAABB) continue;
            if (solid.getAverageEdgeLength() > 4) {
                if (solid.getClass() != AxisAlignedBB.class) continue;
            }
            AxisAlignedBB hit = dse.metaAABB.intersectsWithGet(solid);
            if (hit != null) {
                action = dse.controller.collidedWithWorld(dse.getRealWorld(), solid, dse.getShadowWorld(), hit);
                if (action == IDCController.CollisionAction.IGNORE) {
                    continue;
                }
                collision = solid;
                break;
            }
        }
        if (collision == null) return false;
        if (action == IDCController.CollisionAction.STOP_BEFORE) {
            dse.transform = transformPrevTick;
        }
        dse.velocity.setPos(new Vec3(0, 0, 0));
        dse.velocity.setRot(new Quaternion());
        return false;
    }

    /**
     * If the player is standing on two platforms moving in the same direction, then the natural behavior is for the player to move twice as fast.
     */
    double addLimitedDelta(double prevVal, double currentVal, double delta) {
        if (delta == 0) return currentVal;
        double oldDelta = currentVal - prevVal;
        if (oldDelta != 0 && Math.signum(oldDelta) != Math.signum(delta)) return currentVal; // First DSE wins
        if (delta > 0) {
            return prevVal + Math.max(delta, oldDelta);
        } else {
            return prevVal + Math.min(delta, oldDelta);
        }
    }

    double clipVelocity(double impulse_velocity, double current_velocity) {
        if (impulse_velocity < 0) {
            return Math.min(impulse_velocity, current_velocity);
        } else if (impulse_velocity > 0) {
            return Math.max(impulse_velocity, current_velocity);
        } else {
            return current_velocity;
        }
    }

    static final Predicate<Entity> excludeDseRelatedEntities = new Predicate<Entity>() {
        @Override
        public boolean apply(Entity entity) {
            Class entClass = entity.getClass();
            if (entClass == DimensionSliceEntity.class) return false;
            return entClass != UniversalCollider.class;
        }
    };

}
