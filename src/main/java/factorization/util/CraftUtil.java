package factorization.util;

import factorization.api.Coord;
import factorization.shared.Core;
import factorization.util.ItemUtil;
import factorization.util.PlayerUtil;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.*;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import java.util.*;

public final class CraftUtil {
    private static final ItemStack[] slots3x3 = new ItemStack[9];
    public static boolean craft_succeeded = false;
    public static ArrayList<ItemStack> emptyArrayList = new ArrayList(0);
    static ArrayList<IRecipe> recipeCache = new ArrayList();
    private static int cache_fear = 10;
    private static IRecipe stupid_hacky_vanilla_item_repair_recipe = new IRecipe() {
        ItemStack firstItem, secondItem, result;

        void update(IInventory par1InventoryCrafting) {
            //This is copied from CraftingManager.findMatchingRecipe; with a few tweaks
            firstItem = secondItem = result = null;
            int i = 0;
            int j;

            for (j = 0; j < par1InventoryCrafting.getSizeInventory(); ++j)
            {
                ItemStack itemstack2 = par1InventoryCrafting.getStackInSlot(j);

                if (itemstack2 != null)
                {
                    if (i == 0)
                    {
                        firstItem = itemstack2;
                    }

                    if (i == 1)
                    {
                        secondItem = itemstack2;
                    }

                    ++i;
                }
            }

            if (i == 2 && firstItem.getItem() == secondItem.getItem() && firstItem.stackSize == 1 && secondItem.stackSize == 1 && firstItem.getItem().isRepairable())
            {
                Item item = firstItem.getItem();
                int j1 = item.getMaxDamage() - firstItem.getItemDamageForDisplay();
                int k = item.getMaxDamage() - secondItem.getItemDamageForDisplay();
                int l = j1 + k + item.getMaxDamage() * 5 / 100;
                int i1 = item.getMaxDamage() - l;

                if (i1 < 0)
                {
                    i1 = 0;
                }

                result = new ItemStack(firstItem.getItem(), 1, i1);
            }
        }

        @Override
        public boolean matches(InventoryCrafting inventorycrafting, World world) {
            update(inventorycrafting);
            return result != null;
        }

        @Override
        public ItemStack getCraftingResult(InventoryCrafting inventorycrafting) {
            update(inventorycrafting);
            return result;
        }

        @Override
        public int getRecipeSize() {
            return 2;
        }

        @Override
        public ItemStack getRecipeOutput() {
            return null;
        }

    };

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

        IRecipe recipe = findMatchingRecipe(craft, where == null ? null : where.getWorldObj());
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
                recipeCache.add(stupid_hacky_vanilla_item_repair_recipe);
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

    //Recipe creation
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
}
