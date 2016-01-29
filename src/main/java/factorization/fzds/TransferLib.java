package factorization.fzds;

import factorization.api.Coord;
import factorization.util.NORELEASE;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import java.util.List;

public class TransferLib {
    public static int default_set_method = 0;
    public static final int SET_SNEAKY = 0,
            SET_SNEAKY_NO_LIGHTING_UPDATES = 1,
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

            BlockPos pos = c.toBlockPos();
            TileEntity te = chunk.getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK);
            if (te != null) {
                chunk.removeTileEntity(pos);
            }

            final int origColumHeight = chunk.heightMap[xzIndex];
            final Block origBlock = chunk.getBlock(blockXChunk, c.y, blockZChunk);
            final int origMd = chunk.getBlockMetadata(new BlockPos(blockXChunk, c.y, blockZChunk));

            if (origBlock == id && origMd == md) {
                return;
            }
            chunk.setChunkModified();
            ExtendedBlockStorage[] storageArrays = chunk.getBlockStorageArray();
            ExtendedBlockStorage extendedblockstorage = storageArrays[c.y >> 4];
            boolean extendsHeight = false;

            if (extendedblockstorage == null) {
                if (id == Blocks.air) return;

                extendedblockstorage = storageArrays[c.y >> 4] = new ExtendedBlockStorage(c.y >> 4 << 4, !c.w.provider.getHasNoSky());
                extendsHeight = c.y >= origColumHeight;
            }

            extendedblockstorage.set(blockXChunk, blockYChunk, blockZChunk, id.getStateFromMeta(md));
            if (extendedblockstorage.getBlockByExtId(blockXChunk, c.y & 15, blockZChunk) != id) return;
            if (use_method == SET_SNEAKY_NO_LIGHTING_UPDATES) return;
            chunk.setChunkModified();
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
                    || chunk.getLightFor(EnumSkyBlock.SKY, pos) > 0
                    || chunk.getLightFor(EnumSkyBlock.BLOCK, pos) > 0) {
                chunk.propagateSkylightOcclusion(blockXChunk, blockZChunk);
            }

            break;
        }
        case SET_DIRECT:
            c.setIdMd(id, md, false);
            break;
        case SET_VANILLA_FLAGS:
            c.w.setBlockState(c.toBlockPos(), id.getStateFromMeta(md), 0);
            break;
        case SET_CHUNKY: {
            BlockPos pos = c.toBlockPos();
            Chunk chunk = c.w.getChunkFromBlockCoords(pos);
            chunk.setBlockState(pos, id.getStateFromMeta(md));
            c.markBlockForUpdate();
            break;
            }
        }
    }

    public static void rawRemoveTE(Coord c) {
        c.getChunk().getTileEntityMap().remove(c.toBlockPos());
    }
    
    public static TileEntity move(Coord src, Coord dest, boolean wipeSrc, boolean overwriteDestination) {
        default_set_method = NORELEASE.just(SET_DIRECT);
        return move(src, dest, wipeSrc, overwriteDestination, default_set_method);
    }

    public static TileEntity move(Coord src, Coord dest, boolean wipeSrc, boolean overwriteDestination, int setMethod) {
        Block id = src.getBlock();
        int md = src.getMd();
        int blockLight = src.getLightLevelBlock();
        int skyLight = src.getLightLevelSky();
        if (id == null && !overwriteDestination) {
            return null;
        }
        long block_tick_time = -1;
        int block_tick_priority = -1;

        BlockPos srcPos = src.toBlockPos();
        
        {
            List<NextTickListEntry> pendingTicks = (List<NextTickListEntry>) src.w.getPendingBlockUpdates(src.getChunk(), false);
            if (pendingTicks != null) {
                for (NextTickListEntry tick : pendingTicks) {
                    if (tick.position.equals(srcPos)) {
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
                rawRemoveTE(src);
            }
        }
        if (wipeSrc) {
            setRaw(src, Blocks.air, 0);
        }
        if (dest.getTE() != null) {
            rawRemoveTE(dest);
        }
        setRaw(dest, id, md, setMethod);
        dest.setLightLevelBlock(blockLight);
        dest.setLightLevelSky(skyLight);
        TileEntity ret = null;
        if (teData != null) {
            teData.setInteger("x", dest.x);
            teData.setInteger("y", dest.y);
            teData.setInteger("z", dest.z);
            ret = TileEntity.createAndLoadEntity(teData);
            ret.validate();
            dest.setTE(ret);
        }
        if (block_tick_time > -1) {
            dest.w.updateBlockTick(dest.toBlockPos(), id, (int) block_tick_time, block_tick_priority);
        }
        return ret;
    }
    
    public static void rawErase(Coord c) {
        TileEntity te = c.getTE();
        if (te != null) {
            c.rmTE();
        }
        setRaw(c, Blocks.air, 0);
    }
    
}
