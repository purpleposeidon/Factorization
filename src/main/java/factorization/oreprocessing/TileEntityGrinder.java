package factorization.oreprocessing;

import factorization.shared.Core;
import factorization.util.ItemUtil;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;

public class TileEntityGrinder {
    // This is now just a recipe-holder class...

    public static ArrayList<GrinderRecipe> recipes = new ArrayList();

    public static void addRecipe(Object input, ItemStack output, float probability) {
        GrinderRecipe toAdd = new GrinderRecipe(input, output, probability);
        for (GrinderRecipe gr : recipes) {
            if (gr.getOreDictionaryInput().equals(input)) {
                return;
            }
        }
        recipes.add(toAdd);
    }


    public static class GrinderRecipe {
        private String oreName = null;
        private ItemStack itemstack = null;
        private ArrayList<ItemStack> inputArray = new ArrayList();
        public ItemStack output;
        public float probability;

        GrinderRecipe(Object input, ItemStack output, float probability) {
            this.output = output;
            this.probability = probability;
            if (input instanceof Block) {
                itemstack = new ItemStack((Block) input, 1, ItemUtil.WILDCARD_DAMAGE);
            } else if (input instanceof Item) {
                itemstack = new ItemStack((Item) input, 1, ItemUtil.WILDCARD_DAMAGE);
            } else if (input instanceof ItemStack) {
                itemstack = (ItemStack) input;
                if (itemstack.getItem() == null) {
                    Core.logSevere("Trying to define a recipe with an itemstack with a null item!?");
                    Thread.dumpStack();
                }
            } else {
                this.oreName = (String) input;
                return;
            }
            inputArray.add(itemstack);
        }
        
        public ArrayList<ItemStack> getInput() {
            if (oreName != null) {
                return OreDictionary.getOres(oreName);
            }
            ArrayList<ItemStack> ret = new ArrayList(1);
            return inputArray;
        }
        
        public Object getOreDictionaryInput() {
            if (oreName != null) {
                return oreName;
            }
            return itemstack;
        }
        
    }
}
