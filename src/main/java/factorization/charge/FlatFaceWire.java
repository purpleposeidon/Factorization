package factorization.charge;

import factorization.api.Coord;
import factorization.flat.FlatFace;
import factorization.flat.IFlatModel;
import factorization.flat.IModelMaker;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

public class FlatFaceWire extends FlatFace {
    IFlatModel model;

    @Nullable
    @Override
    public IFlatModel getModel(Coord at, EnumFacing side) {
        return model;
    }

    @Override
    public void loadModels(IModelMaker maker) {
        model = maker.getModel(new ResourceLocation("factorization:model/flat/wire"));
    }
}
