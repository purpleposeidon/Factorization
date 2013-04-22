package factorization.fzds;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.chunk.Chunk;
import factorization.api.Coord;

public class TransferLib {
    public static int set_method = 0;
    public static void setRaw(Coord c, int id, int md) {
        switch (set_method) {
        default:
            Chunk chunk = c.w.getChunkFromBlockCoords(c.x, c.z);
            Block origBlock = Block.blocksList[id];
            Block.blocksList[id] = Block.stone;
            try {
                chunk.setBlockIDWithMetadata(c.x & 15, c.y, c.z & 15, id, md);
            } finally {
                Block.blocksList[id] = origBlock;
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
        }
    }
    
    private static TileEntity wiper = new TileEntity();
    
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
                Chunk chunk = src.w.getChunkFromBlockCoords(src.x, src.z);
                chunk.chunkTileEntityMap.remove(new ChunkPosition(src.x & 15, src.y, src.z & 15));
            }
        }
        if (wipeSrc) {
            setRaw(src, 0, 0);
        }
        wiper.worldObj = null; //Don't hold onto a reference
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
