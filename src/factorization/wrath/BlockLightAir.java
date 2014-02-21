package factorization.wrath;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialTransparent;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import factorization.api.Coord;
import factorization.common.BlockIcons;
import factorization.common.FzConfig;

public class BlockLightAir extends Block {
    static public final int air_md = 0;
    static MaterialTransparent actuallyTransparentMaterial = new MaterialTransparent(Material.air.getMaterialMapColor()) {
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
            return Blocks.glowstone.getIconFromSide(0);
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
    public Item getItemDropped(int p_149650_1_, Random p_149650_2_, int p_149650_3_) {
        return null;
    }

    @Override
    public boolean isReplaceable(IBlockAccess world, int x, int y, int z) {
        return true;
    }

    @Override
    public boolean isAir(IBlockAccess world, int i, int j, int k) {
        return true;
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
}
