package factorization.common;

import java.util.List;

import net.minecraft.src.CreativeTabs;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.EnumAction;
import net.minecraft.src.ItemStack;
import net.minecraft.src.World;

public class ItemAcidBottle extends ItemCraftingComponent {

    public ItemAcidBottle(int id, String itemName, int icon) {
        super(id, itemName, icon);
        setTabToDisplayOn(CreativeTabs.tabMisc);
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
    public void addInformation(ItemStack is, List infoList) {
        super.addInformation(is, infoList);
        //Core.brand(infoList); taken care of in super
    }
}
