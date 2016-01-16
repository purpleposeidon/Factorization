package factorization.beauty;

import factorization.common.FactoryType;
import factorization.shared.SimpleFzBlock;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;

public class BlockAnthroGen extends SimpleFzBlock {
    public static final PropertyBool LIT = PropertyBool.create("lit");

    public BlockAnthroGen(Material material, FactoryType ft) {
        super(material, ft);
    }

    @Override
    protected BlockState createBlockState() {
        return new BlockState(this, LIT);
    }

    @Override
    public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityAnthroGen) {
            TileEntityAnthroGen gen = (TileEntityAnthroGen) te;
            return getDefaultState().withProperty(LIT, gen.isLit);
        }
        return super.getExtendedState(state, world, pos);
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }
}
