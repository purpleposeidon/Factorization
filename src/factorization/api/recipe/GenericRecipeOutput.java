package factorization.api.recipe;

import net.minecraft.item.ItemStack;

public class GenericRecipeOutput {
    public ItemStack output;
    public double bonus;
    
    public GenericRecipeOutput(ItemStack output, double bonus) {
        this.output = output;
        this.bonus = bonus;
    }
    
    public ItemStack getOutput(double random) {
        ItemStack ret = output.copy();
        if (random > bonus) {
            ret.stackSize++;
        }
        return ret;
    }
}
