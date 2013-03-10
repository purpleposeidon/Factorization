package factorization.common;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialTransparent;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Icon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Coord;

public class BlockLightAir extends Block {
    static public final int air_md = 0;
    static public final int fire_md = 1;
    static MaterialTransparent actuallyTransparentMaterial = new MaterialTransparent(Material.air.materialMapColor) {
        public boolean isOpaque() {
            return true;
        }
    };

    public BlockLightAir(int id) {
        super(id, Material.air);
        setLightValue(1F);
        setHardness(0.1F);
        setResistance(0.1F);
        setUnlocalizedName("lightair");
        if (Core.debug_light_air) {
            float r = 0.1F;
            float b = 0.5F;
            setBlockBounds(b - r, b - r, b - r, b + r, b + r, b + r);
        } else {
            float nowhere = -10000F;
            setBlockBounds(nowhere, nowhere, nowhere, nowhere, nowhere, nowhere);
        }
    }

    @Override
    public void breakBlock(World w, int x, int y, int z, int id, int md) {
        //Don't need super calls because we don't carry TEs
        if (w.isRemote) {
            return;
        }
        if (md == air_md) {
            if (TileEntityWrathLamp.isUpdating) {
                return;
            }
            TileEntityWrathLamp.doAirCheck(w, x, y, z);
            int below = w.getBlockId(x, y - 1, z);
            if (below == blockID) {
                w.scheduleBlockUpdate(x, y - 1, z, blockID, 1);
            }
        }
    }
    
    @Override
    public void registerIcon(IconRegister reg) { }

    @Override
    public String getTextureFile() {
        if (Core.debug_light_air) {
            return Block.glowStone.getTextureFile();
        }
        return Core.texture_file_block;
    }
    
    @Override
    public Icon getBlockTextureFromSideAndMetadata(int side, int md) {
        if (Core.debug_light_air) {
            return Block.glowStone.getBlockTextureFromSide(0);
        }
        return BlockIcons.transparent;
    }

    static Random rand = new Random();

    @Override
    public void onNeighborBlockChange(World w, int x, int y, int z, int neighborID) {
        int md = w.getBlockMetadata(x, y, z);
        if (md == air_md) {
            Coord here = new Coord(w, x, y, z);
            if (neighborID == Block.cobblestoneWall.blockID) {
                if (w.getBlockId(x, y - 1, z) == Block.cobblestoneWall.blockID) {
                    here.setId(0);
                }
            }
            TileEntityWrathLamp.doAirCheck(w, x, y, z);
            Coord above = new Coord(w, x, y + 1, z);
            Block b = above.getBlock();
            if (b != null && !above.isAir()) {
                //a li'l hack for sand
                here.setId(0, false);
                b.updateTick(w, x, y + 1, z, rand);
                here.setIdMd(blockID, air_md, false);
            }
        }
        if (md == fire_md) {
            if (w.isAirBlock(x - 1, y, z) && w.isAirBlock(x + 1, y, z) && w.isAirBlock(x, y - 1, z) && w.isAirBlock(x, y + 1, z) && w.isAirBlock(x, y, z - 1) && w.isAirBlock(x, y, z + 1)) {
                w.setBlockAndMetadataWithNotify(x, y, z, 0, 0, Coord.UPDATE);
            }
        }
    }

    @Override
    public boolean getTickRandomly() {
        return true;
    }

    @Override
    public void updateTick(World w, int x, int y, int z, Random par5Random) {
        int md = w.getBlockMetadata(x, y, z);
        if (md == air_md) {
            TileEntityWrathLamp.doAirCheck(w, x, y, z);
        }
    }

    // Features
    @Override
    public int idDropped(int i, Random random, int j) {
        return 0;
    }

    @Override
    public boolean isBlockReplaceable(World world, int x, int y, int z) {
        //XXX: This doesn't actually work
        Coord here = new Coord(world, x, y, z);
        if (here.getMd() == fire_md) {
            return false;
        }
        return true;
    }

    @Override
    public boolean isAirBlock(World world, int i, int j, int k) {
        return true;
    }

    @Override
    public boolean isBlockBurning(World world, int x, int y, int z) {
        int id = world.getBlockId(x, y, z);
        int md = world.getBlockMetadata(x, y, z);
        return id == this.blockID && md == fire_md;
    }

    // Rendering
    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public int getRenderType() {
        return Core.debug_light_air ? 0 : -1;
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
    }

    @Override
    public int getMobilityFlag() {
        return 1; //can't push, but can overwrite
    }

    @Override
    public TileEntity createTileEntity(World world, int md) {
        if (md == fire_md) {
            return new TileEntityWrathFire();
        }
        return null;
    }

    @Override
    public boolean hasTileEntity(int md) {
        if (md == fire_md) {
            return true;
        }
        return false;
    }

    @Override
    public int getLightValue(IBlockAccess world, int x, int y, int z) {
        int md = world.getBlockMetadata(x, y, z);
        if (md == fire_md) {
            return 7;
        }
        if (md == air_md) {
            return super.getLightValue(world, x, y, z);
        }
        return 0;
    }

    @Override
    //-- server. I am so sick and tired of your BULLSHIT
    public void randomDisplayTick(World w, int x, int y, int z, Random rand) {
        Core.proxy.randomDisplayTickFor(w, x, y, z, rand);
    }
}
