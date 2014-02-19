package factorization.fzds;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.chunk.Chunk;
import factorization.api.Coord;

public class TransferLib {
    public static int set_method = 0;
    
    public static void setRaw(Coord c, int id, int md) {
        setRaw(c, id, md, set_method);
    }
    
    /**
     * 
     * @param c
     * @param id
     * @param md
     * @param use_method
     * 	0: Sneaky and intrusive. Set value in chunk directly (and make the block be stone)
     *  1: World.isRemote
     *  2: Direct
     *  3: Vanilla, with flags.
     */
    public static void setRaw(Coord c, int id, int md, int use_method) {
        switch (use_method) {
        default:
            Chunk chunk = c.w.getChunkFromBlockCoords(c.x, c.z);
            int old_id = c.getId();
            Block origOldBlock = Blocks.blocksList[old_id];
            Block origNewBlock = Blocks.blocksList[id];
            Blocks.blocksList[old_id] = Blocks.stone;
            Blocks.blocksList[id] = Blocks.stone;
            try {
                chunk.setBlockIDWithMetadata(c.x & 15, c.y, c.z & 15, id, md);
            } finally {
                Blocks.blocksList[old_id] = origOldBlock;
                Blocks.blocksList[id] = origNewBlock;
            }
            c.markBlockForUpdate();
            break;
        case 1:
            boolean rem = c.w.isRemote;
            c.w.isRemote = true;
            try {
                c.setIdMd(id, md);
            } finally {
                c.w.isRemote = rem;
            }
            break;
        case 2:
            c.setIdMd(id, md);
            break;
        case 3:
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
        int id = src.getId();
        int md = src.getMd();
        if (id == 0 && !overwriteDestination) {
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
            setRaw(src, 0, 0);
        }
        wiper.worldObj = null; //Don't hold onto a reference
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
            c.removeTE();
        }
        setRaw(c, 0, 0);
    }
    
}
