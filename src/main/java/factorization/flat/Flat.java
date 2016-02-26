package factorization.flat;

import com.google.common.collect.Maps;
import factorization.api.Coord;
import factorization.util.PlayerUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.profiler.PlayerUsageSnooper;
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
        onFaceChanged(at, side);
    }

    public static boolean tryUsePlacer(EntityPlayer player, ItemStack is, FlatFace face, Coord at, EnumFacing side) {
        AtSide as = new AtSide(at, side);
        FlatFace orig = as.get();
        if (!orig.isReplaceable(at, side)) return false;
        if (!face.isValidAt(at, side)) return false;
        as.set(face);
        face.onPlaced(at, side, player, is);
        onFaceChanged(at, side);
        PlayerUtil.cheatDecr(player, is);
        return true;
    }

    public static void onBlockChanged(Coord at) {
        for (EnumFacing side : EnumFacing.VALUES) {
            AtSide as = new AtSide(at, side);
            as.get().onBlockNeighborChanged(as.at, as.side);
        }
    }

    public static void onFaceChanged(Coord at, EnumFacing side) {
        // Notifies all 'adjacent' faces. There are 3 sets of 4 faces: the 4 in-plane, and the 8 on both blocks
        for (AtSide as : new AtSide(at, side).iterateConnected()) {
            as.get().onBlockNeighborChanged(as.at, as.side);
        }
    }
}
