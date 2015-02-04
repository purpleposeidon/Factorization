package factorization.colossi;

import static factorization.colossi.TechniqueKind.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import factorization.aabbdebug.AabbDebugger;
import factorization.api.DeltaCoord;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.item.EntityFireworkRocket;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.ForgeDirection;
import factorization.api.Coord;
import factorization.api.ICoordFunction;
import factorization.api.Quaternion;
import factorization.colossi.ColossusController.BodySide;
import factorization.colossi.ColossusController.LimbType;
import factorization.fzds.TransferLib;
import factorization.fzds.interfaces.DeltaCapability;
import factorization.fzds.interfaces.IDeltaChunk;
import factorization.fzds.interfaces.Interpolation;
import factorization.shared.Core;
import factorization.shared.FzUtil;
import factorization.shared.NORELEASE;
import factorization.shared.ReservoirSampler;

public enum Technique implements IStateMachine<Technique> {
    STATE_MACHINE_ENTRY {
        @Override
        TechniqueKind getKind() {
            return TRANSITION;
        }

        @Override
        public Technique tick(ColossusController controller, int age) {
            return INITIAL_BOW;
        }
    },
    
    PICK_NEXT_TECHNIQUE {
        @Override
        TechniqueKind getKind() {
            return IDLER;
        }
        
        @Override
        public Technique tick(ColossusController controller, int age) {
            if (controller.getHealth() <= 0) return DEATH_FALL;
            if (age == 0) {
                Vec3 rot = controller.body.getRotation().toRotationVector();
                double err = Math.abs(rot.xCoord) + Math.abs(rot.zCoord);
                if (err > 0.01) {
                    Quaternion bodyRot = controller.body.getRotation();
                    double yRot = bodyRot.toRotationVector().yCoord;
                    Quaternion straightRot = Quaternion.getRotationQuaternionRadians(yRot, ForgeDirection.UP);
                    controller.bodyLimbInfo.target(straightRot, 1);
                    return FINISH_MOVE;
                }
            }
            boolean use_defense = controller.checkHurt(true);
            List<Technique> avail = Arrays.asList(Technique.values());
            Collections.shuffle(avail);
            Technique chosen_offense = null;
            Technique chosen_defense = null;
            boolean force_idler = iteratePotentialPlayers(controller) == null;
            for (Technique tech : avail) {
                TechniqueKind kind = tech.getKind();
                if (use_defense && kind != DEFENSIVE) continue;
                switch (kind) {
                    case TRANSITION:
                        continue;
                    case OFFENSIVE:
                        chosen_offense = grade(chosen_offense, controller, tech);
                        if (chosen_offense != null) {
                            return chosen_offense;
                        }
                        break;
                    case DEFENSIVE:
                    case IDLER:
                        chosen_defense = grade(chosen_defense, controller, tech);
                        break;
                }
            }
            if (chosen_defense != null) return chosen_defense;
            return STAND_STILL; // Shouldn't happen. Delay for a bit.
        }
        
        Technique grade(Technique orig, ColossusController controller, Technique next) {
            if (orig == null && next.usable(controller)) return next;
            return orig;
        }

        @Override
        protected Object visitPlayer(EntityPlayer player, ColossusController controller) {
            return player;
        }
    },
    
    STAND_STILL {
        @Override
        TechniqueKind getKind() {
            return IDLER;
        }
        
        @Override
        public void onEnterState(ColossusController controller, Technique prevState) {
            // Reset everything, but set the body's rotation to its y-axis rotation

            Quaternion bodyRot = controller.body.getRotation();
            double yRot = bodyRot.toRotationVector().yCoord;
            Quaternion straightRot = Quaternion.getRotationQuaternionRadians(yRot, ForgeDirection.UP);
            controller.bodyLimbInfo.target(straightRot, 1);
            int time = controller.bodyLimbInfo.idc.getEntity().getRemainingRotationTime();

            for (LimbInfo li : controller.limbs) {
                if (li.type.isArmOrLeg()) {
                    Quaternion or = li.idc.getEntity().getRotation();
                    li.idc.getEntity().orderTargetRotation(or, time, Interpolation.SMOOTH3);
                    //li.target(or.conjugate(), 1);
                    li.target(new Quaternion(), 1);
                }
            }


            
            // The above is way better than the commented out stuff! Keep for educational purposes!
            
            /*
            Quaternion bodyRot = controller.body.getRotation();
            Quaternion up = Quaternion.getRotationQuaternionRadians(0, ForgeDirection.UP);
            double tiltAngle = bodyRot.dotProduct(up);
            Vec3 right = bodyRot.cross(up).toVector().normalize();
            Quaternion correction = Quaternion.getRotationQuaternionRadians(tiltAngle, right);
            Quaternion newBod = bodyRot.multiply(correction);
            newBod.incrNormalize(); // Not normalizing causes limbs to de-joint?
            controller.bodyLimbInfo.target(newBod, 1);*/
        }

        @Override
        public Technique tick(ColossusController controller, int age) {
            if (NORELEASE.on) return finishMove(controller);
            if (age > 20 * 15) return PICK_NEXT_TECHNIQUE;
            if (controller.checkHurt(false)) return PICK_NEXT_TECHNIQUE;
            return this;
        }
    },
    
    FINISH_MOVE {
        @Override
        TechniqueKind getKind() {
            return TRANSITION;
        }
        
        @Override
        public Technique tick(ColossusController controller, int age) {
            return finishMove(controller);
        }
    },
    
    INITIAL_BOW {
        @Override
        TechniqueKind getKind() {
            return TRANSITION;
        }
        
        @Override
        public void onEnterState(ColossusController controller, Technique prevState) {
            BOW.onEnterState(controller, prevState);
            // Crack a mask blocks that is exposed UP but not EAST
            final ReservoirSampler<Coord> sampler = new ReservoirSampler<Coord>(1, controller.worldObj.rand);
            Coord.iterateCube(controller.body.getCorner(), controller.body.getFarCorner(), new ICoordFunction() {
                @Override
                public void handle(Coord here) {
                    if (here.getBlock() != Core.registry.colossal_block) return;
                    if (here.getMd() != ColossalBlock.MD_MASK) return;
                    if (!here.add(ForgeDirection.UP).isAir()) return;
                    if (here.add(ForgeDirection.EAST).isAir()) return;
                    sampler.give(here.copy());
                }
            });
            for (Coord found : sampler) {
                // NORELEASE: We might want a "cracked mask"
                found.setIdMd(Core.registry.colossal_block, ColossalBlock.MD_BODY_CRACKED, true);
                return;
            }
            // No suitable mask to crack? Might be a custom design. Silently skip this bow then.
            controller.crackBroken();
        }
        
        @Override
        public Technique tick(ColossusController controller, int age) {
            // Copy of BOW.tick >_>
            if (controller.checkHurt(false)) return UNBOW;
            return this; // Because of this!
        }
        
        @Override
        public void onExitState(ColossusController controller, Technique nextState) {
            // Add the other cracks
            int count = controller.getNaturalCrackCount();
            final ReservoirSampler<Coord> sampler = new ReservoirSampler<Coord>(count, controller.worldObj.rand);
            Coord.iterateCube(controller.body.getCorner(), controller.body.getFarCorner(), new ICoordFunction() {
                @Override
                public void handle(Coord here) {
                    if (isExposedSkin(here)) {
                        sampler.give(here.copy());
                    }
                }
            });
            for (Coord found : sampler) {
                found.setIdMd(Core.registry.colossal_block, ColossalBlock.MD_BODY_CRACKED /* Unlike the case above, we DO want MD_BODY_CRACKED. I know you're going to mess this up. Don't do it. */, true);
            }
            int newCracks = sampler.size();
            int destroyed = controller.getDestroyedCracks();
            controller.setTotalCracks(newCracks + destroyed);
        }
        
        boolean isExposedSkin(Coord cell) {
            if (cell.getBlock() != Core.registry.colossal_block) return false;
            if (cell.getMd() != ColossalBlock.MD_BODY) return false;
            for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
                Coord n = cell.add(dir);
                if (n.isAir() || n.isReplacable()) return true;
            }
            return false;
        }
    },
    
    BOW {
        @Override
        TechniqueKind getKind() {
            return OFFENSIVE;
        }
        
        @Override
        boolean usable(ColossusController controller) {
            if (controller.worldObj.rand.nextFloat() < 0.5) return false; 
            return iteratePotentialPlayers(controller) != null;
        }
        
        @Override
        protected Object visitPlayer(EntityPlayer player, ColossusController controller) {
            if (player.posY < controller.posY) return null;
            double height = controller.bodyLimbInfo.length;
            if (player.posY > controller.posY + height + 5) return null; // cheap coarse-check if the player is far above us
            Coord top = controller.body.getFarCorner().copy();
            controller.body.shadow2real(top);
            if (player.posY > top.y) return null;
            double radius = getTotalSize(controller) + 2;
            if (controller.getDistanceSqToEntity(player) > radius * radius) return null;
            return player;
        }
        
        double getTotalSize(ColossusController controller) {
            int bodWidth = measureWidth(controller.body);
            int armWidth = measureWidth(controller.body);
            return (bodWidth + armWidth) / 2.0;
        }
        
        int measureWidth(IDeltaChunk bod) {
            return bod.getFarCorner().difference(bod.getCorner()).z;
        }
        
        @Override
        public void onEnterState(ColossusController controller, Technique prevState) {
            // So, uh, we really should do some IK here. But that's rather more math than I want to deal with. O.o
            // So body bends to 90°, arms bend to 45°, and it might clip through the ground or be too high up or something.
            // (And the legs bend -90° since they're rooted to the body)
            // And hopefully we're bipedal! It'd do something hilarious & derpy if quadrapedal or polypedal.
            double bowAngle = Math.toRadians(70);
            Quaternion bow = Quaternion.getRotationQuaternionRadians(bowAngle, ForgeDirection.NORTH);
            Quaternion bodyBend = controller.body.getRotation().multiply(bow);
            controller.bodyLimbInfo.target(bodyBend, bow_power, bendInterp);
            int bodyBendTime;
            if (controller.body.hasOrderedRotation()) {
                bodyBendTime = controller.body.getRemainingRotationTime();
            } else {
                bodyBendTime = 60; // Hmph! Make something up. Shouldn't happen.
            }
            Quaternion legBend = Quaternion.getRotationQuaternionRadians(-bowAngle * 1.5, ForgeDirection.NORTH);
            for (LimbInfo limb : controller.limbs) {
                IDeltaChunk idc = limb.idc.getEntity();
                if (idc == null) continue;
                if (limb.type == LimbType.LEG) {
                    idc.orderTargetRotation(legBend, bodyBendTime, bendInterp);
                } else if (limb.type == LimbType.ARM) {
                    double armFlap = Math.toRadians(limb.side == BodySide.RIGHT ? -25 : 25);
                    double armHang = Math.toRadians(-90 - 45);
                    Quaternion flap = Quaternion.getRotationQuaternionRadians(armFlap, ForgeDirection.EAST);
                    Quaternion hang = Quaternion.getRotationQuaternionRadians(armHang, ForgeDirection.NORTH);
                    idc.orderTargetRotation(flap.multiply(hang).multiply(bow), bodyBendTime, Interpolation.SMOOTH);
                }
            }
            controller.setTarget(null);
        }
        
        @Override
        public Technique tick(ColossusController controller, int age) {
            if (controller.checkHurt(false)) return UNBOW;
            if (age > 20 * 15) return UNBOW;
            return this;
        }
    },
    
    UNBOW {
        @Override
        TechniqueKind getKind() {
            return TRANSITION;
        }
        
        @Override
        public void onEnterState(ColossusController controller, Technique prevState) {
            Quaternion bodyRot = controller.body.getRotation();
            double yRot = bodyRot.toRotationVector().yCoord;
            Quaternion straightRot = Quaternion.getRotationQuaternionRadians(yRot, ForgeDirection.UP);
            if (bodyRot.dotProduct(straightRot) < 0) {
                // Sometimes seems to go the long way 'round; this should make it short
                straightRot.incrConjugate();
            }
            controller.bodyLimbInfo.target(straightRot, 1);

            Quaternion straightenIsh = new Quaternion().slerp(bodyRot, 0.5);
            int time = controller.bodyLimbInfo.idc.getEntity().getRemainingRotationTime();
            for (LimbInfo li : controller.limbs) {
                if (li.type.isArmOrLeg()) {
                    // TODO/FIXME: Make the limbs end up normally rather than requiring STAND_STILL to fix it
                    li.idc.getEntity().orderTargetRotation(straightenIsh, time, Interpolation.SMOOTH);
                }
            }
        }
        
        @Override
        public Technique tick(ColossusController controller, int age) {
            return finishMove(controller);
        }
    },

    CHASE_PLAYER {
        @Override
        TechniqueKind getKind() {
            return OFFENSIVE;
        }

        @Override
        boolean usable(ColossusController controller) {
            return iteratePotentialPlayers(controller) != null;
        }

        @Override
        protected Object visitPlayer(EntityPlayer player, ColossusController controller) {
            if (!player.onGround) return CONTINUE;
            double dx = controller.posX - player.posX;
            double dz = controller.posZ - player.posZ;
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d < controller.leg_size + 2) return CONTINUE;
            return player;
        }

        @Override
        public void onEnterState(ColossusController controller, Technique prevState) {
            EntityPlayer player = iteratePotentialPlayers(controller);
            if (player == null) return;
            controller.setTarget(new Coord(player));
        }

        @Override
        public Technique tick(ColossusController controller, int age) {
            if (controller.atTarget()) return PICK_NEXT_TECHNIQUE;
            return this;
        }
    },

    SIT_DOWN {
        @Override
        TechniqueKind getKind() {
            return IDLER;
        }

        @Override
        boolean usable(ColossusController controller) {
            return iteratePotentialPlayers(controller) == null;
        }

        @Override
        protected Object visitPlayer(EntityPlayer player, ColossusController controller) {
            if (!player.onGround) return CONTINUE;
            return player;
        }

        @Override
        public void onEnterState(ColossusController controller, Technique prevState) {
            double v = controller.leg_length / (double) SIT_FALL_TIME;
            controller.body.setVelocity(0, -v, 0);
            Quaternion legBend = Quaternion.getRotationQuaternionRadians(Math.PI / 2, ForgeDirection.SOUTH);
            for (LimbInfo limb : controller.limbs) {
                if (limb.type == LimbType.LEG) {
                    limb.setTargetRotation(legBend, SIT_FALL_TIME, Interpolation.SMOOTH);
                }
                // TODO: Do something with the arms?
            }
        }

        @Override
        public void onExitState(ColossusController controller, Technique nextState) {
            controller.body.setVelocity(0, 0, 0);
        }

        @Override
        public Technique tick(ColossusController controller, int age) {
            if (age >= SIT_FALL_TIME) return SIT_WAIT;
            return this;
        }
    },

    SIT_WAIT {
        @Override
        TechniqueKind getKind() {
            return TRANSITION;
        }

        @Override
        public Technique tick(ColossusController controller, int age) {
            if (age % 60 == 0 && iteratePotentialPlayers(controller) != null) return STAND_UP;
            if (controller.checkHurt(false)) return STAND_UP;
            return this;
        }

        @Override
        protected Object visitPlayer(EntityPlayer player, ColossusController controller) {
            return player;
        }
    },

    STAND_UP {
        @Override
        TechniqueKind getKind() {
            return TRANSITION;
        }

        @Override
        public void onEnterState(ColossusController controller, Technique prevState) {
            double v = controller.leg_length / (double) SIT_FALL_TIME;
            controller.body.setVelocity(0, +v, 0);
            for (LimbInfo limb : controller.limbs) {
                if (limb.type.isArmOrLeg()) {
                    limb.setTargetRotation(new Quaternion(), SIT_FALL_TIME, Interpolation.SMOOTH);
                }
            }
        }

        @Override
        public Technique tick(ColossusController controller, int age) {
            if (age >= SIT_FALL_TIME) return PICK_NEXT_TECHNIQUE;
            return this;
        }

        @Override
        public void onExitState(ColossusController controller, Technique nextState) {
            controller.body.setVelocity(0, 0, 0);
        }
    },
    
    HIT_WITH_LIMB {
        @Override
        TechniqueKind getKind() {
            return OFFENSIVE;
        }
        
        @Override
        boolean usable(ColossusController controller) {
            return findSmashable(controller) != null;
        }
        
        @Override
        public void onEnterState(ColossusController controller, Technique prevState) {
            TargetSmash smash = findSmashable(controller);
            if (smash == null) return; // !!
            smash.limb.causesPain(true);
            smash.limb.target(smash.rotation, 6, Interpolation.CUBIC);
        }
        
        @Override
        public Technique tick(ColossusController controller, int age) {
            return finishMove(controller, FINISH_HIT);
        }
        
        TargetSmash findSmashable(ColossusController controller) {
            return iteratePotentialPlayers(controller);
        }
        
        @Override
        protected Object visitPlayer(EntityPlayer player, ColossusController controller) {
            DeltaCoord bodySize = controller.body.getFarCorner().difference(controller.body.getCorner());
            double halfBodyWidth = bodySize.x / 2;
            for (LimbInfo li : controller.limbs) {
                if (li.type != LimbType.ARM && li.type != LimbType.LEG) continue;
                IDeltaChunk idc = li.idc.getEntity();
                if (idc == null) continue;
                
                // So! We need to hit the player. There are some constraints on hitability. (These are sorted for efficiency)
                
                // If it's a leg, we won't kick too far up
                if (li.type == LimbType.LEG) {
                    if (player.posY > idc.posY) continue;
                }
                
                // The player has to be within a shell defined by the limb's length
                double farthest = li.length + 2;
                double nearest = li.length - 2;
                
                double dist = idc.getDistanceToEntity(player);
                if (dist > farthest || dist < nearest) continue;
                
                // We won't hit across the body
                Vec3 li2player = FzUtil.subtract(FzUtil.fromEntPos(player), FzUtil.fromEntPos(idc));
                Vec3 localOffset = FzUtil.copy(li2player);
                controller.body.getRotation().applyReverseRotation(localOffset);
                if (li.side == BodySide.LEFT) {
                    if (localOffset.zCoord > +halfBodyWidth) continue;
                } else {
                    if (localOffset.zCoord < -halfBodyWidth) continue;
                }
                
                // And striking backwards would be weird
                if (localOffset.xCoord < 0) continue;
                

                // The Quaternion needed to cause the limb to hit the player is the quaternion
                // that changes DOWN to the normalized direction.
                // The cross product of the two gives the axis of rotation,
                // and the dot product can be converted to
                Vec3 src = Vec3.createVectorHelper(0, -1, 0);
                Vec3 dst = li2player.normalize();
                Vec3 axis = src.crossProduct(dst);
                double angle = FzUtil.getAngle(src, dst);

                // A single strike through your oponnent will hurt him more than two hundred blows to his skin
                angle *= 1.25;

                Quaternion hitQuat = Quaternion.getRotationQuaternionRadians(angle, axis);

                TargetSmash smash = new TargetSmash();
                smash.limb = li;
                smash.rotation = hitQuat;
                return smash;
            }
            return CONTINUE;
        }
        
        class TargetSmash {
            LimbInfo limb;
            Quaternion rotation;
        }
    },
    
    FINISH_HIT {
        @Override
        TechniqueKind getKind() {
            return TRANSITION;
        }
        
        @Override
        public void onEnterState(ColossusController controller, Technique prevState) {
            for (LimbInfo li : controller.limbs) {
                if (!li.type.isArmOrLeg()) continue;
                li.causesPain(false);
                li.target(new Quaternion(), 1, Interpolation.SMOOTH);

            }
        }
        
        @Override
        public Technique tick(ColossusController controller, int age) {
            return finishMove(controller);
        }
    },
    
    LEAN_BACK_AND_FLAIL {
        @Override
        TechniqueKind getKind() {
            return TRANSITION; // lame technique; disabled
            //return DEFENSIVE;
        }
        
        @Override
        public void onEnterState(ColossusController controller, Technique prevState) {
            Quaternion lean = Quaternion.getRotationQuaternionRadians(Math.toRadians(20), ForgeDirection.SOUTH);
            Quaternion rot = controller.body.getRotation().multiply(lean);
            controller.bodyLimbInfo.target(rot, 4);
            // TODO Auto-generated method stub
            super.onEnterState(controller, prevState);
        }
        
        @Override
        public Technique tick(ColossusController controller, int age) {
            return finishMove(controller, FINISH_MOVE);
        }
        
        @Override
        public void onExitState(ColossusController controller, Technique nextState) {
            STAND_STILL.onEnterState(controller, this);
        }
    },
    
    SPIN_WINDUP {
        @Override
        TechniqueKind getKind() {
            return DEFENSIVE;
        }
        
        @Override
        public void onEnterState(ColossusController controller, Technique prevState) {
            // Hold arms to the $LEFT
            BodySide side = controller.worldObj.rand.nextBoolean() ? BodySide.LEFT : BodySide.RIGHT;
            BodySide oppositeSide = controller.spin_direction == BodySide.LEFT ? BodySide.RIGHT : BodySide.LEFT;
            //if (NORELEASE.on) side = BodySide.RIGHT;
            controller.spin_direction = side;
            for (LimbInfo li : controller.limbs) {
                targetLimb(li, side);
            }
            for (LimbInfo li : controller.limbs) {
                if (li.type == LimbType.LEG && li.side == oppositeSide) {
                    Quaternion rot = Quaternion.getRotationQuaternionRadians(Math.PI * 0.30, ForgeDirection.NORTH);
                    li.target(rot, 1, Interpolation.SMOOTH);
                    break; // Only lift 1 leg if not bipedal
                }
            }
        }
        
        @Override
        public Technique tick(ColossusController controller, int age) {
            return finishMove(controller, SPIN_UNWIND);
        }
    },
    
    SPIN_UNWIND {
        @Override
        TechniqueKind getKind() {
            return TRANSITION;
        }
        
        @Override
        public void onEnterState(ColossusController controller, Technique prevState) {
            // Swing arms to the $RIGHT, extend a $LEFT leg, rapidly spin body
            
            BodySide oppositeSide = controller.spin_direction == BodySide.LEFT ? BodySide.RIGHT : BodySide.LEFT;
            for (LimbInfo li : controller.limbs) {
                targetLimb(li, oppositeSide);
                if (li.type.isArmOrLeg()) li.causesPain(true);
            }
            double d = controller.spin_direction == BodySide.LEFT ? -1 : +1;
            Quaternion bod = controller.body.getRotation();
            Quaternion newBod = bod.multiply(Quaternion.getRotationQuaternionRadians(d * Math.PI * 1.75, ForgeDirection.UP));
            newBod.incrLongFor(bod);
            controller.bodyLimbInfo.target(newBod, 1F/8F, Interpolation.SMOOTHER);
        }
        
        @Override
        public Technique tick(ColossusController controller, int age) {
            return finishMove(controller, FINISH_MOVE);
            //return finishMove(controller, STAND_STILL);
        }
        
        @Override
        public void onExitState(ColossusController controller, Technique nextState) {
            for (LimbInfo li : controller.limbs) {
                li.causesPain(false);
            }
            STAND_STILL.onEnterState(controller, this);
        }
    },
    
    SHRUG {
        @Override
        TechniqueKind getKind() {
            return DEFENSIVE;
        }
        
        @Override
        public void onEnterState(ColossusController controller, Technique prevState) {
            for (LimbInfo li : controller.limbs) {
                IDeltaChunk idc = li.idc.getEntity();
                if (idc == null) continue;
                if (li.type != LimbType.ARM) continue;
                double angle = (li.side == BodySide.RIGHT ? -1 : +1) * Math.toRadians(90 + 45);
                Quaternion rot = Quaternion.getRotationQuaternionRadians(angle, ForgeDirection.EAST);
                li.target(rot, 3, Interpolation.SMOOTH);
            }
        }
        
        @Override
        public Technique tick(ColossusController controller, int age) {
            return finishMove(controller, UNSHRUG);
        }
    },
    
    UNSHRUG {
        @Override
        TechniqueKind getKind() {
            return TRANSITION;
        }
        
        @Override
        public void onEnterState(ColossusController controller, Technique prevState) {
            STAND_STILL.onEnterState(controller, prevState);
        }
        
        @Override
        public Technique tick(ColossusController controller, int age) {
            return finishMove(controller);
        }
    },
    
    WANDER {
        @Override
        TechniqueKind getKind() {
            return IDLER;
        }
        
        @Override
        public Technique tick(ColossusController controller, int age) {
            if (controller.atTarget()) return PICK_NEXT_TECHNIQUE;
            return this;
        }
        
        @Override
        public void onEnterState(ColossusController controller, Technique prevState) {
            double range = (WorldGenColossus.SMOOTH_START + WorldGenColossus.SMOOTH_END)/2;
            Coord target = controller.getHome().copy();
            double dx = rng(controller) * range;
            double dz = rng(controller) * range;
            target = target.add((int) dx, 0, (int) dz);
            controller.setTarget(target);
        }
        
        double rng(ColossusController controller) {
            return controller.worldObj.rand.nextDouble() * 2 - 1;
        }
    },
    
    DEATH_FALL {
        @Override
        TechniqueKind getKind() {
            return TRANSITION;
        }

        @Override
        boolean usable(ColossusController controller) {
            return controller.getHealth() <= 0;
        }

        @Override
        public void onEnterState(ColossusController controller, Technique prevState) {
            for (LimbInfo li : controller.limbs) {
                IDeltaChunk idc = li.idc.getEntity();
                idc.setVelocity(0, 0, 0);
                idc.setRotationalVelocity(new Quaternion());
                idc.permit(DeltaCapability.VIOLENT_COLLISIONS);
            }
            Quaternion fallAxis = Quaternion.getRotationQuaternionRadians(Math.PI / 2, ForgeDirection.SOUTH);
            Quaternion rotation = controller.body.getRotation();
            fallAxis = rotation.multiply(fallAxis);
            controller.bodyLimbInfo.target(fallAxis, 3, Interpolation.CUBIC);
            controller.setTarget(null);
        }
        
        @Override
        public Technique tick(ColossusController controller, int age) {
            return controller.body.hasOrderedRotation() ? this : DEATH_EXPLODE;
        }
        
        @Override
        public void onExitState(ColossusController controller, Technique nextState) {
            for (LimbInfo li : controller.limbs) {
                IDeltaChunk idc = li.idc.getEntity();
                idc.setVelocity(0, 0, 0);
                idc.setRotationalVelocity(new Quaternion());
            }
        }
    },
    
    DEATH_EXPLODE {
        @Override
        TechniqueKind getKind() {
            return TRANSITION;
        }
        
        @Override
        public Technique tick(ColossusController controller, int age) {
            if (age % 15 != 0) return this;
            boolean any = false;
            double n = 1 + (controller.leg_size / 2.0) * (age / 45);
            for (LimbInfo li : controller.limbs) {
                final ReservoirSampler<Coord> sampler = new ReservoirSampler<Coord>((int)n, null);
                IDeltaChunk idc = li.idc.getEntity();
                Coord.iterateCube(idc.getCorner(), idc.getFarCorner(), new ICoordFunction() {
                    @Override
                    public void handle(Coord here) {
                        if (here.isAir()) return;
                        if (here.getBlock() == Core.registry.colossal_block) {
                            int md = here.getMd();
                            if (md == ColossalBlock.MD_MASK) return;
                            if (md == ColossalBlock.MD_CORE) return;
                        }
                        sampler.give(here.copy());
                    }
                });
                for (Coord c : sampler) {
                    dislodge(idc, c);
                    any = true;
                }
            }
            if (any) {
                // Explosions can mess with our IDCs. :|
                for (LimbInfo li : controller.limbs) {
                    IDeltaChunk idc = li.idc.getEntity();
                    idc.motionX = idc.motionY = idc.motionZ = 0;
                }
            }
            return any ? this : DEATH_EXPIRE;
        }
        
        void dislodge(IDeltaChunk idc, Coord src) {
            Coord dest = src.copy();
            idc.shadow2real(dest);
            Block b = src.getBlock();
            int md = src.getMd();
            float explosionPower = 2F;
            float explodeChance = 0.125F;
            if (b == Core.registry.colossal_block && md == ColossalBlock.MD_EYE) {
                explodeChance = 1;
                explodeChance = 4;
            }
            if (src.w.rand.nextFloat() < explodeChance) {
                dest.w.createExplosion(null, dest.x + 0.5, dest.y + 0.5, dest.z + 0.5, explosionPower, false);
            }
            if (!dest.isReplacable() || src.getTE() != null) {
                src.breakBlock();
                src.setAir();
                return;
            }
            if (b == Core.registry.colossal_block || src.getHardness() <= 0) {
                src.setAir();
                return;
            }
            TransferLib.move(src, dest, true, true);
            EntityFallingBlock sand = new EntityFallingBlock(dest.w, dest.x, dest.y, dest.z, dest.getId(), dest.getMd());
            sand.field_145812_b = 1; // "Time" field. This is set to make it not suicide immediately.
            dest.setAir();
            double gs = 1.0/20.0;
            sand.motionX = 0; //dest.w.rand.nextGaussian() * gs;
            sand.motionZ = 0; //dest.w.rand.nextGaussian() * gs;
            sand.motionY = Math.abs(dest.w.rand.nextGaussian() * gs);
            sand.worldObj.spawnEntityInWorld(sand);
        }
    },
    
    DEATH_EXPIRE {
        @Override
        TechniqueKind getKind() {
            return TRANSITION;
        }
        
        @Override
        public Technique tick(ColossusController controller, int age) { return this; }
        
        @Override
        public void onEnterState(final ColossusController controller, Technique prevState) {
            final ArrayList<Entity> lmps = new ArrayList();
            for (final LimbInfo li : controller.limbs) {
                final IDeltaChunk idc = li.idc.getEntity();
                Coord min = idc.getCorner();
                Coord max = idc.getFarCorner();
                Coord.iterateCube(min, max, new ICoordFunction() {
                    @Override
                    public void handle(Coord here) {
                        if (here.getBlock() != Core.registry.colossal_block) return;
                        int md = here.getMd();
                        switch (md) {
                        default: return;
                        case ColossalBlock.MD_EYE:
                        case ColossalBlock.MD_BODY_CRACKED:
                        case ColossalBlock.MD_CORE:
                            here.setAir();
                            Vec3 core = idc.shadow2real(here.createVector().addVector(0.5, 0.5, 0.5));
                            controller.worldObj.newExplosion(null, core.xCoord, core.yCoord, core.zCoord, 0.25F, false, true);
                            if (md == ColossalBlock.MD_CORE) {
                                ItemStack lmp = new ItemStack(Core.registry.logicMatrixProgrammer);
                                EntityItem ei = new EntityItem(controller.worldObj, core.xCoord, core.yCoord, core.zCoord, lmp);
                                ei.invulnerable = true;
                                ei.motionY = 1;
                                lmps.add(ei);
                                EntityFireworkRocket flare = new EntityFireworkRocket(controller.worldObj, core.xCoord, core.yCoord, core.zCoord, null);
                                lmps.add(flare);
                            }
                            break;
                        case ColossalBlock.MD_MASK:
                            here.setAir();
                            Coord real = here.copy();
                            idc.shadow2real(real);
                            if (real.isReplacable()) {
                                EntityFallingBlock mask = new EntityFallingBlock(real.w, real.x, real.y, real.z, Core.registry.colossal_block, ColossalBlock.MD_MASK);
                                mask.field_145812_b = 1; // "Time" field. This is set to make it not suicide immediately.
                                lmps.add(mask);
                            }
                            break;
                        }
                    }
                });
            }
            
            // This is so that they don't get blown up by the core explosion
            for (Entity l : lmps) {
                l.worldObj.spawnEntityInWorld(l);
            }
            
            for (LimbInfo li : controller.limbs) {
                li.idc.getEntity().setDead();
            }
            controller.setDead();
        }
    }
    
    ;
    
    abstract TechniqueKind getKind();
    
    boolean usable(ColossusController controller) {
        return true;
    }
    
    @Override
    public Technique tick(ColossusController controller, int age) {
        return this;
    }

    @Override
    public void onEnterState(ColossusController controller, Technique prevState) { }

    @Override
    public void onExitState(ColossusController controller, Technique nextState) { }
    
    static final double bow_power = 0.1;
    static final Interpolation bendInterp = Interpolation.LINEAR; // SMOOTH could work; playing it safe tho
    
    protected Technique finishMove(ColossusController controller, Technique next) {
        for (LimbInfo li : controller.limbs) {
            if (li.isTurning()) return this;
        }
        return next;
    }
    
    protected Technique finishMove(ColossusController controller) {
        return finishMove(controller, PICK_NEXT_TECHNIQUE);
    }
    
    protected void targetLimb(LimbInfo li, BodySide turnDirection) {
        if (li.type != LimbType.ARM) return;
        // Swing arms towards side
        double d = turnDirection == BodySide.RIGHT ? -1 : +1;
        Quaternion rot = Quaternion.getRotationQuaternionRadians(Math.PI / 2, ForgeDirection.SOUTH);
        double turn;
        // But the arm that's crossing the body can't, anatomically speaking, go all the way
        if (li.side == turnDirection) {
            turn = 90;
        } else {
            turn = +45;
        }
        turn = Math.toRadians(turn) * d;
        rot = Quaternion.getRotationQuaternionRadians(turn, ForgeDirection.UP).multiply(rot);
        li.target(rot, 1F/3F, Interpolation.SMOOTH);
    }
    
    private static final double distSq = WorldGenColossus.SMOOTH_START * WorldGenColossus.SMOOTH_START;
    protected static final Object CONTINUE = new Object();
    
    protected <E> E iteratePotentialPlayers(ColossusController controller) {
        for (EntityPlayer player : (Iterable<EntityPlayer>) controller.worldObj.playerEntities) {
            if (player.getDistanceSqToEntity(controller) > distSq) continue;
            if (player.capabilities.isCreativeMode /*&& !Core.dev_environ*/) continue;
            Object res = this.visitPlayer(player, controller);
            if (res == CONTINUE) continue;
            return (E) res;
        }
        return null;
    }
    
    protected Object visitPlayer(EntityPlayer player, ColossusController controller) { return null; }

    static final int SIT_FALL_TIME = 20 * 3;
}
