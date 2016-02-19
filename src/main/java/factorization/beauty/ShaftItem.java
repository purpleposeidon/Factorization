package factorization.beauty;

import factorization.shared.Core;
import factorization.shared.ItemDynamic;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.List;

public class ShaftItem extends ItemDynamic {
    public ShaftItem(Block block) {
        super(block);
        Core.tab(this, Core.TabType.BLOCKS);
    }

    @Override
    public void getSubItems(Item itemIn, CreativeTabs tab, List<ItemStack> subItems) {
        subItems.addAll(TileEntityShaft.instances);
    }
}
