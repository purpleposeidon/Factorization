package factorization.flat.render;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.IBlockAccess;

class SideSelectors extends Block {
    static Block get(EnumFacing side) {
        return instances[side.ordinal()];
    }

    static final Block[] instances = new Block[6];
    static {
        for (EnumFacing f : EnumFacing.VALUES) {
            instances[f.ordinal()] = new SideSelectors(f);
        }
    }

    private final EnumFacing side;
    private SideSelectors(EnumFacing side) {
        super(Material.rock);
        this.side = side;
    }

    @Override
    public boolean shouldSideBeRendered(IBlockAccess worldIn, BlockPos pos, EnumFacing side) {
        return side == this.side;
    }
}
