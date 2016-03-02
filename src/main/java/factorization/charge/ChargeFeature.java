package factorization.charge;

import factorization.flat.Flat;
import net.minecraft.util.ResourceLocation;

public class ChargeFeature {
    public static final ChargeFeature INSTANCE = new ChargeFeature();
    private ChargeFeature() {}

    public FlatFaceWire wire0;

    public void init() {
        wire0 = new FlatFaceWire(0);
        Flat.registerStatic(new ResourceLocation("factorization:charge/wire0"), wire0);
    }
}
