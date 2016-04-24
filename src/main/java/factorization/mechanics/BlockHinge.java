package factorization.mechanics;

import factorization.common.FactoryType;
import factorization.shared.SimpleFzBlock;
import factorization.shared.TileEntityCommon;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.IBlockAccess;

public class BlockHinge extends SimpleFzBlock {
    public static final IProperty<EnumFacing> FACING = PropertyDirection.create("facing");
    public BlockHinge(Material material, FactoryType ft) {
        super(material, ft);
    }

    @Override
    protected BlockState createBlockState() {
        return new BlockState(this, FACING);
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        TileEntityCommon tec = get(worldIn, pos);
        if (!(tec instanceof TileEntityHinge)) {
            return super.getActualState(state, worldIn, pos);
        }
        TileEntityHinge hinge = (TileEntityHinge) tec;
        return state.withProperty(FACING, hinge.facing.facing);
    }
}
