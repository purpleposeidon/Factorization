package factorization.colossi;

import java.io.IOException;
import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import factorization.api.Coord;
import factorization.api.Quaternion;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.DataInNBT;
import factorization.api.datahelpers.DataOutNBT;
import factorization.api.datahelpers.Share;
import factorization.fzds.api.IDeltaChunk;

public class ColossusController extends Entity {
    static enum BodySide { LEFT, RIGHT, UNKNOWN_BODY_SIDE };
    static enum LimbType { BODY, ARM, LEG, UNKNOWN_LIMB_TYPE };
    LimbInfo[] limbs;
    IDeltaChunk body;
    final ColossusAI controller = new ColossusAI(this);
    boolean setup = false;
    int arm_size = 0, arm_length = 0;
    int leg_size = 0, leg_length = 0;
    
    Coord home = null;
    Coord path_target = null;
    
    transient Entity target_entity;
    transient int target_count = 0;
    transient long shake_seed = 0;

    
    public ColossusController(World world) {
        super(world);
        path_target = new Coord(this);
    }
    
    public ColossusController(World world, LimbInfo[] limbInfo, int arm_size, int arm_length, int leg_size, int leg_length) {
        super(world);
        this.limbs = limbInfo;
        for (LimbInfo li : limbs) {
            li.entityId = li.ent.getUniqueID();
            if (li.type == LimbType.BODY) {
                body = li.ent;
            }
        }
        for (LimbInfo li : limbs) {
            li.setOffsetFromBody(body);
        }
        byte left_leg_parity = 0, left_arm_parity = 0, right_leg_parity = 0, right_arm_parity = 0;
        discover_limb_parities: for (LimbInfo li : limbs) {
            byte use_parity = 0;
            switch (li.type) {
            default: continue discover_limb_parities;
            case ARM:
                if (li.side == BodySide.RIGHT) {
                    use_parity = right_arm_parity++;
                } else if (li.side == BodySide.LEFT) {
                    use_parity = left_arm_parity++;
                } else {
                    continue;
                }
                break;
            case LEG:
                if (li.side == BodySide.RIGHT) {
                    use_parity = right_leg_parity++;
                } else if (li.side == BodySide.LEFT) {
                    use_parity = left_leg_parity++;
                } else {
                    continue;
                }
                break;
            }
            li.parity = use_parity;
        }
        this.arm_size = arm_size;
        this.arm_length = arm_length;
        this.leg_size = leg_size;
        this.leg_length = leg_length;
    }
    
    AnimationExecutor ae = new AnimationExecutor("legWalk");
    
    @Override
    public void onEntityUpdate() {
        if (worldObj.isRemote) return;
        if (!setup) {
            setup = true;
            loadLimbs();
            if (home == null) {
                home = new Coord(this);
                path_target = home.copy();
            }
            if (!setup) return;
        }
        if (body == null || body.isDead) {
            setDead();
            return;
        }
        moveToTarget(body);
        tickLimbSwing();
        controller.tick();
        Quaternion bodyRotation = body.getRotation();
        for (LimbInfo limb : limbs) {
            if (limb.type == LimbType.BODY) continue;
            Vec3 joint = limb.originalBodyOffset.addVector(0, limb.extension, 0);
            bodyRotation.applyRotation(joint);
            Quaternion rot = new Quaternion(bodyRotation);
            
            if (limb.sweep != 0) {
                Quaternion sweep = Quaternion.getRotationQuaternionRadians(limb.sweep, ForgeDirection.UP);
                rot.incrMultiply(sweep);
            }
            
            if (limb.swing != 0) {
                Quaternion swing = Quaternion.getRotationQuaternionRadians(limb.swing, ForgeDirection.SOUTH);
                rot.incrMultiply(swing);
            }
            
            if (limb.twist != 0) {
                Quaternion twist = Quaternion.getRotationQuaternionRadians(limb.twist, ForgeDirection.UP);
                rot.incrMultiply(twist);
            }
            
            if (limb.flap != 0) {
                Quaternion flap = Quaternion.getRotationQuaternionRadians(limb.flap, ForgeDirection.EAST);
                rot.incrMultiply(flap);
            }
            
            
            
            
            rot.incrNormalize();
            limb.ent.setRotation(rot);
            limb.ent.posX = body.posX + joint.xCoord;
            limb.ent.posY = body.posY + joint.yCoord;
            limb.ent.posZ = body.posZ + joint.zCoord;
            
            
            /*if (ae.tick(body, limb)) {
                ae = new AnimationExecutor("legWalk");
            }*/
        }
        setPosition(body.posX, body.posY, body.posZ);
    }
    
    void loadLimbs() {
        boolean needed = false;
        for (LimbInfo li : limbs) {
            if (li.ent == null) {
                needed = true;
                break;
            } else if (li.entityId == null) {
                li.entityId = li.ent.getUniqueID();
            }
        }
        if (!needed) return;
        for (Entity ent : (Iterable<Entity>) worldObj.loadedEntityList) {
            UUID test = ent.getUniqueID();
            for (LimbInfo li : limbs) {
                if (li.entityId == null || li.ent != null) continue;
                if (li.entityId.equals(test) && ent instanceof IDeltaChunk) {
                    li.ent = (IDeltaChunk) ent;
                    break;
                }
            }
        }
        for (LimbInfo li : limbs) {
            if (li.ent == null) {
                setup = false;
                break;
            }
            if (li.type == LimbType.BODY) {
                body = li.ent;
            }
        }
    }

    @Override
    protected void entityInit() { }
    
    void putData(DataHelper data) throws IOException {
        int limb_count = data.as(Share.PRIVATE, "limbCount").putInt(limbs == null ? 0 : limbs.length);
        if (data.isReader()) {
            limbs = new LimbInfo[limb_count];
        }
        for (int i = 0; i < limbs.length; i++) {
            if (data.isReader()) {
                limbs[i] = new LimbInfo();
            }
            limbs[i].putData(data, i);
        }
        controller.serialize("controller", data);
        leg_size = data.as(Share.PRIVATE, "legSize").putInt(leg_size);
        home = data.as(Share.PRIVATE, "home").put(home);
        path_target = data.as(Share.PRIVATE, "path_target").put(path_target);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        try {
            putData(new DataInNBT(tag));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        try {
            putData(new DataOutNBT(tag));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    boolean atTarget() {
        if (path_target == null) return true;
        double d = 0.1;
        double dx = path_target.x - body.posX;
        double dz = path_target.z - body.posZ;
        
        if (dx < d && dx > -d && dz < d && dz > -d) {
            return true;
        }
        return false;
    }
    
    int turning = 0;
    
    void moveToTarget(IDeltaChunk body) {
        if (atTarget()) {
            for (LimbInfo limb : limbs) {
                limb.ent.motionX = 0;
                limb.ent.motionY = 0;
                limb.ent.motionZ = 0;
            }
            return;
        }
        Vec3 target = path_target.createVector();
        target.yCoord = posY;
        Vec3 me = Vec3.createVectorHelper(body.posX, body.posY, body.posZ);
        Vec3 delta = me.subtract(target);
        double speed = Math.min(1.0/20.0, delta.lengthVector());
        delta = delta.normalize();
        double angle = Math.atan2(delta.xCoord, delta.zCoord) - Math.PI / 2;
        //System.out.println(Math.toDegrees(angle));
        Quaternion target_rotation = Quaternion.getRotationQuaternionRadians(angle, ForgeDirection.UP);
        Quaternion current_rotation = body.getRotation();
        double rotation_distance = target_rotation.getAngleBetween(current_rotation);
        turning = 0;
        if (rotation_distance > Math.PI / 1800) {
            int size = leg_size + 1;
            double rotation_speed = (Math.PI * 2) / (360 * size * size * 2);
            double t = rotation_speed / rotation_distance;
            t = Math.min(0.5, t);
            target_rotation.incrLerp(current_rotation, 1 - t);
            body.setRotation(target_rotation);
            turning = angle > 0 ? 1 : -1;
        } else if (rotation_distance > Math.PI * 0.0001) {
            body.setRotation(target_rotation);
            body.setRotationalVelocity(new Quaternion());
        } else {
            if (!body.getRotationalVelocity().isZero()) {
                body.setRotationalVelocity(new Quaternion());
            }
            for (LimbInfo limb : limbs) {
                limb.ent.motionX = delta.xCoord * speed;
                limb.ent.motionY = delta.yCoord * speed;
                limb.ent.motionZ = delta.zCoord * speed;
            }
            walked += speed;
        }
    }
    
    
    double swingTicks = 0;
    double walked = 0;
    void tickLimbSwing() {
        if (turning != 0) {
            tickLimbTurn();
            return;
        } else {
            turnTicks = 0;
        }
        double slow = 1;
        double offsetTick = swingTicks * slow + Math.PI / 2;
        double loop = offsetTick % (2 * Math.PI);
        loop = Math.abs(loop - Math.PI) - Math.PI / 2;
        double swing = loop * 3.0 / 16.0;
        for (LimbInfo limb : limbs) {
            if (limb.type == LimbType.LEG || limb.type == LimbType.ARM) {
                limb.swing = swing;
                if (limb.limbSwingParity()) {
                    limb.swing *= -1;
                }
                if (limb.type == LimbType.ARM) {
                    boolean should_swing_arms = true;
                    if (should_swing_arms) {
                        limb.swing *= 0.025;
                        limb.extension = swing * 0.01;
                        limb.flap = Math.abs(swing) / arm_length * (limb.limbSwingParity() ? 1 : -1);
                    } else {
                        limb.swing = 0;
                        limb.extension = 0;
                        limb.flap = 0;
                    }
                }
            }
        }
        if (walked == 0) {
            double clip = 0.01;
            double stop_moving_delta = (Math.abs(swing) - clip) * 0.25;
            if (swing > clip) {
                swingTicks += stop_moving_delta;
            } else if (swing < -clip) {
                swingTicks -= stop_moving_delta;
            }
        } else {
            swingTicks += walked;
            walked = 0;
        }
    }
    
    int turnTicks = 0;
    void tickLimbTurn() {
        // lift right leg while twisting left leg
        // set right leg down
        // lift left leg while twiting right leg
        // set left leg down
        
        double lift_height = 1.5F/16F;
        double base_twist = Math.PI * 2 * 0.005;
        double phase_length = 18;
        double freq = Math.sin(turnTicks * phase_length / Math.PI);
        double deriv = Math.cos(turnTicks * phase_length / Math.PI);
        for (LimbInfo limb : limbs) {
            if (limb.type != LimbType.LEG) continue;
            if (limb.limbSwingParity() ^ freq > 0) {
                limb.extension = Math.abs(freq * lift_height);
                limb.swing = freq * 0.01;
                if (deriv < 0) {
                    limb.twist = freq * base_twist;
                }
            } else {
                limb.extension *= 0.90;
                if (limb.extension < 0.01) {
                    limb.extension = 0;
                }
                if (deriv > 0) {
                    limb.twist = freq * base_twist;
                }
            }
            limb.act_violently = true;
        }
        
        
        turnTicks++;
    }
}
