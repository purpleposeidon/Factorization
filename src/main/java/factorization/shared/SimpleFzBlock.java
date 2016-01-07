package factorization.shared;

import factorization.common.FactoryType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.List;

public class SimpleFzBlock extends BlockFactorization {
    final FactoryType factoryType;
    final BlockClass blockClass;

    public SimpleFzBlock(Material material, FactoryType ft) {
        super(material);
        setUnlocalizedName("factorization.factoryBlock." + ft.toString());
        this.factoryType = ft;
        this.blockClass = ft.makeTileEntity().getBlockClass();
    }

    @Override
    public FactoryType getFactoryType(int md) {
        return factoryType;
    }

    @Override
    public BlockClass getClass(IBlockAccess world, BlockPos pos) {
        return blockClass;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return factoryType.makeTileEntity();
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
    public void getSubBlocks(Item me, CreativeTabs tab, List<ItemStack> itemList) {
        itemList.add(new ItemStack(this));
    }
}
