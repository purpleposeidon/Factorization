package factorization.beauty;

import factorization.common.FactoryType;
import factorization.shared.SimpleFzBlock;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;

public class BlockWindmill extends SimpleFzBlock {
    // FIXME: Code duplication
    public static final PropertyDirection DIRECTION = PropertyDirection.create("facing");

    public BlockWindmill() {
        super(Material.wood, FactoryType.WIND_MILL_GEN);
    }

    @Override
    protected BlockState createBlockState() {
        return new BlockState(this, DIRECTION);
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityWindMill) {
            TileEntityWindMill wm = (TileEntityWindMill) te;
            return getDefaultState().withProperty(DIRECTION, wm.sailDirection);
        }
        return super.getActualState(state, world, pos);
    }
}
