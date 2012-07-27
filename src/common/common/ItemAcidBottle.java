package factorization.common;

import net.minecraft.src.EntityPlayer;
import net.minecraft.src.EnumAction;
import net.minecraft.src.FactorizationHack;
import net.minecraft.src.ItemStack;
import net.minecraft.src.World;

public class ItemAcidBottle extends ItemCraftingComponent {

    public ItemAcidBottle(int id, String itemName, int icon) {
        super(id, itemName, icon);
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
        if (!Core.instance.isCannonical(w)) {
            return is;
        }
        FactorizationHack.damageEntity(player, FactorizationHack.acidBurn, 12);
        player.getFoodStats().addStats(-20, 0);
        return is;
    }

    @Override
    public boolean onItemUse(ItemStack par1ItemStack, EntityPlayer par2EntityPlayer,
            World par3World, int par4, int par5, int par6, int par7) {
        return false;
    }
}
