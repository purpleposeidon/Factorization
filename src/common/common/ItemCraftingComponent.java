package factorization.common;

import java.util.List;

import net.minecraft.src.CreativeTabs;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;

public class ItemCraftingComponent extends Item {
    int icon;

    public ItemCraftingComponent(int id, String itemName, int icon) {
        super(id);
        setItemName(itemName);
        Core.proxy.addName(this, itemName);
        this.icon = icon;
        setTabToDisplayOn(CreativeTabs.tabMaterials);
        setTextureFile(Core.texture_file_item);
    }

    @Override
    //-- SERVERf
    public int getIconFromDamage(int par1) {
        return icon;
    }

    @Override
    public void addInformation(ItemStack is, List infoList) {
        super.addInformation(is, infoList);
        Core.brand(infoList);
    }
}
