package factorization.shared;

import factorization.api.annotation.Nonnull;
import net.minecraft.item.ItemStack;

import java.util.List;

public interface ISensitiveMesh {
    String getMeshName(@Nonnull ItemStack is);

    @Nonnull
    List<ItemStack> getMeshSamples();
}
