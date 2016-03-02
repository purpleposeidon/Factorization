package factorization.flat.render;

import factorization.api.Coord;
import factorization.flat.api.IFlatModel;
import factorization.shared.FzModel;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

public class FlatModel implements IFlatModel {
    final FzModel model;

    public FlatModel(ResourceLocation url) {
        model = new FzModel(url);
    }

    @Override
    public IBakedModel getModel(Coord at, EnumFacing side) {
        return model.model;
    }
}
