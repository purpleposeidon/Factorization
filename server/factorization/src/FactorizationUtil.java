package factorization.src;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import net.minecraft.src.Block;
import net.minecraft.src.EntityItem;
import net.minecraft.src.IInventory;
import net.minecraft.src.IRecipe;
import net.minecraft.src.InventoryLargeChest;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.ShapedRecipes;
import net.minecraft.src.ShapelessRecipes;
import net.minecraft.src.TileEntityChest;
import net.minecraft.src.World;

public class FactorizationUtil {
	public static NBTTagCompound getTag(ItemStack is) {
		NBTTagCompound ret = is.getTagCompound();
		if (ret == null) {
			ret = new NBTTagCompound();
			is.setTagCompound(ret);
		}
		return ret;
	}

	public static ItemStack transferStackToArea(IInventory srcInv, int slotIndex,
			IInventory destInv, Iterable<Integer> targetSlots) {
		//this is probably all wrong. >_>
		ItemStack is = srcInv.getStackInSlot(slotIndex);
		if (is == null || is.stackSize == 0) {
			return null;
		}
		// fill up pre-existing stacks
		for (int i : targetSlots) {
			ItemStack target = destInv.getStackInSlot(i);
			if (target == null) {
				continue;
			}
			if (is.isItemEqual(target)) {
				int free_space = target.getMaxStackSize() - target.stackSize;
				int incr = Math.min(free_space, is.stackSize);
				if (incr <= 0) {
					continue;
				}
				is.stackSize -= incr;
				target.stackSize += incr;
			}
			if (is.stackSize <= 0) {
				srcInv.setInventorySlotContents(slotIndex, null);
				return null;
			}
		}
		// make new stacks
		for (int i : targetSlots) {
			ItemStack target = destInv.getStackInSlot(i);
			if (target == null) {
				destInv.setInventorySlotContents(i, is.copy());
				is.stackSize = 0;
				srcInv.setInventorySlotContents(slotIndex, null);
				return null;
			}
		}
		if (is.stackSize <= 0) {
			srcInv.setInventorySlotContents(slotIndex, null);
			return null;
		}
		srcInv.setInventorySlotContents(slotIndex, is);
		return is;
	}

	/**
	 * If you are accessing multiple chests, and some might be adjacent you'll want to treat them as a double chest. Calling this function with a lower chest
	 * will return 'null'; calling with an upper chest will return an InventoryLargeChest. If it's a single chest, it'll return that chest.
	 * 
	 * @param chest
	 * @return
	 */
	public static IInventory openDoubleChest(TileEntityChest chest) {
		IInventory origChest = (TileEntityChest) chest;
		World world = chest.worldObj;
		int i = chest.xCoord, j = chest.yCoord, k = chest.zCoord;
		int chestBlock = Block.chest.blockID;
		if (world.getBlockId(i - 1, j, k) == chestBlock) {
			return new InventoryLargeChest(origChest.getInvName(), (TileEntityChest) world.getBlockTileEntity(i - 1, j, k), origChest);
		}
		if (world.getBlockId(i, j, k - 1) == chestBlock) {
			return new InventoryLargeChest(origChest.getInvName(), (TileEntityChest) world.getBlockTileEntity(i, j, k - 1), origChest);
		}
		// If we're the lower chest, skip ourselves
		if (world.getBlockId(i + 1, j, k) == chestBlock) {
			return null;
		}
		if (world.getBlockId(i, j, k + 1) == chestBlock) {
			return null;
		}

		return chest;
	}

	public static IRecipe createShapedRecipe(ItemStack result, Object... args) {
		String var3 = "";
		int var4 = 0;
		int var5 = 0;
		int var6 = 0;

		if (args[var4] instanceof String[]) {
			String[] var7 = (String[]) ((String[]) args[var4++]);

			for (int var8 = 0; var8 < var7.length; ++var8) {
				String var9 = var7[var8];
				++var6;
				var5 = var9.length();
				var3 = var3 + var9;
			}
		} else {
			while (args[var4] instanceof String) {
				String var11 = (String) args[var4++];
				++var6;
				var5 = var11.length();
				var3 = var3 + var11;
			}
		}

		HashMap var12;

		for (var12 = new HashMap(); var4 < args.length; var4 += 2) {
			Character var13 = (Character) args[var4];
			ItemStack var14 = null;

			if (args[var4 + 1] instanceof Item) {
				var14 = new ItemStack((Item) args[var4 + 1]);
			} else if (args[var4 + 1] instanceof Block) {
				var14 = new ItemStack((Block) args[var4 + 1], 1, -1);
			} else if (args[var4 + 1] instanceof ItemStack) {
				var14 = (ItemStack) args[var4 + 1];
			}

			var12.put(var13, var14);
		}

		ItemStack[] var15 = new ItemStack[var5 * var6];

		for (int var16 = 0; var16 < var5 * var6; ++var16) {
			char var10 = var3.charAt(var16);

			if (var12.containsKey(Character.valueOf(var10))) {
				var15[var16] = ((ItemStack) var12.get(Character.valueOf(var10))).copy();
			} else {
				var15[var16] = null;
			}
		}

		return new ShapedRecipes(var5, var6, var15, result);
	}

	public static IRecipe createShapelessRecipe(ItemStack result, Object... args) {
		ArrayList var3 = new ArrayList();
		int var5 = args.length;

		for (int var6 = 0; var6 < var5; ++var6)
		{
			Object var7 = args[var6];

			if (var7 instanceof ItemStack)
			{
				var3.add(((ItemStack) var7).copy());
			}
			else if (var7 instanceof Item)
			{
				var3.add(new ItemStack((Item) var7));
			}
			else
			{
				if (!(var7 instanceof Block))
				{
					throw new RuntimeException("Invalid shapeless recipy!");
				}

				var3.add(new ItemStack((Block) var7));
			}
		}

		return new ShapelessRecipes(result, var3);
	}

	static Random rand = new Random();

	static void spawnItemStack(Coord c, ItemStack item) {
		if (item == null) {
			return;
		}
		double dx = rand.nextFloat() * 0.5 - 0.5;
		double dy = rand.nextFloat() * 0.5 - 0.5;
		double dz = rand.nextFloat() * 0.5 - 0.5;

		EntityItem entityitem = new EntityItem(c.w, c.x + 0.5, c.y + 0.5, c.z + 0.5, item);
		entityitem.motionY = 0.2 + rand.nextGaussian() * 0.02;
		entityitem.motionX = rand.nextGaussian() * 0.02;
		entityitem.motionZ = rand.nextGaussian() * 0.02;
		c.w.spawnEntityInWorld(entityitem);
	}
}
