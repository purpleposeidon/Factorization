package factorization.api;

import net.minecraft.world.World;

import java.util.ArrayList;

public class HeatConverters {
    private static final ArrayList<IHeatConverter> converters = new ArrayList<IHeatConverter>();

    public static void addConverter(IHeatConverter converter) {
        converters.add(converter);
    }

    public static IFurnaceHeatable convert(World w, BlockPos pos) {
        for (IHeatConverter conv : converters) {
            IFurnaceHeatable toastable = conv.convert(w, x, y, z);
            if (toastable != null) return toastable;
        }
        return null;
    }

    public interface IHeatConverter {
        public IFurnaceHeatable convert(World w, BlockPos pos);
    }

    static {
        addConverter(new DefaultHeatConverter());
    }

}
