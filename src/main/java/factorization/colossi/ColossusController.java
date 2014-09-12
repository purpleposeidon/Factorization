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

    
    static class LimbInfo {
        static UUID fake_uuid = UUID.fromString("00000000-0000-0000-0000-000000000000");
        LimbType type = LimbType.UNKNOWN_LIMB_TYPE;
        BodySide side = BodySide.UNKNOWN_BODY_SIDE;
        byte parity = 0; // If you have a centipede, you kind of need to swap left & right every other foot
        int length; // How long the limb is
        UUID entityId = fake_uuid;
        IDeltaChunk ent = null;
        Vec3 originalBodyOffset; // Joint position, relative to the body's DSE.
        
        // The following transformations are applied in the order given here.
        // They aren't relevant to LimbType.BODY.
        // Angles are in radians.
        double extension = 0; // Translation along the limb's local Y axis
        double twist = 0; // Rotation on the local Y axis
        double swing = 0; // Rotation on the local Z axis. Swinging arms/legs.
        double sweep = 0; // Rotation on the global Y axis
        
        public LimbInfo() { }
        
        public LimbInfo(LimbType type, BodySide side, int length, IDeltaChunk ent) {
            this.type = type;
            this.side = side;
            this.length = length;
            this.ent = ent;
            this.entityId = ent.getUniqueID();
        }


        void putData(DataHelper data, int index) throws IOException {
            type = data.as(Share.PRIVATE, "limbType" + index).putEnum(type);
            side = data.as(Share.PRIVATE, "limbSide" + index).putEnum(side);
            parity = data.as(Share.PRIVATE, "limbParity" + index).putByte(parity);
            length = data.as(Share.PRIVATE, "limbLength" + index).putInt(length);
            entityId = data.as(Share.PRIVATE, "entityId" + index).putUUID(entityId);
            if (data.isReader()) {
                originalBodyOffset = Vec3.createVectorHelper(0, 0, 0);
            }
            originalBodyOffset.xCoord = data.as(Share.PRIVATE, "bodyX" + index).putDouble(originalBodyOffset.xCoord);
            originalBodyOffset.yCoord = data.as(Share.PRIVATE, "bodyY" + index).putDouble(originalBodyOffset.yCoord);
            originalBodyOffset.zCoord = data.as(Share.PRIVATE, "bodyZ" + index).putDouble(originalBodyOffset.zCoord);
            extension = data.as(Share.PRIVATE, "extension" + index).putDouble(extension);
            twist = data.as(Share.PRIVATE, "twist" + index).putDouble(twist);
            swing = data.as(Share.PRIVATE, "swing" + index).putDouble(swing);
            sweep = data.as(Share.PRIVATE, "sweep" + index).putDouble(sweep);
        }
        
        void setOffsetFromBody(IDeltaChunk body) {
            originalBodyOffset = Vec3.createVectorHelper(ent.posX - body.posX, ent.posY - body.posY, ent.posZ - body.posZ);
        }
        
        boolean limbSwingParity() {
            return side == BodySide.RIGHT ^ (parity % 1 == 0) ^ type == LimbType.ARM;
        }
    }
    
    
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
            
            Quaternion sweep = Quaternion.getRotationQuaternionRadians(limb.sweep, ForgeDirection.UP);
            rot.incrMultiply(sweep);
            
            Quaternion swing = Quaternion.getRotationQuaternionRadians(limb.swing, ForgeDirection.SOUTH);
            rot.incrMultiply(swing);
            
            Quaternion twist = Quaternion.getRotationQuaternionRadians(limb.twist, ForgeDirection.UP);
            rot.incrMultiply(twist);
            
            
            
            
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
        if (path_target.x > body.posX - d && path_target.x < body.posX + d) {
            if (path_target.z > body.posZ - d && path_target.z < body.posZ + d) {
                return true;
            }
        }
        return false;
    }
    
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
        if (rotation_distance > Math.PI / 1800) {
            int size = leg_size + 1;
            double rotation_speed = (Math.PI * 2) / (360 * size * size);
            double t = rotation_speed / rotation_distance;
            t = Math.min(0.5, t);
            target_rotation.incrLerp(current_rotation, 1 - t);
            body.setRotation(target_rotation);
        } else if (rotation_distance > 0) {
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
                    boolean should_swing_arms = true; //((offsetTick % (8 * Math.PI)) < Math.PI * 2);
                    if (should_swing_arms) {
                        limb.swing *= 0.025;
                        limb.extension = swing * 0.01;
                    } else {
                        limb.swing = 0;
                        limb.extension = 0;
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
}
