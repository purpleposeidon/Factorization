package factorization.src;

import net.minecraft.src.DamageSource;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.EnumArmorMaterial;
import net.minecraft.src.ItemArmor;
import net.minecraft.src.ItemStack;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.forge.ArmorProperties;
import net.minecraft.src.forge.ISpecialArmor;
import net.minecraft.src.forge.ITextureProvider;

public class MechaArmor extends ItemArmor
		implements ISpecialArmor, ITextureProvider {
	int slotCount = 2;

	public MechaArmor(int par1, int armorType) {
		super(par1, EnumArmorMaterial.CHAIN, 0, armorType);
		setMaxDamage(0); //never break!
		setItemName("item.mechaArmor" + armorType);
	}

	//mecha features
	MechaArmor setSlotCount(int count) {
		slotCount = count;
		return this;
	}

	ItemStack getStackInSlot(ItemStack is, int slot) {
		if (slot < 0 || slot >= slotCount) {
			return null;
		}
		NBTTagCompound tag = is.getTagCompound();
		if (tag == null) {
			return null;
		}
		String index = "slot" + slot;
		if (!tag.hasKey(index)) {
			return null;
		}
		return ItemStack.loadItemStackFromNBT(tag.getCompoundTag(index));
	}

	IMechaUpgrade getUpgradeInSlot(ItemStack is, int slot) {
		return getUpgrade(getStackInSlot(is, slot));
	}

	IMechaUpgrade getUpgrade(ItemStack i) {
		if (i == null || !(i.getItem() instanceof IMechaUpgrade)) {
			return null;
		}
		return (IMechaUpgrade) i.getItem();
	}

	void setStackInSlot(ItemStack is, int slot, ItemStack stack) {
		if (slot < 0 || slot >= slotCount) {
			return;
		}
		if (is.getTagCompound() == null) {
			is.setTagCompound(new NBTTagCompound());
		}
		if (stack == null) {
			is.getTagCompound().setTag("slot" + slot, new NBTTagCompound());
			return;
		}
		NBTTagCompound itemTag = new NBTTagCompound();
		stack.writeToNBT(itemTag);
		is.getTagCompound().setCompoundTag("slot" + slot, itemTag);
	}

	static void onTickPlayer(EntityPlayer player) {
		for (ItemStack armorStack : player.inventory.armorInventory) {
			if (armorStack == null) {
				return;
			}
			if (armorStack.getItem() instanceof MechaArmor) {
				((MechaArmor) armorStack.getItem()).tickArmor(player, armorStack);
			}
		}
	}

	void tickArmor(EntityPlayer player, ItemStack armorStack) {
		for (int i = 0; i < slotCount; i++) {
			ItemStack is = getStackInSlot(armorStack, i);
			IMechaUpgrade up = getUpgrade(is);
			if (up != null) {
				up.tickUpgrade(player, is);
			}
		}
	}

	//vanilla armor feature
	@Override
	public int getItemEnchantability() {
		return -1;
	}

	public String getArmorTextureFile(ItemStack itemstack) {
		//presumably we'll have to change this depending on what type of armor we are
		//XXX NOTE: LexManos needs to put IArmorTextureProvider in common
		//For now, the client uses render.MechaArmorTextured
		if (armorType == 2) {
			return Core.texture_dir + "mecha_armor_2.png";
		}
		return Core.texture_dir + "mecha_armor_1.png";
	}

	// @Override -- XXX Waiting for MCP update
	public boolean getShareNBT() {
		return true;
	}

	// @Override -- XXX TODO SERVER get___?
	public boolean func_46003_i() {
		return getShareNBT();
	}

	// @Override -- XXX TODO CLIENT get___?
	public boolean func_46056_k() {
		return getShareNBT();
	}

	@Override
	public ArmorProperties getProperties(EntityLiving player, ItemStack armor, DamageSource source,
			double damage, int slot) {
		ArmorProperties prop = new ArmorProperties(0, 0, 0);
		MechaArmor ma = (MechaArmor) armor.getItem();
		boolean found_vanilla_armor = false;
		for (int i = 0; i < slotCount; i++) {
			ItemStack is = ma.getStackInSlot(armor, i);
			if (is == null) {
				continue;
			}
			if (is.getItem().getClass() == ItemArmor.class) {
				if (found_vanilla_armor) {
					continue;
				}
				found_vanilla_armor = true;
				ItemArmor ar = (ItemArmor) is.getItem();
				prop.AbsorbRatio += ar.damageReduceAmount / 25D;
				prop.AbsorbMax += ar.getMaxDamage() + 1 - is.getItemDamage();
			}
			IMechaUpgrade up = getUpgrade(is);
			if (up != null) {
				up.addArmorProperties(is, prop);
			}
		}
		return prop;
	}

	@Override
	public int getArmorDisplay(EntityPlayer player, ItemStack armor, int slot) {
		int ret = 0;
		MechaArmor ma = (MechaArmor) armor.getItem();
		boolean found_vanilla_armor = false;
		for (int i = 0; i < slotCount; i++) {
			ItemStack is = ma.getStackInSlot(armor, i);
			if (is == null) {
				continue;
			}
			if (is.getItem().getClass() == ItemArmor.class) {
				if (found_vanilla_armor) {
					continue;
				}
				found_vanilla_armor = true;
				ret += ((ItemArmor) is.getItem()).damageReduceAmount;
				continue;
			}
			IMechaUpgrade up = getUpgrade(is);
			if (up != null) {
				ret += up.getArmorDisplay(is);
			}
		}
		return ret;
	}

	@Override
	public void damageArmor(EntityLiving entity, ItemStack armor, DamageSource source, int damage,
			int slot) {
		MechaArmor ma = (MechaArmor) armor.getItem();
		boolean found_vanilla_armor = false;
		for (int i = 0; i < slotCount; i++) {
			ItemStack is = ma.getStackInSlot(armor, i);
			if (is == null) {
				continue;
			}
			if (is.getItem().getClass() == ItemArmor.class) {
				if (found_vanilla_armor) {
					continue;
				}
				found_vanilla_armor = true;
				is.damageItem(damage, entity);
				if (is.stackSize <= 0) {
					is = null;
				}
				ma.setStackInSlot(armor, i, is);
				continue;
			}
			IMechaUpgrade up = getUpgrade(is);
			if (up != null) {
				if (up.damageArmor(entity, is, source, damage, slot)) {
					ma.setStackInSlot(armor, i, is);
				}
			}
		}
	}

	@Override
	public String getTextureFile() {
		return Core.texture_file_item;
	}

	//@Override seeerveerr
	public int getIconFromDamage(int par1) {
		return (4 + armorType) * 16;
	}

}
