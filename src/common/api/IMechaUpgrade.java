package factorization.api;

import net.minecraft.src.DamageSource;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ItemStack;
import net.minecraft.src.forge.ArmorProperties;

public interface IMechaUpgrade {
    /**
     * 
     * @param player
     * @param armor
     *            Armor that contains the upgrade
     * @param is
     *            The upgrade
     * @param isEnabled
     *            If the upgrade has been activated
     * @return non-null to save changes to the item. Returning null causes no changes. Set stackSize to 0 to remove.
     */
    ItemStack tickUpgrade(EntityPlayer player, ItemStack armor, ItemStack upgrade, boolean isEnabled);

    void addArmorProperties(ItemStack is, ArmorProperties armor);

    /**
     * @param is
     *            the upgrade
     * @return how many halves of armor to draw
     */
    int getArmorDisplay(ItemStack is);

    /**
     * @param entity
     * @param stack
     * @param source
     * @param damage
     * @param slot
     * @return true if the stack's NBT needs to be updated
     */
    boolean damageArmor(EntityLiving entity, ItemStack stack, DamageSource source, int damage, int slot);

    String getDescription();
}
