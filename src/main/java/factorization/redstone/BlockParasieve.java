package factorization.redstone;

import factorization.common.FactoryType;
import factorization.shared.BlockClass;
import factorization.shared.BlockFactorization;
import factorization.shared.Core;
import factorization.shared.SimpleFzBlock;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.List;

public class BlockParasieve extends SimpleFzBlock {
    public static final IProperty<EnumFacing> FACING = PropertyDirection.create("direction");
    public BlockParasieve() {
        super(Core.registry.materialMachine, FactoryType.PARASIEVE);
    }

    @Override
    protected BlockState createBlockState() {
        return new BlockState(this, FACING);
    }

    @Override
    public boolean isBlockSolid(IBlockAccess world, BlockPos pos, EnumFacing side) {
        return true;
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityParaSieve) {
            TileEntityParaSieve sieve = (TileEntityParaSieve) te;
            return getDefaultState().withProperty(FACING, sieve.facing_direction);
        }
        return super.getActualState(state, worldIn, pos);
    }
}
