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

public class BlockWaterwheel extends SimpleFzBlock {
    public static final PropertyDirection DIRECTION = PropertyDirection.create("facing");

    public BlockWaterwheel() {
        super(Material.wood, FactoryType.WATER_WHEEL_GEN);
    }

    @Override
    protected BlockState createBlockState() {
        return new BlockState(this, DIRECTION);
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityWaterWheel) {
            TileEntityWaterWheel ww = (TileEntityWaterWheel) te;
            return getDefaultState().withProperty(DIRECTION, ww.wheelDirection);
        }
        return super.getActualState(state, world, pos);
    }
}
