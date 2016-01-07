package factorization.shared;

import net.minecraft.item.ItemStack;

import java.util.List;

public interface ISensitiveMesh {
    String getMeshName(@Nonnull ItemStack is);

    @Nonnull
    List<ItemStack> getMeshSamples();
}
