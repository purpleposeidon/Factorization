package factorization.beauty;

import factorization.charge.TileEntityCaliometricBurner;
import factorization.servo.ItemMatrixProgrammer;
import factorization.shared.Core;
import factorization.shared.ItemFactorization;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;

public class ItemGrossFood extends ItemFactorization {
    boolean isInsane;

    public ItemGrossFood(String name, Core.TabType tabType, boolean isInsane) {
        super(name, tabType);
        this.isInsane = isInsane;
        setMaxStackSize(16);
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.EAT;
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World world, EntityPlayer player) {
        if (lmpFeed(player, stack)) {
            stack.splitStack(1);
            return stack;
        }
        if (isInsane) {
            for (Potion potion : new Potion[] {
                    Potion.weakness,
                    Potion.digSlowdown,
                    Potion.moveSlowdown,
                    Potion.blindness,
                    Potion.wither
            }) {
                if (player.getActivePotionEffect(potion) != null) continue;
                player.addPotionEffect(new PotionEffect(potion.getId(), 20 * 20, 4, true, true));
                return stack;
            }
        }
        if (world.isRemote) return stack;
        player.dropPlayerItemWithRandomChoice(stack, false);
        return null;
    }

    boolean lmpFeed(EntityPlayer player, ItemStack stack) {
        ItemStack helmet = player.getCurrentArmor(3);
        if (helmet == null) return false;
        if (!(helmet.getItem() instanceof ItemMatrixProgrammer)) return false;
        TileEntityCaliometricBurner.FoodInfo food = TileEntityCaliometricBurner.lookup(stack);
        if (food == null) return false;
        if (player.worldObj.isRemote) return true;
        player.getFoodStats().addStats(food.heal, (float) food.sat);
        return true;
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 32;
    }

    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (player.canEat(false)) {
            player.setItemInUse(stack, getMaxItemUseDuration(stack));
        }
        return stack;
    }
}
