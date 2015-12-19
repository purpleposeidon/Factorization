package factorization.truth;

import java.util.ArrayList;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.chunk.Chunk;

import net.minecraftforge.common.util.Constants;

import factorization.api.Coord;
import factorization.api.DeltaCoord;
import factorization.shared.Core;
import factorization.util.DataUtil;

public class DocWorld extends WorldClient {
    static final Minecraft mc = Minecraft.getMinecraft();
    static final int LEN = 16*16*16;
    int[] blockIds;
    int[] blockMetadatas;
    ArrayList<TileEntity> tileEntities = new ArrayList<TileEntity>();
    ArrayList<Entity> entities = new ArrayList<Entity>();
    public int diagonal = 32;
    Coord orig = new Coord(this, 0, 0, 0);
    
    public DocWorld() {
        super(mc.getNetHandler(), new WorldSettings(mc.theWorld.getWorldInfo()), 0, mc.theWorld.getDifficulty(), mc.mcProfiler);
        blockIds = new int[LEN];
        blockMetadatas = new int[LEN];
    }
    
    private static final String BLOCK_IDS = "i", BLOCK_METADATA = "m", TE_LIST = "t", ENTITY_LIST = "e", DIAGONAL = "d", ORIG_ENT_POS = "o";
    
    public DocWorld(NBTTagCompound tag) {
        this();
        orig.readFromNBT(ORIG_ENT_POS, tag);
        blockIds = tag.getIntArray(BLOCK_IDS);
        blockMetadatas = tag.getIntArray(BLOCK_METADATA);
        NBTTagList teList = tag.getTagList(TE_LIST, Constants.NBT.TAG_COMPOUND);
        tileEntities.clear();
        for (int i = 0; i < teList.tagCount(); i++) {
            NBTTagCompound tc = teList.getCompoundTagAt(i);
            TileEntity te = TileEntity.createAndLoadEntity(tc);
            if (te != null) {
                te.setWorldObj(this);
                tileEntities.add(te);
            }
        }
        entities.clear();
        NBTTagList entList = tag.getTagList(ENTITY_LIST, Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < entList.tagCount(); i++) {
            NBTTagCompound tc = entList.getCompoundTagAt(i);
            Entity e = EntityList.createEntityFromNBT(tc, this);
            if (e != null) {
                e.worldObj = this;
                entities.add(e);
            }
        }
        diagonal = tag.getInteger(DIAGONAL);
    }
    
    void writeToTag(NBTTagCompound tag) {
        orig.writeToNBT(ORIG_ENT_POS, tag);
        tag.setIntArray(BLOCK_IDS, blockIds);
        tag.setIntArray(BLOCK_METADATA, blockMetadatas);
        NBTTagList teList = new NBTTagList();
        for (TileEntity te : tileEntities) {
            NBTTagCompound tc = new NBTTagCompound();
            te.writeToNBT(tc);
            teList.appendTag(tc);
        }
        tag.setTag(TE_LIST, teList);
        NBTTagList entList = new NBTTagList();
        for (Entity ent : entities) {
            NBTTagCompound tc = new NBTTagCompound();
            ent.writeToNBTOptional(tc);
            entList.appendTag(tc);
        }
        tag.setTag(ENTITY_LIST, entList);
        tag.setInteger(DIAGONAL, diagonal);
    }

    @Override
    protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) {
        return false;
    }

    private int getIndex(BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        if (x < 0 || y < 0 || z < 0) return -1;
        if (x > 0xF || y > 0xF || z > 0xF) return -1;
        return x + (y << 4) + (z << 8);
    }
    
    @Override
    public IBlockState getBlockState(BlockPos pos) {
        int i = getIndex(pos);
        if (i == -1) return Blocks.air.getDefaultState();
        int id = blockIds[i];
        if (id == -10) {
            return Core.registry.factory_block.getDefaultState();
        } else if (id == -11) {
            return Core.registry.resource_block.getDefaultState();
        } else if (id == -12) {
            return Core.registry.dark_iron_ore.getDefaultState();
        } else {
            return DataUtil.getBlock(id).getDefaultState();
        }
    }

    public int getBlockMetadata(BlockPos pos) {
        int i = getIndex(pos);
        if (i == -1) return 0;
        return blockMetadatas[i];
    }
    

    @Override
    public TileEntity getTileEntity(BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        for (TileEntity te : tileEntities) {
            if (te.getPos().getX() == x && te.getPos().getY() == y && te.getPos().getZ() == z) {
                return te;
            }
        }
        return null;
    }

    @Override
    public float getLightBrightness(BlockPos pos) {
        return 0xF;
    }

    @Override
    public int getLightFor(EnumSkyBlock type, BlockPos pos) {
        return 0xF;
    }

    void setIdMdTe(DeltaCoord dc, Block block, int md, TileEntity te) {
        int i = getIndex(dc.toBlockPos());
        if (i == -1) return;
        int useId = DataUtil.getId(block);
        if (block == Core.registry.factory_block || block == Core.registry.factory_block_barrel) {
            useId = -10;
        } else if (block == Core.registry.resource_block) {
            useId = -11;
        } else if (block == Core.registry.dark_iron_ore) {
            useId = -12;
        }
        blockIds[i] = useId;
        blockMetadatas[i] = md;
        
        if (te == null) return;
        TileEntity clone = DataUtil.cloneTileEntity(te);
        clone.setPos(dc.toBlockPos());
        tileEntities.add(clone);
    }
    
    void addEntity(Entity ent) {
        if (ent == null) return;
        entities.add(ent);
    }
    
    Chunk myChunk = new Chunk(this, 0, 0) {
        @Override
        public Block getBlock(BlockPos pos) {
            return DocWorld.this.getBlockState(pos).getBlock();
        }

        @Override
        public TileEntity getTileEntity(BlockPos pos, EnumCreateEntityType p_177424_2_) {
            return DocWorld.this.getTileEntity(pos);
        }

        @Override
        public boolean getAreLevelsEmpty(int par1, int par2) {
            return false;
        }

        @Override
        public IBlockState getBlockState(BlockPos pos) {
            Block b = getBlock(pos);
            return b.getStateFromMeta(getBlockMetadata(pos));
        }

        @Override
        public int getBlockMetadata(BlockPos pos) {
            return DocWorld.this.getBlockMetadata(pos);
        }
        
    };
    
    @Override
    public Chunk getChunkFromChunkCoords(int chunkX, int chunkZ) {
        if (chunkX != 0 || chunkZ != 0) {
            return super.getChunkFromChunkCoords(chunkX, chunkZ);
        }
        return myChunk;
    }

}
