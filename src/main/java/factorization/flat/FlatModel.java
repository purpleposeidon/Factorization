package factorization.flat;

import factorization.api.Coord;
import factorization.api.Quaternion;
import factorization.shared.FzModel;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.TRSRTransformation;

public class FlatModel implements IFlatModel {
    final FzModel xModel, yModel, zModel;

    private static final TRSRTransformation yTrsrt, zTrsrt;
    static {
        Quaternion y = Quaternion.getRotationQuaternionRadians(Math.PI / 2, 1, 0, 0);
        Quaternion z = Quaternion.getRotationQuaternionRadians(Math.PI / 2, 0, 1, 0);
        yTrsrt = new TRSRTransformation(null, y.toJavax(), null, null);
        zTrsrt = new TRSRTransformation(null, z.toJavax(), null, null);
    }

    public FlatModel(ResourceLocation url) {
        xModel = new FzModel(url);
        yModel = new FzModel(url);
        zModel = new FzModel(url);
        yModel.trsrt = yTrsrt;
        zModel.trsrt = zTrsrt;
    }

    @Override
    public IBakedModel getModel(Coord at, EnumFacing side) {
        switch (side.getAxis()) {
            default:
            case X: return xModel.model;
            case Y: return yModel.model;
            case Z: return zModel.model;
        }
    }
}
