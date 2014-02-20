package factorization.wrath;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialTransparent;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import factorization.api.Coord;
import factorization.common.BlockIcons;
import factorization.common.FzConfig;
import factorization.shared.Core;

public class BlockLightAir extends Block {
    static public final int air_md = 0;
    static MaterialTransparent actuallyTransparentMaterial = new MaterialTransparent(Material.air.materialMapColor) {
        @Override
        public boolean isOpaque() {
            return true;
        }
    };

    public BlockLightAir() {
        super(Material.air);
        setLightLevel(1F);
        setHardness(0.1F);
        setResistance(0.1F);
        setBlockName("lightair");
        if (FzConfig.debug_light_air) {
            float r = 0.1F;
            float b = 0.5F;
            setBlockBounds(b - r, b - r, b - r, b + r, b + r, b + r);
        } else {
            float nowhere = -10000F;
            setBlockBounds(nowhere, nowhere, nowhere, nowhere, nowhere, nowhere);
        }
    }

    @Override
    public void breakBlock(World w, int x, int y, int z, Block id, int md) {
        //Don't need super calls because we don't carry TEs
        if (w.isRemote) {
            return;
        }
        if (TileEntityWrathLamp.isUpdating) {
            return;
        }
        TileEntityWrathLamp.doAirCheck(w, x, y, z);
        Block below = w.getBlock(x, y - 1, z);
        if (below == this) {
            w.scheduleBlockUpdate(x, y - 1, z, this, 1);
        }
    }
    
    @Override
    public void registerBlockIcons(IIconRegister reg) { }
    
    @Override
    public IIcon getIcon(int side, int md) {
        if (FzConfig.debug_light_air) {
            return Blocks.glowstone.getBlockTextureFromSide(0);
        }
        return BlockIcons.transparent;
    }

    static Random rand = new Random();

    @Override
    public void onNeighborBlockChange(World w, int x, int y, int z, Block neighborID) {
        int md = w.getBlockMetadata(x, y, z);
        int notifyFlag = Coord.NOTIFY_NEIGHBORS | Coord.UPDATE;
        if (neighborID == Blocks.cobblestone_wall) {
            if (w.getBlock(x, y - 1, z) == Blocks.cobblestone_wall) {
                w.setBlockToAir(x, y, z);
                return;
            }
        }
        TileEntityWrathLamp.doAirCheck(w, x, y, z);
        Block b = Blocks.blocksList[w.getBlock(x, y + 1, z)];
        if (b != null && !b.isAir(w, x, y + 1, z)) {
            //a li'l hack for sand
            w.setBlock(x, y, z, 0, 0, 0);
            b.updateTick(w, x, y + 1, z, rand);
            w.setBlock(x, y, z, blockID, air_md, 0);
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
    public boolean isReplaceable(IBlockAccess world, int x, int y, int z) {
        return true;
    }

    @Override
    public boolean isAir(IBlockAccess world, int i, int j, int k) {
        return true;
    }

    @Override
    public boolean isBurning(IBlockAccess world, int x, int y, int z) {
        return world.getBlock(x, y, z) == this && world.getBlockMetadata(x, y, z) == fire_md;
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
        return FzConfig.debug_light_air ? 0 : -1;
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
