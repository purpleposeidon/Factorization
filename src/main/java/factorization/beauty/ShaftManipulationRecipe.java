package factorization.beauty;

import factorization.shared.Core;
import factorization.util.DataUtil;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.RecipeSorter;

import java.util.Random;

public class ShaftManipulationRecipe implements IRecipe {
    public static void addManipulationRecipes() {
        GameRegistry.addRecipe(new ShaftManipulationRecipe());
        RecipeSorter.register("factorization:shaft", ShaftManipulationRecipe.class, RecipeSorter.Category.SHAPELESS, "");
    }

    @Override
    public boolean matches(InventoryCrafting inv, World worldIn) {
        return getCraftingResult(inv) != null;
    }

    final Item shear = Items.shears;
    final Item shaft = DataUtil.getItem(Core.registry.shaft);

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        ItemStack found_shaft = null;
        ItemStack found_manip = null;
        boolean shear_it = false;
        int found = 0;
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack is = inv.getStackInSlot(i);
            if (is == null) continue;
            found++;
            Item it = is.getItem();
            if (it == shaft) {
                found_shaft = is;
            } else if (it == shear) {
                found_manip = is;
                shear_it = true;
            } else if (it instanceof ItemBlock) {
                Block b = DataUtil.getBlock(it);
                if (b.hasTileEntity(b.getDefaultState())) return null;
                if (!b.isOpaqueCube()) return null;
                if (b.getRenderType() != Blocks.stone.getRenderType()) return null;
                if (!b.getMaterial().blocksMovement()) return null;
                found_manip = is;
            } else {
                return null;
            }
        }
        if (found != 2) return null;
        if (found_shaft == null || found_manip == null) return null;
        ShaftItemCache cache = new ShaftItemCache(found_shaft);
        if (shear_it) {
            if (cache.sheared) return null;
            return new ShaftItemCache(cache.log, true).pack();
        }
        return new ShaftItemCache(found_manip, cache.sheared).pack();
    }

    @Override
    public int getRecipeSize() {
        return 2;
    }

    ItemStack output = new ShaftItemCache(new ItemStack(Blocks.log), false).pack();
    @Override
    public ItemStack getRecipeOutput() {
        return output;
    }

    private Random rng = new Random();
    @Override
    public ItemStack[] getRemainingItems(InventoryCrafting inv) {
        ItemStack[] ret = new ItemStack[inv.getSizeInventory()];
        for (int i = 0; i < ret.length; i++) {
            ItemStack is = inv.getStackInSlot(i);
            if (is == null) continue;
            if (is.getItem() instanceof ItemShears) {
                ItemStack s = is.copy();
                if (!s.attemptDamageItem(1, rng)) {
                    ret[i] = s;
                }
            }
        }
        return ret;
    }
}
