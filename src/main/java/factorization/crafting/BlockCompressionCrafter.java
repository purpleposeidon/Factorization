package factorization.crafting;

import factorization.common.FactoryType;
import factorization.shared.SimpleFzBlockCutout;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraft.world.IBlockAccess;

public class BlockCompressionCrafter extends SimpleFzBlockCutout {
    public static final PropertyDirection FACING = PropertyDirection.create("facing");

    public BlockCompressionCrafter() {
        super(Material.iron, FactoryType.COMPRESSIONCRAFTER);
    }

    @Override
    public boolean canRenderInLayer(EnumWorldBlockLayer layer) {
        return layer == EnumWorldBlockLayer.CUTOUT;
    }

    @Override
    protected BlockState createBlockState() {
        return new BlockState(this, FACING);
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        TileEntity tile = worldIn.getTileEntity(pos);
        if (tile instanceof TileEntityCompressionCrafter) {
            return state.withProperty(FACING, ((TileEntityCompressionCrafter) tile).facing);
        }
        return state;
    }
}
