package factorization.fzds;

import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.fzds.interfaces.IDimensionSlice;
import factorization.fzds.interfaces.transform.Pure;
import factorization.fzds.interfaces.transform.TransformData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;

public class NullOrder implements ITransformOrder {
    public static void give(IDimensionSlice idc) {
        idc.giveOrder(new NullOrder());
    }

    @Nullable
    @Override
    public TransformData<Pure> tickTransform(IDimensionSlice idc) {
        return null;
    }

    @Override
    public boolean isNull() {
        return true;
    }

    @Nonnull
    @Override
    public TransformData<Pure> removed(boolean completeElseCancelled) {
        return TransformData.newPure();
    }

    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        return this;
    }
}
