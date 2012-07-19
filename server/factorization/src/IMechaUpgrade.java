package factorization.src;

import net.minecraft.src.DamageSource;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ItemStack;
import net.minecraft.src.forge.ArmorProperties;

public interface IMechaUpgrade {
	ItemStack tickUpgrade(EntityPlayer player, ItemStack is);

	void addArmorProperties(ItemStack is, ArmorProperties armor);

	int getArmorDisplay(ItemStack is);

	///Return true if we need to update the stack's NBT
	boolean damageArmor(EntityLiving entity, ItemStack stack, DamageSource source, int damage,
			int slot);
}
