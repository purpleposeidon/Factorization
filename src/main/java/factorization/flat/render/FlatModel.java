package factorization.flat.render;

import factorization.api.Coord;
import factorization.flat.api.IFlatModel;
import factorization.shared.FzModel;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

public class FlatModel implements IFlatModel {
    final FzModel[] model;

    public FlatModel(ResourceLocation... url) {
        model = new FzModel[url.length];
        for (int i = 0; i < url.length; i++) {
            model[i] = new FzModel(url[i]);
        }
    }

    @Override
    public IBakedModel[] getModel(Coord at, EnumFacing side) {
        IBakedModel[] ret = new IBakedModel[model.length];
        for (int i = 0; i < model.length; i++) {
            ret[i] = model[i].model;
        }
        return ret;
    }
}
