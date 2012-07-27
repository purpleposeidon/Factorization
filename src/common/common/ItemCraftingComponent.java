package factorization.common;

import net.minecraft.src.Item;
import net.minecraft.src.forge.ITextureProvider;

public class ItemCraftingComponent extends Item implements ITextureProvider {
    int icon;

    public ItemCraftingComponent(int id, String itemName, int icon) {
        super(id);
        setItemName(itemName);
        Core.instance.addName(this, itemName);
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
