package factorization.flat.api;

import factorization.api.Coord;
import factorization.api.ICoordFunction;
import factorization.coremodhooks.IExtraChunkData;
import factorization.flat.FlatFaceAir;
import factorization.flat.FlatMod;
import factorization.flat.FlatRayTracer;
import factorization.util.PlayerUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.chunk.Chunk;

import java.util.ArrayList;

public final class Flat {
    public static FlatFace registerStatic(ResourceLocation name, FlatFace flat) {
        if (flat.staticId != FlatMod.DYNAMIC_SENTINEL) {
            throw new IllegalArgumentException(name + " was already registered");
        }
        //noinspection deprecation -- Yes cpw, I'll just hack this into GameRegistry
        FlatMod.staticReg.register(-1 /* The registry should pick a proper ID */, name, flat);
        if (!getName(flat).equals(name)) throw new AssertionError("registration failed");
        return flat;
    }

    public static void registerDynamic(ResourceLocation name, Class<? extends FlatFace> flatClass) {
        if (FlatMod.dynamicReg.containsKey(name)) {
            throw new IllegalArgumentException("Already registered: " + name);
        }
        try {
            FlatFace sample = flatClass.newInstance();
            FlatMod.dynamicSamples.put(name, sample);
        } catch (Throwable t) {
            throw new IllegalArgumentException(t);
        }
        FlatMod.dynamicReg.put(name, flatClass);
    }

    public static ResourceLocation getName(FlatFace flat) {
        if (flat.isStatic()) {
            return FlatMod.staticReg.getNameForObject(flat);
        } else {
            return FlatMod.dynamicReg.inverse().get(flat.getClass());
        }
    }

    public static FlatFace getDynamic(ResourceLocation name) {
        try {
            return FlatMod.dynamicReg.get(name).newInstance();
        } catch (Throwable e) {
            e.printStackTrace();
            return FlatFaceAir.INSTANCE;
        }
    }

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
            as.get().onNeighborBlockChanged(as.at, as.side);
        }
    }

    public static void onFaceChanged(Coord at, EnumFacing side) {
        // Notifies all 'adjacent' faces. There are 3 sets of 4 faces: the 4 in-plane, and the 8 on both blocks
        for (AtSide as : new AtSide(at, side).iterateConnected()) {
            as.get().onNeighborFaceChanged(as.at, as.side);
        }
    }

    public static Iterable<FlatFace> getAll() {
        ArrayList<FlatFace> ret = new ArrayList<FlatFace>();
        for (FlatFace ff : FlatMod.staticReg) {
            ret.add(ff);
        }
        ret.addAll(FlatMod.dynamicSamples.values());
        return ret;
    }

    private static int SPECIES = 0;
    public static int nextSpeciesId() {
        return ++SPECIES;
    }

    public static void setAir(Coord at, EnumFacing side) {
        set(at, side, FlatFaceAir.INSTANCE);
    }

    public static void iterateRegion(final Coord min, final Coord max, final IFlatVisitor visitor) {
        Coord.iterateCube(min, max, new ICoordFunction() {
            @Override
            public void handle(Coord here) {
                Chunk chunk = here.getChunk();
                IExtraChunkData ecd = (IExtraChunkData) chunk;
                ecd.getFlatLayer().iterateBounded(min, max, visitor);
            }
        });
    }
}
