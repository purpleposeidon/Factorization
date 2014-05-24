package factorization.oreprocessing;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.oredict.OreDictionary;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Charge;
import factorization.api.IChargeConductor;
import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.shared.BlockClass;
import factorization.shared.Core;
import factorization.shared.FzUtil;
import factorization.shared.NetworkFactorization.MessageType;
import factorization.shared.TileEntityFactorization;

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
                itemstack = new ItemStack((Block) input, 1, FzUtil.WILDCARD_DAMAGE);
            } else if (input instanceof Item) {
                itemstack = new ItemStack((Item) input, 1, FzUtil.WILDCARD_DAMAGE);
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
