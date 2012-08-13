package factorization.common;

import net.minecraft.src.Item;

public class ItemCraftingComponent extends Item {
    int icon;

    public ItemCraftingComponent(int id, String itemName, int icon) {
        super(id);
        setItemName(itemName);
        Core.proxy.addName(this, itemName);
        this.icon = icon;
    }

    @Override
    public String getTextureFile() {
        return Core.texture_file_item;
    }

    //@Override -- SERVERf
    public int getIconFromDamage(int par1) {
        return icon;
    }
}
