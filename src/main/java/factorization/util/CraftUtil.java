package factorization.util;

import factorization.api.Coord;
import factorization.shared.Core;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public final class CraftUtil {
    private static final ItemStack[] slots3x3 = new ItemStack[9];
    public static boolean craft_succeeded = false;
    public static ArrayList<ItemStack> emptyArrayList = new ArrayList<ItemStack>(0);
    static ArrayList<IRecipe> recipeCache = new ArrayList<IRecipe>();
    private static int cache_fear = 10;

    public static InventoryCrafting makeCraftingGrid() {
        return new InventoryCrafting(new Container() {
            @Override
            public boolean canInteractWith(EntityPlayer entityplayer) {
                return false;
            }

            @Override
            public void onCraftMatrixChanged(IInventory iinventory) {
            }
        }, 3, 3);
    }

    public static void addInventoryToArray(IInventory inv, ArrayList<ItemStack> ret) {
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack is = ItemUtil.normalize(inv.getStackInSlot(i));
            if (is != null) {
                ret.add(is);
            }
        }
    }

    static InventoryCrafting getCrafter(ItemStack...slots) {
        InventoryCrafting craft = makeCraftingGrid();
        for (int i = 0; i < 9; i++) {
            craft.setInventorySlotContents(i, slots[i]);
        }
        return craft;
    }

    static boolean wantSize(int size, TileEntity where, ItemStack...slots) {
        if (slots.length != size) {
            System.out.println("Tried to craft with items.length != " + size);
            if (where != null) {
                System.out.println("At " + new Coord(where));
            }
            Thread.dumpStack();
            return true;
        }
        return false;
    }

    public static List<ItemStack> craft1x1(TileEntity where, boolean fake, ItemStack what) {
        for (int i = 0; i < slots3x3.length; i++) {
            slots3x3[i] = null;
        }
        slots3x3[4] = what;
        return craft3x3(where, fake, false, slots3x3);
    }

    public static List<ItemStack> craft2x2(TileEntity where, boolean fake, ItemStack...slots) {
        if (wantSize(4, where, slots)) {
            return Arrays.asList(slots);
        }
        for (int i = 0; i < slots3x3.length; i++) {
            slots3x3[i] = null;
        }
        slots3x3[0] = slots[0];
        slots3x3[1] = slots[1];
        slots3x3[3] = slots[2];
        slots3x3[4] = slots[3];
        return craft3x3(where, fake, false, slots3x3);
    }

    public static List<ItemStack> craft3x3(TileEntity where, boolean fake, boolean leaveSlots, ItemStack... slots) {
        craft_succeeded = false;
        // Return the crafting result, and any leftover ingredients (buckets)
        // If the crafting recipe fails, return our contents.
        if (wantSize(9, where, slots)) {
            return leaveSlots ? emptyArrayList : Arrays.asList(slots);
        }

        InventoryCrafting craft = getCrafter(slots);

        IRecipe recipe = findMatchingRecipe(craft, where == null ? null : where.getWorld());
        ItemStack result = null;
        if (recipe != null) {
            result = recipe.getCraftingResult(craft);
        }

        if (result == null) {
            // crafting failed, dump everything
            return leaveSlots ? emptyArrayList : Arrays.asList(slots);
        }
        final ArrayList<ItemStack> ret = new ArrayList<ItemStack>();
        if (fake) {
            ret.add(result);
            craft_succeeded = true;
            return ret;
        }
        Coord pos = null;
        if (where != null) {
            pos = new Coord(where);
        }
        EntityPlayer fakePlayer = PlayerUtil.makePlayer(pos, "Crafting");
        if (pos != null) {
            pos.setAsEntityLocation(fakePlayer);
        }

        IInventory craftResult = new InventoryCraftResult();
        craftResult.setInventorySlotContents(0, result);
        SlotCrafting slot = new SlotCrafting(fakePlayer, craft, craftResult, 0, 0, 0);
        slot.onPickupFromSlot(fakePlayer, result);
        ret.add(result);
        if (!leaveSlots) {
            addInventoryToArray(craft, ret);
        }
        addInventoryToArray(fakePlayer.inventory, ret);
        PlayerUtil.recycleFakePlayer(fakePlayer);

        craft_succeeded = true;
        return ret;
    }

    public static IRecipe findMatchingRecipe(InventoryCrafting inv, World world) {
        if (Core.serverStarted) {
            List<IRecipe> craftingManagerRecipes = CraftingManager.getInstance().getRecipeList();
            cache_fear--;
            if (cache_fear > 0) {
                return lookupRecipeUncached(inv, world);
            }
            if (craftingManagerRecipes.size() != recipeCache.size()) {
                if (cache_fear < 0) {
                    cache_fear = 10;
                    return lookupRecipeUncached(inv, world);
                }
                recipeCache.clear();
                recipeCache.ensureCapacity(craftingManagerRecipes.size());
                recipeCache.addAll(craftingManagerRecipes);
            }
            for (int i = 0; i < recipeCache.size(); i++) {
                IRecipe recipe = recipeCache.get(i);
                if (recipe == null) continue;
                if (recipe.matches(inv, world)) {
                    if (i > 50) {
                        int j = i/3;
                        IRecipe swapeh = recipeCache.get(j);
                        recipeCache.set(j, recipe);
                        recipeCache.set(i, swapeh);
                    }
                    return recipe;
                }
            }
        } else {
            return lookupRecipeUncached(inv, world);
        }

        return null;
    }

    public static IRecipe lookupRecipeUncached(InventoryCrafting inv, World world) {
        while (true) {
            List<IRecipe> craftingManagerRecipes = CraftingManager.getInstance().getRecipeList();
            Iterator<IRecipe> iterator = craftingManagerRecipes.iterator();
            IRecipe recipe = null;
            try {
                while (iterator.hasNext()) {
                    recipe = iterator.next();
                    if (recipe.matches(inv, world)) {
                        return recipe;
                    }
                }
            } catch (Throwable t) {
                if (recipe == null) return null; // Won't happen.
                Core.logSevere("Recipe crashed: " + recipe.getClass() + " for " + recipe.getRecipeOutput());
                Core.logSevere("It has been removed.");
                iterator.remove();
                t.printStackTrace();
                continue;
            }
            return null;
        }
    }
}
