package factorization.charge.enet;

import factorization.common.FactoryType;
import factorization.shared.SimpleFzBlockCutout;
import factorization.util.ItemUtil;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.List;

public class BlockLeydenJar extends SimpleFzBlockCutout {
    public BlockLeydenJar() {
        super(Material.glass, FactoryType.LEYDENJAR);
        float s = 2F / 16F;
        setBlockBounds(s, 0, s, 1 - s, 1, 1 - s);
        // FIXME: LeydenJar items with damage of 1 should draw a sparkling. Have fun kids! Daddy neptune's gonna get drunk!
    }

    /*static final IProperty<Boolean> FULL = PropertyBool.create("full");

    @Override
    protected BlockState createBlockState() {
        return new BlockState(this, FULL);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(FULL, meta != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FULL) ? 1 : 0;
    }*/

    @Override
    public void getSubBlocks(Item me, CreativeTabs tab, List<ItemStack> itemList) {
        ItemStack empty = new ItemStack(this, 1, 0);
        ItemUtil.getTag(empty).setInteger("storage", 0);
        ItemStack full = new ItemStack(this, 1, 1);
        ItemUtil.getTag(full).setInteger("storage", TileEntityLeydenJar.MAX_STORAGE);
        itemList.add(empty);
        itemList.add(full);
    }
}

