 package factorization.common;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class ItemInverium extends ItemCraftingComponent {
    public ItemInverium(int id, String itemName) {
        super(id, itemName);
    }

    @Override
    public boolean hasEffect(ItemStack is) {
        return true;
    }
    
    //getColorFromDamage (or maybe getColorForRenderPass)
    @Override
    public int getColorFromItemStack(ItemStack is, int renderPass) {
        int now = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
        int r = (int) (Math.abs(Math.cos(now/32000))*0x20) + 0xD0;
        int g = Math.min(0xFF, r*2);
        int b = 0x20;
        return (r << 16) + (g << 8) + b;
    }
    
    @Override
    public void addInformation(ItemStack is, EntityPlayer player, List infoList, boolean verbose) {
        if (is.getItemDamage() == 1) {
            infoList.add("Temporary recipe");
        }
        super.addInformation(is, player, infoList, verbose);
    }
    
    public void getSubItems(int itemID, CreativeTabs tab, List list) {
        list.add(new ItemStack(itemID, 1, 1));
    }
}
