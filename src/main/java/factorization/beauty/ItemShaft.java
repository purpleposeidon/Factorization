package factorization.beauty;

import factorization.shared.Core;
import factorization.shared.ItemDynamic;
import factorization.util.LangUtil;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.List;

public class ItemShaft extends ItemDynamic {
    public ItemShaft(Block block) {
        super(block);
        Core.tab(this, Core.TabType.BLOCKS);
    }

    @Override
    public void getSubItems(Item itemIn, CreativeTabs tab, List<ItemStack> subItems) {
        subItems.addAll(TileEntityShaft.instances);
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        ShaftItemCache info = new ShaftItemCache(stack);
        return LangUtil.translateWithCorrectableFormat("factorization.itemshaft.format", info.log.getDisplayName());
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer playerIn, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, playerIn, tooltip, advanced);
        ShaftItemCache info = new ShaftItemCache(stack);
        tooltip.add(LangUtil.translateThis("factorization.itemshaft.sheared" + info.sheared));
    }
}
