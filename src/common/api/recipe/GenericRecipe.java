package factorization.api.recipe;

import factorization.common.FactorizationUtil;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class GenericRecipe {
    public IGenericRecipeInput inputs[];
    public GenericRecipeOutput outputs[];
    
    public GenericRecipe(IGenericRecipeInput[] inputs, GenericRecipeOutput[] outputs) {
        this.inputs = inputs;
        this.outputs = outputs;
    }
    
    public boolean canCraft(IInventory input, IInventory output) {
        return inputMatches(input) && roomForOutput(output);
    }
    
    public void apply(IInventory input, IInventory output) {
        ItemStack outputs[] = getOutputs(Math.random());
    }
    
    private boolean inputMatches(IInventory inv) {
        for (int inp = 0; inp < inputs.length; inp++) {
            IGenericRecipeInput input = inputs[inp];
            boolean match = false;
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                ItemStack is = inv.getStackInSlot(i);
                if (is == null) {
                    continue;
                }
                if (input.matches(is)) {
                    match = true;
                    break;
                }
            }
            if (!match) {
                return false;
            }
        }
        return true;
    }
    
    private boolean roomForOutput(IInventory inv) {
        ItemStack[] outs = getOutputs(1);
        int stackLimit = inv.getInventoryStackLimit();
        for (int o = 0; o < outs.length; o++) {
            ItemStack out = outs[o];
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                ItemStack is = inv.getStackInSlot(i);
                if (is == null) {
                    out.stackSize -= stackLimit;
                    continue;
                }
                if (!out.isItemEqual(is)) {
                    continue;
                }
                out.stackSize -= FactorizationUtil.getFreeSpace(is, stackLimit);
                if (out.stackSize <= 0) {
                    break;
                }
            }
            if (out.stackSize > 0) {
                return false;
            }
        }
        return true;
    }
    
    private ItemStack[] getOutputs(double random) {
        ItemStack[] ret = new ItemStack[outputs.length];
        for (int i = 0; i < outputs.length; i++) {
            ret[i] = outputs[i].getOutput(random);
        }
        return ret;
    }
}
