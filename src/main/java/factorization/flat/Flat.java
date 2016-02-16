package factorization.flat;

import com.google.common.collect.Maps;
import factorization.api.Coord;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

import java.util.HashMap;

public final class Flat {
    public static void registerStatic(ResourceLocation name, FlatFace flat) {
        if (flat.staticId != FlatFeature.DYNAMIC_SENTINEL) {
            throw new IllegalArgumentException(name + " was already registered");
        }
        FlatFeature.registry.putObject(name, flat);
    }

    public static void registerDynamic(ResourceLocation name, Class<? extends FlatFace> flatClass) {
        if (dynamicReg.containsKey(name)) {
            throw new IllegalArgumentException("Already registered: " + name);
        }
        dynamicReg.put(name, flatClass);
    }

    static final HashMap<ResourceLocation, Class<? extends FlatFace>> dynamicReg = Maps.newHashMap();

    public static FlatFace get(Coord at, EnumFacing side) {
        return new AtSide(at, side).get();
    }

    public static void set(Coord at, EnumFacing side, FlatFace face) {
        new AtSide(at, side).set(face);
    }
}
