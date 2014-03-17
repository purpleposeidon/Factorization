package factorization.docs;

import java.util.ArrayList;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.util.Constants;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Coord;
import factorization.api.DeltaCoord;
import factorization.shared.Core;
import factorization.shared.FzUtil;

public class DocWorld extends WorldClient {
    static final Minecraft mc = Minecraft.getMinecraft();
    static final int LEN = 16*16*16;
    int[] blockIds;
    int[] blockMetadatas;
    ArrayList<TileEntity> tileEntities = new ArrayList();
    ArrayList<Entity> entities = new ArrayList();
    public int diagonal = 32;
    Coord orig = new Coord(this, 0, 0, 0);
    
    public DocWorld() {
        super(mc.getNetHandler(), new WorldSettings(mc.theWorld.getWorldInfo()), 0, mc.theWorld.difficultySetting, mc.mcProfiler);
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
            NBTTagCompound tc = (NBTTagCompound) teList.getCompoundTagAt(i);
            TileEntity te = TileEntity.createAndLoadEntity(tc);
            if (te != null) {
                te.setWorldObj(this);
                tileEntities.add(te);
            }
        }
        entities.clear();
        NBTTagList entList = tag.getTagList(ENTITY_LIST, Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < entList.tagCount(); i++) {
            NBTTagCompound tc = (NBTTagCompound) entList.getCompoundTagAt(i);
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
    protected boolean chunkExists(int chunkX, int chunkZ) {
        return chunkX == 0 && chunkZ == 0;
    }
    
    private int getIndex(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0) return -1;
        if (x > 0xF || y > 0xF || z > 0xF) return -1;
        return x + (y << 4) + (z << 8);
    }
    
    @Override
    public Block getBlock(int x, int y, int z) {
        int i = getIndex(x, y, z);
        if (i == -1) return Blocks.air;
        int id = blockIds[i];
        if (id == -10) {
            return Core.registry.factory_block;
        } else if (id == -11) {
            return Core.registry.resource_block;
        } else if (id == -12) {
            return Core.registry.dark_iron_ore;
        } else {
            return FzUtil.getBlock(id);
        }
    }
    
    @Override
    public int getBlockMetadata(int x, int y, int z) {
        int i = getIndex(x, y, z);
        if (i == -1) return 0;
        return blockMetadatas[i];
    }
    
    @Override
    public TileEntity getTileEntity(int x, int y, int z) {
        for (TileEntity te : tileEntities) {
            if (te.xCoord == x && te.yCoord == y && te.zCoord == z) {
                return te;
            }
        }
        return null;
    }
    
    @Override
    public int getBlockLightValue(int par1, int par2, int par3) {
        return 0xF;
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public int getSkyBlockTypeBrightness(EnumSkyBlock par1EnumSkyBlock, int par2, int par3, int par4) {
        return 0xF;
    }
    
    void setIdMdTe(DeltaCoord dc, Block block, int md, TileEntity te) {
        int i = getIndex(dc.x, dc.y, dc.z);
        if (i == -1) return;
        int useId = FzUtil.getId(block);
        if (block == Core.registry.factory_block) {
            useId = -10;
        } else if (block == Core.registry.resource_block) {
            useId = -11;
        } else if (block == Core.registry.dark_iron_ore) {
            useId = -12;
        }
        blockIds[i] = useId;
        blockMetadatas[i] = md;
        
        if (te == null) return;
        TileEntity clone = FzUtil.cloneTileEntity(te);
        clone.xCoord = dc.x;
        clone.yCoord = dc.y;
        clone.zCoord = dc.z;
        tileEntities.add(clone);
    }
    
    void addEntity(Entity ent) {
        if (ent == null) return;
        entities.add(ent);
    }
    
    Chunk myChunk = new Chunk(this, 0, 0) {
        @Override
        public Block getBlock(int x, int y, int z) {
            return DocWorld.this.getBlock(x, y, z);
        }
        
        @Override
        public TileEntity func_150806_e(int x, int y, int z) {
            return DocWorld.this.getTileEntity(x, y, z);
        }
        
        @Override
        public TileEntity getTileEntityUnsafe(int x, int y, int z) {
            return DocWorld.this.getTileEntity(x, y, z);
        }
        
        @Override
        public boolean getAreLevelsEmpty(int par1, int par2) {
            return false;
        }
        
        @Override
        public int getBlockMetadata(int x, int y, int z) {
            return DocWorld.this.getBlockMetadata(x, y, z);
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
