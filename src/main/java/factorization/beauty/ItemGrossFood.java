package factorization.beauty;

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
        return EnumAction.eat;
    }

    @Override
    public ItemStack onEaten(ItemStack stack, World world, EntityPlayer player) {
        if (isInsane) {
            for (Potion potion : new Potion[] {
                    Potion.digSlowdown,
                    Potion.moveSlowdown,
                    Potion.confusion,
                    Potion.blindness,
                    Potion.wither
            }) {
                if (player.getActivePotionEffect(potion) != null) continue;
                player.addPotionEffect(new PotionEffect(potion.getId(), 20 * 20, 4, false));
                return stack;
            }
        }
        if (world.isRemote) return stack;
        player.dropPlayerItemWithRandomChoice(stack, false);
        return null;
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 32;
    }

    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (player.canEat(isInsane)) {
            player.setItemInUse(stack, getMaxItemUseDuration(stack));
        }
        return stack;
    }
}
