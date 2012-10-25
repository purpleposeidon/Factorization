package factorization.common;

import java.util.List;

import net.minecraft.src.EntityPlayer;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import factorization.common.Core.TabType;

public class ItemCraftingComponent extends Item {
    int icon;

    public ItemCraftingComponent(int id, String itemName, int icon) {
        super(id);
        setItemName(itemName);
        Core.proxy.addName(this, itemName);
        this.icon = icon;
        Core.tab(this, TabType.MATERIALS);
        setTextureFile(Core.texture_file_item);
    }

    @Override
    //-- SERVERf
    public int getIconFromDamage(int par1) {
        return icon;
    }

    @Override
    public void addInformation(ItemStack is, EntityPlayer player, List infoList, boolean verbose) {
        Core.brand(infoList);
    }
}
