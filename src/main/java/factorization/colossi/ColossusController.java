package factorization.colossi;

import java.io.IOException;
import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
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
        static UUID fake_uuid = UUID.randomUUID();
        LimbType type = LimbType.UNKNOWN_LIMB_TYPE;
        BodySide side = BodySide.UNKNOWN_BODY_SIDE;
        int length;
        UUID entityId = fake_uuid;
        IDeltaChunk ent = null;
        Vec3 originalBodyOffset;
        
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
            length = data.as(Share.PRIVATE, "limbLength" + index).putInt(length);
            entityId = data.as(Share.PRIVATE, "entityId" + index).putUUID(entityId);
            if (data.isReader()) {
                originalBodyOffset = Vec3.createVectorHelper(0, 0, 0);
            }
            originalBodyOffset.xCoord = data.as(Share.PRIVATE, "bodyX" + index).putDouble(originalBodyOffset.xCoord);
            originalBodyOffset.yCoord = data.as(Share.PRIVATE, "bodyY" + index).putDouble(originalBodyOffset.yCoord);
            originalBodyOffset.zCoord = data.as(Share.PRIVATE, "bodyZ" + index).putDouble(originalBodyOffset.zCoord);
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
        for (LimbInfo li : limbs) {
            if (li.type == LimbType.BODY) {
                body = li;
            }
        }
        for (LimbInfo limb : limbs) {
            if (limb == body) continue;
            if (ae.tick(body, limb)) {
                ae = new AnimationExecutor("legWalk");
            }
            break;
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
