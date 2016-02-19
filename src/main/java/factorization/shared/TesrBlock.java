package factorization.shared;

import factorization.common.FactoryType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.IBlockAccess;

public class TesrBlock extends SimpleFzBlock {
    public TesrBlock(Material material, FactoryType ft) {
        super(material, ft);
    }

    @Override
    protected BlockState createBlockState() {
        return new BlockState(this);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState();
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return 0;
    }

    @Override
    public int getRenderType() {
        return 2 /* TESR */;
    }

    @Override
    public boolean isBlockSolid(IBlockAccess world, BlockPos pos, EnumFacing side) {
        return false;
    }
}
