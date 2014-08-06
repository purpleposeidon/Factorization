package factorization.colossi;

import java.io.IOException;
import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import factorization.api.Quaternion;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.DataInNBT;
import factorization.api.datahelpers.DataOutNBT;
import factorization.api.datahelpers.Share;
import factorization.fzds.api.IDeltaChunk;

public class ColossusController extends Entity {
    static enum BodySide { LEFT, RIGHT };
    static enum LimbType { BODY, ARM, LEG };
    LimbInfo[] limbs;
    boolean setup = false;
    
    static class LimbInfo {
        LimbType type;
        BodySide side;
        int length;
        UUID entityId;
        IDeltaChunk ent = null;
        
        public LimbInfo() { }
        
        
        public LimbInfo(LimbType type, BodySide side, int length, IDeltaChunk ent) {
            this.type = type;
            this.side = side;
            this.length = length;
            this.ent = ent;
        }


        void putData(DataHelper data, int index) throws IOException {
            type = data.as(Share.PRIVATE, "limbType" + index).putEnum(type);
            side = data.as(Share.PRIVATE, "limbSide" + index).putEnum(side);
            length = data.as(Share.PRIVATE, "limbLength" + index).putInt(length);
            entityId = data.as(Share.PRIVATE, "entityId" + index).putUUID(entityId);
        }
    }
    
    
    public ColossusController(World world) {
        super(world);
    }
    
    public ColossusController(World world, LimbInfo[] limbInfo) {
        super(world);
        this.limbs = limbInfo;
        for (LimbInfo li : limbs) {
            li.entityId = li.ent.getUniqueID();
        }
        setup = true;
    }
    
    @Override
    public void onEntityUpdate() {
        if (!setup) {
            setup = true;
            loadLimbs();
        }
    }
    
    void loadLimbs() {
        boolean needed = false;
        for (LimbInfo li : limbs) {
            if (li.ent == null) {
                needed = true;
                break;
            }
        }
        if (!needed) return;
        for (Entity ent : (Iterable<Entity>) worldObj.loadedEntityList) {
            UUID test = ent.getUniqueID();
            for (LimbInfo li : limbs) {
                if (li.entityId.equals(test) && li.ent instanceof IDeltaChunk) {
                    li.ent = (IDeltaChunk) ent;
                    break;
                }
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
