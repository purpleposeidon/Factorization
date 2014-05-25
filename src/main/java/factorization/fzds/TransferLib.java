package factorization.fzds;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.chunk.Chunk;
import factorization.api.Coord;

public class TransferLib {
    public static int default_set_method = 0;
    public static final int SET_SNEAKY = 0, SET_ISREMOTE = 1, SET_DIRECT = 2, SET_VANILLA_FLAGS = 3;
    
    public static void setRaw(Coord c, Block id, int md) {
        setRaw(c, id, md, default_set_method);
    }
    
    /**
     * 
     * @param c
     * @param id
     * @param md
     * @param use_method
     * 	0: Sneaky and intrusive. Set value in chunk directly
     *  1: World.isRemote
     *  2: Direct
     *  3: Vanilla, with flags.
     */
    public static void setRaw(Coord c, Block id, int md, int use_method) {
        switch (use_method) {
        default:
        case SET_SNEAKY:
            Chunk chunk = c.w.getChunkFromBlockCoords(c.x, c.z);
            chunk.func_150807_a(c.x & 15, c.y, c.z & 15, id, md);
            c.markBlockForUpdate();
            break;
        case SET_ISREMOTE:
            boolean rem = c.w.isRemote;
            c.w.isRemote = true;
            try {
                c.setIdMd(id, md, false);
            } finally {
                c.w.isRemote = rem;
            }
            break;
        case SET_DIRECT:
            c.setIdMd(id, md, false);
            break;
        case SET_VANILLA_FLAGS:
            c.w.setBlock(c.x, c.y, c.z, id, md, 0);
            break;
        }
    }
    
    private static TileEntity wiper = new TileEntity();
    
    public static void rawRemoveTE(Coord c) {
        Chunk chunk = c.w.getChunkFromBlockCoords(c.x, c.z);
        chunk.chunkTileEntityMap.remove(new ChunkPosition(c.x & 15, c.y, c.z & 15));
    }
    
    public static TileEntity move(Coord src, Coord dest, boolean wipeSrc, boolean overwriteDestination) {
        return move(src, dest, wipeSrc, overwriteDestination, default_set_method);
    }

    public static TileEntity move(Coord src, Coord dest, boolean wipeSrc, boolean overwriteDestination, int setMethod) {
        Block id = src.getId();
        int md = src.getMd();
        if (id == null && !overwriteDestination) {
            return null;
        }
        TileEntity te = src.getTE();
        NBTTagCompound teData = null;
        if (te != null) {
            teData = new NBTTagCompound();
            te.writeToNBT(teData);
            if (wipeSrc) {
                wiper.validate();
                src.setTE(wiper);
                rawRemoveTE(src);
            }
        }
        if (wipeSrc) {
            setRaw(src, Blocks.air, 0);
        }
        wiper.setWorldObj(null); //Don't hold onto a reference
        if (dest.getTE() != null) {
            rawRemoveTE(dest);
        }
        setRaw(dest, id, md);
        if (teData != null) {
            teData.setInteger("x", dest.x);
            teData.setInteger("y", dest.y);
            teData.setInteger("z", dest.z);
            TileEntity ret = TileEntity.createAndLoadEntity(teData);
            ret.validate();
            dest.setTE(ret);
            return ret;
        }
        return null;
    }
    
    public static void rawErase(Coord c) {
        TileEntity te = c.getTE();
        if (te != null) {
            c.rmTE();
        }
        setRaw(c, Blocks.air, 0);
    }
    
}
