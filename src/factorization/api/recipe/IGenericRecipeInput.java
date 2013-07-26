package factorization.api.recipe;

import net.minecraft.item.ItemStack;

public interface IGenericRecipeInput {
    boolean matches(ItemStack is);
}
