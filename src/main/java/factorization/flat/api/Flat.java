package factorization.flat.api;

import factorization.api.Coord;
import factorization.api.ICoordFunction;
import factorization.coremodhooks.IExtraChunkData;
import factorization.flat.FlatChunkLayer;
import factorization.flat.FlatFaceAir;
import factorization.flat.FlatMod;
import factorization.flat.FlatNet;
import factorization.shared.Core;
import factorization.util.NORELEASE;
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


    public static void registerDynamic(ResourceLocation name, FlatFace sample) {
        registerDynamic(name, sample.getClass());
        FlatMod.dynamicSamples.put(name, sample);
    }

    public static void registerDynamic(ResourceLocation name, Class<? extends FlatFace> flatClass) {
        if (FlatMod.dynamicReg.containsKey(name)) {
            throw new IllegalArgumentException("Already registered: " + name);
        }
        try {
            // This happens even through registerDynamic to verify the class is instantiable.
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
        setWithNotification(at, side, face, FlatChunkLayer.FLAGS_ALL);
    }

    public static void setWithNotification(Coord at, EnumFacing side, FlatFace face, byte flags) {
        AtSide atSide = new AtSide(at, side);
        atSide.set(face, flags);
    }

    public static boolean tryUsePlacer(EntityPlayer player, ItemStack is, FlatFace face, Coord at, EnumFacing side) {
        AtSide as = new AtSide(at, side);
        FlatFace orig = as.get();
        if (!orig.isReplaceable(at, side)) return false;
        if (!face.isValidAt(at, side)) return false;
        as.set(face, FlatChunkLayer.FLAGS_ALL);
        face.onPlaced(at, side, player, is);
        onFaceChanged(at, side);
        PlayerUtil.cheatDecr(player, is);
        return true;
    }

    public static void onBlockChanged(Coord standardBlock) {
        for (EnumFacing side : EnumFacing.VALUES) {
            AtSide as = new AtSide(standardBlock, side);
            FlatChunkLayer layer = as.getLayer();
            FlatFace flatFace = layer.get(as.at, as.side);
            flatFace.onNeighborBlockChanged(as.at, as.side);
            layer.renderInfo.markDirty(as.at);
        }
    }

    public static void onFaceChanged(Coord standardBlock, EnumFacing side) {
        // Notifies all 'adjacent' faces. There are 3 sets of 4 faces: the 4 in-plane, and the 8 on both blocks
        for (AtSide as : new AtSide(standardBlock, side).iterateConnected()) {
            as.get().onNeighborFaceChanged(as.at, as.side);
            as.getLayer().renderInfo.markDirty(as.at);
            NORELEASE.fixme("This doesn't fix updates across curves+chunkboundaries; see screenshot 18.43.13");
        }
    }

    public static Iterable<FlatFace> getAllSamples() {
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
                getLayer(chunk).iterateBounded(min, max, visitor);
            }
        });
    }

    public static void playSound(Coord at, EnumFacing side, FlatFace face) {
        FlatNet.fx(at, side, face, FlatNet.FX_PLACE);
        NORELEASE.println("sound", face);
    }

    public static void emitParticle(Coord at, EnumFacing side, FlatFace face) {
        FlatNet.fx(at, side, face, FlatNet.FX_BREAK);
        NORELEASE.println("particle", face);
    }

    static FlatChunkLayer getLayer(Chunk chunk) {
        IExtraChunkData ecd = (IExtraChunkData) chunk;
        return ecd.getFlatLayer();
    }

    public static void iterateDynamics(Chunk at, IFlatVisitor visitor) {
        getLayer(at).iterateDynamic(visitor);
    }
}
