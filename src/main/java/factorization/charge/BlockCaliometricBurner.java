package factorization.charge;

import factorization.common.FactoryType;
import factorization.shared.BlockClass;
import factorization.shared.BlockFactorization;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockCaliometricBurner extends BlockFactorization {
    public BlockCaliometricBurner() {
        super(Material.piston);
    }

    @Override
    public FactoryType getFactoryType(int md) {
        return FactoryType.CALIOMETRIC_BURNER;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new TileEntityCaliometricBurner();
    }

    @Override
    protected BlockState createBlockState() {
        return new BlockState(this);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return 0;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState();
    }

    @Override
    public BlockClass getClass(IBlockAccess world, BlockPos pos) {
        return BlockClass.Machine;
    }
}
