package factorization.api.recipe;

import java.util.ArrayList;
import java.util.HashMap;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class GenericRecipeManager {
    public ArrayList<GenericRecipe> recipes = new ArrayList();
    
    
    private Object[] inputs;
    
    public GenericRecipeManager input(Object ...inputs) {
        if (inputs != null) {
            throw new IllegalArgumentException("output() must be called to finish the recipe definition");
        }
        this.inputs = inputs;
        return this;
    }
    
    public GenericRecipe output(Object ...outputs) {
        if (this.inputs == null) {
            throw new IllegalArgumentException("input() must be called to begin the recipe definition");
        }
        
        try {
            return new GenericRecipe(convertInput(inputs), convertOutput(outputs));
        } finally {
            inputs = null;
        }
    }
    
    private IGenericRecipeInput[] convertInput(Object[] inputs) {
        ArrayList<IGenericRecipeInput> ret = new ArrayList(inputs.length);
        for (int i = 0; i < inputs.length; i++) {
            Object first = inputs[i];
            if (first instanceof IGenericRecipeInput) {
                ret.add((IGenericRecipeInput) first);
            } else if (first instanceof ItemStack) {
                ret.add(new InputItemStack((ItemStack) first));
            } else if (first instanceof String) {
                ret.add(new InputOreDict((String) first));
            } else if (first instanceof Item) {
                ret.add(new InputItemStack(new ItemStack((Item) first, 1, 1)));
            } else if (first instanceof Block) {
                ret.add(new InputItemStack(new ItemStack((Block) first, 1, 1)));
            } else {
                throw new IllegalArgumentException("Must be ItemStack/Item/Block/String<OreDictionaryName>/IGenericRecipeInput: " + first);
            }
        }
        return (IGenericRecipeInput[]) ret.toArray();
    }
    
    private GenericRecipeOutput[] convertOutput(Object[] outputs) {
        ArrayList<GenericRecipeOutput> ret = new ArrayList(outputs.length);
        for (int i = 0; i < inputs.length; i++) {
            Object first = inputs[i];
            if (first instanceof GenericRecipeOutput) {
                ret.add((GenericRecipeOutput) first);
                continue;
            }
            double bonus = 0;
            if (first instanceof Double || first instanceof Float) {
                bonus = (Double) first;
                i++;
                first = inputs[i];
            }
            ItemStack is = null;
            if (first instanceof ItemStack) {
                is = (ItemStack) first;
            } else if (first instanceof Item) {
                is = new ItemStack((Item) first, 1, 1);
            } else if (first instanceof Block) {
                is = new ItemStack((Block) first, 1, 1);
            } else {
                throw new IllegalArgumentException("Must be [Float] ItemStack/Item/Block: " + first);
            }
            ret.add(new GenericRecipeOutput(is, bonus));
        }
        return (GenericRecipeOutput[]) ret.toArray();
    }
}
