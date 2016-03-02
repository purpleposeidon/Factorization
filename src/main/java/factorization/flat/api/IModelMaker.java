package factorization.flat.api;

import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

public interface IModelMaker {
    @Nullable
    IFlatModel getModel(ResourceLocation url);
}
