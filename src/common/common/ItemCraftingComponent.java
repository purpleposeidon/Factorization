package factorization.common;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import factorization.common.Core.TabType;

public class ItemCraftingComponent extends Item {

    public ItemCraftingComponent(int id, String name) {
        super(id);
        setUnlocalizedName("factorization:" + name.replace('.', '/'));
        Core.tab(this, TabType.MATERIALS);
    }

    @Override
    public void addInformation(ItemStack is, EntityPlayer player, List infoList, boolean verbose) {
        Core.brand(infoList);
    }
}
