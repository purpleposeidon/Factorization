package factorization.src;

import net.minecraft.src.Block;
import net.minecraft.src.DamageSource;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.Material;
import net.minecraft.src.forge.ArmorProperties;

class MechaBuoyantBarrel extends Item implements IMechaUpgrade {

	protected MechaBuoyantBarrel(int par1) {
		super(par1);
	}

	@Override
	public ItemStack tickUpgrade(EntityPlayer player, ItemStack armor, ItemStack upgrade,
			boolean enabled) {
		if (!enabled) {
			return null;
		}
		Coord head = new Coord(player.worldObj, player.posX, player.posY - 0.5, player.posZ);
		Block b = head.getBlock();
		boolean headInWater = false;
		if (b != null) {
			headInWater = b.blockMaterial == Material.water;
		}
		if (player.isInWater()) {
			float maxSpeed = 0.5F;
			float accel = 0.020F;
			if (headInWater) {
				accel = 0.1F;
			}
			if (armor.getItem() == Core.registry.mecha_foot) {
				//bonus for water-walking...
				maxSpeed = 0.8F;
				accel *= 1.2;
			}
			if (player.motionY < maxSpeed) {
				player.motionY += accel;
				if (player.motionY > maxSpeed) {
					player.motionY = maxSpeed;
				}
			}
		}
		return null;
	}

	@Override
	public void addArmorProperties(ItemStack is, ArmorProperties armor) {
	}

	@Override
	public int getArmorDisplay(ItemStack is) {
		return 0;
	}

	@Override
	public boolean damageArmor(EntityLiving entity, ItemStack stack, DamageSource source,
			int damage, int slot) {
		return false;
	}

	@Override
	public String getItemName() {
		return "mecha.buoyantbarrel";
	}

	@Override
	public String getItemNameIS(ItemStack par1ItemStack) {
		return "Buoyant Barrel";
	}

	@Override
	public String getDescription() {
		return "Rise quickly when underwater";
	}
}