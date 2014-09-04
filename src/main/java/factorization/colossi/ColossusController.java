package factorization.colossi;

import java.io.IOException;
import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
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
    boolean setup = false;
    
    static enum States {
        WALK_EAST_SAFELY {},
        RUN_BACK,
        WAIT,
        FLAIL_ARMS,
        SHAKE_BODY,
        CHASE_PLAYER_ON_GROUND;
    };

    
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
    }
    
    
    public ColossusController(World world) {
        super(world);
    }
    
    public ColossusController(World world, LimbInfo[] limbInfo) {
        super(world);
        this.limbs = limbInfo;
        IDeltaChunk body = null;
        for (LimbInfo li : limbs) {
            li.entityId = li.ent.getUniqueID();
            if (li.type == LimbType.BODY) {
                body = li.ent;
            }
        }
        for (LimbInfo li : limbs) {
            li.setOffsetFromBody(body);
        }
        setup = true;
    }
    
    AnimationExecutor ae = new AnimationExecutor("legWalk");
    
    @Override
    public void onEntityUpdate() {
        if (!setup) {
            if (worldObj.isRemote) return;
            setup = true;
            loadLimbs();
            if (!setup) return;
        }
        LimbInfo body = null;
        double TAU = Math.PI * 2;
        for (LimbInfo li : limbs) {
            if (li.type == LimbType.BODY) {
                body = li;
            }
            if (li.type == LimbType.ARM) {
                li.extension = -0.0;
                li.twist += (TAU * 1.0/8.0) / 20;
                li.twist = 0;
                li.swing = TAU * 0.40;
                li.sweep = TAU / 8;
                if (li.side == BodySide.LEFT) li.sweep *= -1;
            }
            if (li.type == LimbType.LEG) {
                li.swing = TAU * 0.025;
                if (li.side == BodySide.LEFT) li.swing *= -1;
            }
        }
        if (body == null) return; // !
        Quaternion bodyRotation = body.ent.getRotation();
        for (LimbInfo limb : limbs) {
            if (limb == body) continue;
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
            limb.ent.posX = body.ent.posX + joint.xCoord;
            limb.ent.posY = body.ent.posY + joint.yCoord;
            limb.ent.posZ = body.ent.posZ + joint.zCoord;
            
            
            /*if (ae.tick(body, limb)) {
                ae = new AnimationExecutor("legWalk");
            }*/
        }
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

}
