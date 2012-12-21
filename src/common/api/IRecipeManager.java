package factorization.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.common.ISidedInventory;
import net.minecraftforge.oredict.OreDictionary;

public interface IRecipeManager {
    public static class GenericRecipe {
        public final Object[] inputs;
        public final Object[] outputs;
        public GenericRecipe(Object[] inputs, Object[] outputs) {
            verifyItemList(inputs, true);
            verifyItemList(outputs, false);
            this.inputs = inputs;
            this.outputs = outputs;
        }
        
        void verifyItemList(Object itemList[], boolean is_input) {
            /**
             * Recipe itemList grammar: ((frequency=float)? item=(ItemStack|Item|Block|String))*;
             */
            for (int i = 0; i < itemList.length; i++) {
                Object first = itemList[i];
                if (first instanceof Float) {
                    i++;
                    first = itemList[i];
                    if (is_input) {
                        throw new IllegalArgumentException("Can't have bonuses on input materials");
                    }
                }
                if (first instanceof ItemStack) {
                    continue;
                }
                if (first instanceof String) {
                    continue;
                }
                if (first instanceof Item) {
                    itemList[i] = new ItemStack((Item) first, 1, 1);
                    continue;
                }
                if (first instanceof Block) {
                    itemList[i] = new ItemStack((Block) first, 1, 1);
                    continue;
                }
                throw new IllegalArgumentException("Must be Float/ItemStack/Item/Block/String<OreDictionaryName>: " + first);
            }
        }
        
        ArrayList<ItemStack> getItemsInInventory(ISidedInventory inv, ForgeDirection side) {
            ArrayList<ItemStack> ret = new ArrayList();
            int start = inv.getStartInventorySide(side);
            int end = start + inv.getSizeInventorySide(side);
            for (int i = start; i < end; i++) {
                ItemStack is = inv.getStackInSlot(i);
                if (is == null || is.stackSize == 0) {
                    continue;
                }
                ret.add(is.copy());
            }
            return ret;
        }
        
        ItemStack getItemBonus(ItemStack is, float chance) {
            //The chance is in [0, 1] for the stack size to be increased by 1
            ItemStack ret = is.copy();
            ret.stackSize += chance > Math.random() ? 1 : 0;
            return ret;
        }
        
        boolean removeItemFromList(ItemStack is, ArrayList<ItemStack> items) {
            for (int i = 0; i < items.size(); i++) {
                ItemStack here = items.get(i);
                if (is.isItemEqual(here)) {
                    int toRemove = Math.min(is.stackSize, here.stackSize);
                    is.stackSize -= toRemove;
                    here.stackSize -= toRemove;
                    if (is.stackSize <= 0) {
                        return true;
                    }
                }
            }
            return is.stackSize == 0;
        }
        
        void addItemToList(ItemStack is, ArrayList<ItemStack> items) {
            for (int i = 0; i < items.size(); i++) {
                ItemStack here = items.get(i);
                if (is.isItemEqual(here)) {
                    int free = here.getMaxStackSize() - here.stackSize;
                    int delta = Math.min(free, is.stackSize);
                    is.stackSize -= delta;
                    here.stackSize += delta;
                }
            }
            if (is.stackSize > 0) {
                items.add(is);
            }
        }
        
        public ArrayList<ItemStack> getVariants(Object obj) {
            if (obj instanceof ItemStack) {
                ArrayList<ItemStack> ret = new ArrayList(1);
                ret.add((ItemStack) obj);
                return ret;
            } else {
                return OreDictionary.getOres((String) obj);
            }
        }
        
        public boolean matches(ISidedInventory inv, ForgeDirection input_side, ForgeDirection output_side) {
            ArrayList<ItemStack> machine_in = getItemsInInventory(inv, input_side);
            for (int i = 0; i < inputs.length; i++) {
                ArrayList<ItemStack> variants = getVariants(inputs[i]);
                boolean foundMatchingVariant = false;
                for (int j = 0; j < variants.size(); j++) {
                    ItemStack is = variants.get(j);
                    if (removeItemFromList(is, machine_in)) {
                        foundMatchingVariant = true;
                        break;
                    }
                }
                if (!foundMatchingVariant) {
                    return false;
                }
            }
            
            ArrayList<ItemStack> machine_out = getItemsInInventory(inv, output_side);
            for (int i = 0; i < outputs.length; i++) {
                float bonusChance = 0;
                if (outputs[i] instanceof Float) {
                    bonusChance = 1; //(Float) inputs[i];
                    i++;
                }
                ItemStack is = getItemBonus((ItemStack) outputs[i], bonusChance);
                addItemToList(is, machine_out);
            }
            return machine_out.size() <= inv.getSizeInventorySide(output_side);
        }
        
        public void apply(ISidedInventory inv, ForgeDirection input_side, ForgeDirection output_side) {
            
        }
    }
}
