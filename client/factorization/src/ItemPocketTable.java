package factorization.src;

import net.minecraft.src.EntityPlayer;
import net.minecraft.src.InventoryPlayer;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.World;
import net.minecraft.src.forge.ITextureProvider;

public class ItemPocketTable extends Item implements ITextureProvider {

	public ItemPocketTable(int id) {
		super(id);
		setMaxStackSize(1);
	}

	@Override
	public String getTextureFile() {
		return Core.texture_file_item;
	}

	public int getIconFromDamage(int damage) {
		return 4;
	}

	//
	// @Override
	// public boolean onItemUseFirst(ItemStack stack, EntityPlayer player,
	// World world, int X, int Y, int Z, int side) {
	// player.openGui(FactorizationCore.instance, FactoryType.POCKETCRAFT.gui,
	// null, 0, 0, 0);
	// return true;
	// }
	//
	// @Override
	// public boolean onItemUse(ItemStack stack,
	// EntityPlayer player, World world, int X, int Y,
	// int Z, int side) {
	// // TODO Auto-generated method stub
	// return super.onItemUse(par1ItemStack, par2EntityPlayer, par3World, par4,
	// par5,
	// par6, par7);
	// }

	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
		ItemStack save = player.inventory.getItemStack();
		if (save != null) {
			player.inventory.setItemStack(null);
		}
		//XXX TODO: Chests stay open. Man, how do I fix this?
		player.openGui(Core.instance, FactoryType.NULLGUI.gui, null, 0, 0, 0);
		player.openGui(Core.instance, FactoryType.POCKETCRAFTGUI.gui, null, 0, 0, 0);
		if (save != null) {
			player.inventory.setItemStack(save);
			Core.instance.updateHeldItem(player);
		}
		return stack;
	}

	@Override
	public String getItemName() {
		return "Pocket Crafting Table";
	}

	@Override
	public String getItemNameIS(ItemStack par1ItemStack) {
		return getItemName();
	}

	ItemStack findPocket(EntityPlayer player) {
		InventoryPlayer inv = player.inventory;
		for (int i = 0; i < inv.getSizeInventory(); i++) {
			ItemStack is = inv.getStackInSlot(i);
			if (is == null) {
				continue;
			}
			if (is.getItem() == this) {
				return is;
			}
		}
		ItemStack mouse_item = player.inventory.getItemStack();
		if (mouse_item != null && mouse_item.getItem() == this) {
			return mouse_item;
		}
		return null;
	}

	public boolean tryOpen(EntityPlayer player) {
		ItemStack is = findPocket(player);
		if (is == null) {
			return false;
		}
		this.onItemRightClick(is, player.worldObj, player);
		return true;
	}
}
