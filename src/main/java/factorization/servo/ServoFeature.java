package factorization.servo;

import factorization.flat.api.Flat;
import factorization.servo.instructions.GenericPlaceholder;
import factorization.servo.rail.Decorator;
import factorization.servo.rail.FlatServoRail;
import net.minecraft.util.ResourceLocation;

public class ServoFeature {
    public static FlatServoRail static_rail;

    static FlatServoRail staticComponent(Decorator sc) {
        FlatServoRail fsr = new FlatServoRail();
        fsr.component = sc;
        return fsr;
    }

    public static void setup() {
        static_rail = new FlatServoRail();
        Flat.registerStatic(new ResourceLocation("factorization:servo/flat/static"), static_rail);
        Flat.registerDynamic(new ResourceLocation("factorization:servo/flat/dynamic"), FlatServoRail.class);
        staticComponent(GenericPlaceholder.INSTANCE);
    }
}
