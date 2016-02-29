package factorization.flat;

import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

public interface IModelMaker {
    @Nullable
    IFlatModel getModel(ResourceLocation url);
}
