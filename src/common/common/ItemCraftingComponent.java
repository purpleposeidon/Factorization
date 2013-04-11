package factorization.common;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import factorization.common.Core.TabType;

public class ItemCraftingComponent extends Item {
    String hint_text = null;

    public ItemCraftingComponent(int id, String name) {
        super(id);
        setUnlocalizedName("factorization:" + name.replace('.', '/'));
        Core.tab(this, TabType.MATERIALS);
    }
    
    public ItemCraftingComponent setHint(String hint) {
        this.hint_text = hint;
        return this;
    }
    
    @Override
    public void addInformation(ItemStack is, EntityPlayer player, List infoList, boolean verbose) {
        if (hint_text != null) {
            infoList.add(hint_text);
        }
        Core.brand(infoList);
    }
}
