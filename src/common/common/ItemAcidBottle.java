package factorization.common;

import java.util.List;

import factorization.common.Core.TabType;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class ItemAcidBottle extends ItemCraftingComponent {

    public ItemAcidBottle(int id, String itemName, int icon) {
        super(id, itemName, icon);
        Core.tab(this, TabType.MISC);
    }

    public int getMaxItemUseDuration(ItemStack par1ItemStack) {
        return 32;
    }

    public EnumAction getItemUseAction(ItemStack par1ItemStack) {
        return EnumAction.drink;
    }

    public ItemStack onItemRightClick(ItemStack is, World w, EntityPlayer player) {
        player.setItemInUse(is, getMaxItemUseDuration(is));
        return is;
    }

    @Override
    public ItemStack onFoodEaten(ItemStack is, World w, EntityPlayer player) {
        is.stackSize--;
        Sound.acidBurn.playAt(player);
        if (w.isRemote) {
            return is;
        }
        FactorizationHack.damageEntity(player, FactorizationHack.acidBurn, 12);
        player.getFoodStats().addStats(-20, 0);
        return is;
    }

    @Override
    public void addInformation(ItemStack is, EntityPlayer player, List infoList, boolean verbose) {
        super.addInformation(is, player, infoList, verbose);
        //Core.brand(infoList); taken care of in super
    }
}
