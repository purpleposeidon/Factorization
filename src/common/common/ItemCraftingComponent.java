package factorization.common;

import java.util.List;

import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import factorization.common.Core.TabType;

public class ItemCraftingComponent extends Item {
    int icon;
    String texture_name;

    public ItemCraftingComponent(int id, String name) {
        super(id);
        setUnlocalizedName("factorization.item." + name);
        Core.tab(this, TabType.MATERIALS);
        texture_name = "factroziation/texture/" + name;
    }

    @Override
    public void addInformation(ItemStack is, EntityPlayer player, List infoList, boolean verbose) {
        Core.brand(infoList);
    }
    
    @Override
    public void registerIcon(IconRegister reg) {
        iconIndex = Core.texture(reg, texture_name);
    }
    
}
