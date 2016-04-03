package factorization.beauty;

import factorization.common.FactoryType;
import factorization.shared.SimpleFzBlock;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.IBlockAccess;

public class BlockShaftGen extends SimpleFzBlock {
    public static final PropertyDirection DIRECTION = PropertyDirection.create("facing");
    public static final PropertyBool POWERED = PropertyBool.create("powered");

    public BlockShaftGen() {
        super(Material.iron, FactoryType.SHAFT_GEN);
    }

    @Override
    protected BlockState createBlockState() {
        return new BlockState(this, DIRECTION, POWERED);
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityShaftGen) {
            TileEntityShaftGen sg = (TileEntityShaftGen) te;
            return getDefaultState().withProperty(DIRECTION, sg.shaft_direction).withProperty(POWERED, sg.on);
        }
        return super.getActualState(state, world, pos);
    }

    @Override
    public boolean isBlockSolid(IBlockAccess world, BlockPos pos, EnumFacing side) {
        return false;
    }

    @Override
    public boolean isOpaqueCube() {
        return true;
    }

    @Override
    public boolean isNormalCube() {
        return true;
    }
}
