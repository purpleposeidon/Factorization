package factorization.flat;

import factorization.api.Coord;
import factorization.api.Quaternion;
import factorization.shared.FzModel;
import factorization.util.NORELEASE;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.TRSRTransformation;

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
