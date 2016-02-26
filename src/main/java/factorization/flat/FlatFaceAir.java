package factorization.flat;

import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nullable;
import java.io.IOException;

public final class FlatFaceAir extends FlatFace {
    public static final FlatFaceAir INSTANCE = new FlatFaceAir();

    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        return this;
    }

    @Override
    @Nullable
    public IBakedModel getModel(Coord at, EnumFacing side) {
        return null;
    }

    @Override
    public boolean isReplaceable(Coord at, EnumFacing side) {
        return true;
    }
}
