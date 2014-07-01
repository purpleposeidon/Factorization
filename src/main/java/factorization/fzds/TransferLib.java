package factorization.fzds;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import factorization.api.Coord;

public class TransferLib {
    public static int default_set_method = 0;
    public static final int SET_SNEAKY = 0,
            SET_SNEAKY_NO_LIGHTING_UPDATES = 1,
            SET_ISREMOTE = 2,
            SET_DIRECT = 3,
            SET_VANILLA_FLAGS = 4,
            SET_CHUNKY = 5;
    
    public static void setRaw(Coord c, Block id, int md) {
        setRaw(c, id, md, default_set_method);
    }
    
    /**
     * 
     * @param c
     * @param id
     * @param md
     * @param use_method
     * 	SET_SNEAKY: Sneaky and intrusive. Set value in chunk directly
     *  SET_ISREMOTE: World.isRemote
     *  SET_DIRECT: Direct
     *  SET_VANILLA_FLAGS: Vanilla, with flags.
     *  SET_CHUNKY: Set through the chunk
     */
    public static void setRaw(Coord c, Block id, int md, int use_method) {
        if (c.y < 0) return;
        if (c.y > 0xFF) return;
        
        switch (use_method) {
        default:
        case SET_SNEAKY:
        case SET_SNEAKY_NO_LIGHTING_UPDATES: {
            //From Chunk.func_150807_a, aka SET_CHUNKY
            final Chunk chunk = c.getChunk();
            final int blockXChunk = c.x & 0xF;
            final int blockZChunk = c.z & 0xF;
            final int blockYChunk = c.y & 0xF;
            final int xzIndex = blockZChunk << 4 | blockXChunk;

            if (c.y >= chunk.precipitationHeightMap[xzIndex] - 1) {
                chunk.precipitationHeightMap[xzIndex] = -999;
            }
            
            TileEntity te = chunk.getTileEntityUnsafe(blockXChunk, c.y, blockZChunk);
            if (te != null) {
                chunk.worldObj.removeTileEntity(c.x, c.y, c.z);
            }
            
            final int origColumHeight = chunk.heightMap[xzIndex];
            final Block origBlock = chunk.getBlock(blockXChunk, c.y, blockZChunk);
            final int origMd = chunk.getBlockMetadata(blockXChunk, c.y, blockZChunk);

            if (origBlock == id && origMd == md) {
                return;
            }
            ExtendedBlockStorage[] storageArrays = chunk.getBlockStorageArray();
            ExtendedBlockStorage extendedblockstorage = storageArrays[c.y >> 4];
            boolean extendsHeight = false;

            if (extendedblockstorage == null) {
                if (id == Blocks.air) return;

                extendedblockstorage = storageArrays[c.y >> 4] = new ExtendedBlockStorage(c.y >> 4 << 4, !chunk.worldObj.provider.hasNoSky);
                extendsHeight = c.y >= origColumHeight;
            }

            extendedblockstorage.func_150818_a(blockXChunk, blockYChunk, blockZChunk, id);
            extendedblockstorage.setExtBlockMetadata(blockXChunk, blockYChunk, blockZChunk, md);
            if (extendedblockstorage.getBlockByExtId(blockXChunk, c.y & 15, blockZChunk) != id) return;
            if (use_method == SET_SNEAKY_NO_LIGHTING_UPDATES) return;
            chunk.isModified = true;
            if (extendsHeight) {
                chunk.generateSkylightMap();
                return;
            }
            int newOpacity = id.getLightOpacity();
            int oldOpacity = origBlock.getLightOpacity();

            if (newOpacity > 0) {
                if (c.y >= origColumHeight) {
                    chunk.relightBlock(blockXChunk, c.y + 1, blockZChunk);
                }
            } else if (c.y == origColumHeight - 1) {
                chunk.relightBlock(blockXChunk, c.y, blockZChunk);
            }
            if (newOpacity == oldOpacity) return;
            if (newOpacity < oldOpacity 
                    || chunk.getSavedLightValue(EnumSkyBlock.Sky, blockXChunk, c.y, blockZChunk) > 0
                    || chunk.getSavedLightValue(EnumSkyBlock.Block, blockXChunk, c.y, blockZChunk) > 0) {
                chunk.propagateSkylightOcclusion(blockXChunk, blockZChunk);
            }

            break;
        }
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
        case SET_CHUNKY:
            Chunk chunk = c.w.getChunkFromBlockCoords(c.x, c.z);
            chunk.func_150807_a(c.x & 15, c.y, c.z & 15, id, md);
            c.markBlockForUpdate();
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
        long block_tick_time = -1;
        int block_tick_priority = -1;
        
        {
            List<NextTickListEntry> pendingTicks = (List<NextTickListEntry>) src.w.getPendingBlockUpdates(src.getChunk(), false);
            if (pendingTicks != null) {
                for (NextTickListEntry tick : pendingTicks) {
                    if (tick.xCoord == src.x && tick.yCoord == src.y && tick.zCoord == src.z){
                        block_tick_time = tick.scheduledTime - src.w.getWorldInfo().getWorldTotalTime();
                        block_tick_priority = tick.priority;
                        break;
                    }
                }
            }
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
        if (block_tick_time > -1) {
            dest.w.scheduleBlockUpdateWithPriority(dest.x, dest.y, dest.z, id, (int) block_tick_time, block_tick_priority);
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
