package factorization.fzds;

import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.RegisteredDataUnion;
import factorization.fzds.interfaces.IDimensionSlice;
import factorization.fzds.interfaces.transform.Pure;
import factorization.fzds.interfaces.transform.TransformData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ITransformOrder extends IDataSerializable {
    class TransformRegistry {
        public static final RegisteredDataUnion registry = new RegisteredDataUnion();
        static {
            register("basic", BasicTransformOrder.class);
            register("null", NullOrder.class);
        }

        public static void register(String name, Class<? extends ITransformOrder> orderClass) {
            registry.add(name, orderClass);
        }
    }

    /**
     * @param idc The IDeltaChunk that will be transformed
     * @return The TransformData that will be applied to the IDC this tick. If null, then the order will be cancelled.
     * Note that this is NOT the actual value of the transformation; the return value is a velocity.
     * So you can think of an ITransformOrder as being a kind of acceleration, and this method returns the current
     * velocity.
     */
    @Nullable
    TransformData<Pure> tickTransform(IDimensionSlice idc);

    boolean isNull();

    @Nonnull
    TransformData<Pure> removed(boolean completeElseCancelled);
}
