package factorization.flat;

import factorization.api.Coord;
import factorization.util.SpaceUtil;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.registry.FMLControlledNamespacedRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class FlatChunkLayer {
    public static short index(int slabX, int slabY, int slabZ, EnumFacing face) {
        if (slabX < 0 || slabY < 0 || slabZ < 0) return -1;
        if (slabX > 0xF || slabY > 0xF || slabZ > 0xF) return -1;
        if (SpaceUtil.sign(face) != 1) return -1;
        int ret = (slabX << 2) | (slabY << 6) | (slabZ << 10) | FlatFeature.side2byte(face);
        return (short) ret;
    }

    class IterateContext {
        final Chunk chunk;
        final int slabYHeight;
        final IFlatVisitor visitor;
        final Coord at;

        IterateContext(Chunk chunk, int slabY, IFlatVisitor visitor) {
            this.chunk = chunk;
            this.slabYHeight = slabY << 4;
            this.visitor = visitor;
            this.at = new Coord(chunk.getWorld(), 0, 0, 0);
        }

        void visit(int index, @Nonnull FlatFace face) {
            EnumFacing side = FlatFeature.byte2side(index);
            int slX = (index >> 2) & 0xF;
            int slY = (index >> 6) & 0xF;
            int slZ = (index >> 10) & 0xF;
            at.x = (chunk.xPosition << 4) + slX;
            at.y = slabYHeight + slY;
            at.z = (chunk.zPosition << 4) + slZ;
            visitor.visit(at, side, face);
        }
    }

    static abstract class Data {
        static final int FULL_SIZE = 16 * 16 * 16 * 3;
        transient int set = 0;

        @Nullable
        abstract FlatFace get(short index);

        @Nonnull
        abstract Data set(short index, @Nullable FlatFace face, Coord at);

        final void replaced(@Nullable FlatFace orig, @Nullable Coord at, short index) {
            if (orig == null && at != null) set++;
            if (orig != null && at == null) set--;
            if (orig == null || at == null) {
                return;
            }
            EnumFacing face = FlatFeature.byte2side(index);
            orig.onReplaced(at, face);
        }

        public final boolean isEmpty() {
            return set == 0;
        }

        public abstract void iterate(IterateContext visitor);
    }

    static final FMLControlledNamespacedRegistry<FlatFace> registry = FlatFeature.registry;

    static class NoData extends Data {
        static final Data INSTANCE = new NoData();
        // 0 cache misses.
        @Override
        FlatFace get(short index) {
            return null;
        }

        @Nonnull
        @Override
        Data set(short index, FlatFace face, Coord at) {
            return new TinyData().set(index, face, at);
        }

        @Override
        public void iterate(IterateContext visitor) {
        }
    }

    static class TinyData extends Data {
        /*
        ~2 cache misses.
        The goal: Fit indices into a cacheline. My CPU is conservatively crappy; /proc/cpuinfo says 'cache_alignment: 64',
        so that probably means 64 bytes per cacheline. A short is 2 bytes. Therefore 16 shorts will fit in a cacheline.
        Of course the array may possibly not be aligned to this, so a second cache miss is possible.
        There will be an additional cache miss jumping to the correct FlatFace.
         */
        int size() {
            return 16;
        }

        final short[] indices = new short[size()];
        final FlatFace[] faces = new FlatFace[size()];

        @Override
        FlatFace get(short index) {
            int L = indices.length;
            for (int i = 0; i < L; i++) {
                if (indices[i] != index) continue;
                if (faces[i] == null) {
                    // Shouldn't happen.
                    indices[i] = 0;
                    continue;
                }
                return faces[i];
            }
            return null;
        }

        @Nonnull
        @Override
        Data set(short index, FlatFace face, Coord at) {
            int L = indices.length;
            for (int i = 0; i < L; i++) {
                int indexI = indices[i];
                if (indexI != 0 || indexI == index) continue;
                indices[i] = index;
                replaced(faces[i], at, index);
                faces[i] = face;
                return this;
            }
            return upsize().set(index, face, at);
        }

        @Override
        public void iterate(IterateContext visitor) {
            for (int i = 0; i < indices.length; i++) {
                int index = indices[i];
                FlatFace face = faces[i];
                visitor.visit(index, face);
            }
        }

        Data upsize() {
            Data ret = nextSize();
            int L = indices.length;
            for (int i = 0; i < L; i++) {
                short index = indices[i];
                if (index <= 0) continue;
                ret = ret.set(index, faces[i], null);
            }
            return ret;
        }

        Data nextSize() {
            return new SmallData();
        }
    }

    static class SmallData extends TinyData {
        @Override
        int size() {
            return 16 * 4;
        }

        @Override
        Data nextSize() {
            return new LargeData();
        }
    }


    static class LargeData extends Data {
        /*
        1 cache miss for static; not sure for dynamic; maybe 5.
        The IDs of static faces are stored.
        Dynamic faces have a sentinel value, and are stored in a hashmap.
         */
        final char[] data = new char[FULL_SIZE];
        HashMap<Integer, FlatFace> dynamic = new HashMap<>();
        static final int MAX_DYNAMIC = 16 * 16 * 16 / 4;

        FlatFace lookup(char id, short index) {
            if (id == FlatFeature.NO_FACE) {
                return null;
            }
            if (id == FlatFeature.DYNAMIC_SENTINEL) {
                return dynamic.get((int) index);
            }
            return registry.getObjectById(id);
        }

        @Nullable
        @Override
        FlatFace get(short index) {
            if (index < 0) return null;
            if (index >= data.length) return null;
            char id = data[index];
            return lookup(id, index);
        }

        @Nonnull
        @Override
        Data set(short index, @Nullable FlatFace face, Coord at) {
            if (index < 0) return this;
            if (index >= data.length) return this;
            char oldId = data[index];
            if (oldId != 0) {
                FlatFace oldFace = lookup(data[index], index);
                replaced(oldFace, at, index);
                if (oldFace.isDynamic() && (face == null || face.isStatic())) {
                    dynamic.remove((int) index);
                }
            }
            if (face == null) {
                data[index] = FlatFeature.NO_FACE;
            } else if (face.isStatic()) {
                data[index] = face.staticId;
            } else {
                data[index] = FlatFeature.DYNAMIC_SENTINEL;
                dynamic.put((int) index, face);
                if (dynamic.size() > MAX_DYNAMIC) {
                    return upsize();
                }
            }
            return this;
        }

        @Override
        public void iterate(IterateContext visitor) {
            for (int i = 0; i < FULL_SIZE; i++) {
                int index = data[i];
                if (index == FlatFeature.NO_FACE) continue;
                if (index == FlatFeature.DYNAMIC_SENTINEL) continue;
                FlatFace face = registry.getObjectById(index);
                visitor.visit(index, face);
            }
            for (Map.Entry<Integer, FlatFace> e : dynamic.entrySet()) {
                int index = e.getKey();
                FlatFace face = e.getValue();
                visitor.visit(index, face);
            }
        }

        Data upsize() {
            Data ret = new JumboData();
            for (short index = 0; index < data.length; index++) {
                int id = data[index];
                if (id == FlatFeature.NO_FACE || id == FlatFeature.DYNAMIC_SENTINEL) {
                    continue;
                }
                ret.set(index, registry.getObjectById(id), null);
            }

            for (Map.Entry<Integer, FlatFace> entry : dynamic.entrySet()) {
                int index = entry.getKey();
                FlatFace face = entry.getValue();
                if (face == null) continue;
                ret.set((short) index, face, null);
            }
            return ret;
        }
    }

    static class JumboData extends Data {
        // 32kb. Please don't use this.
        // On the plus side: Only 1 cache miss! :D
        final FlatFace[] data = new FlatFace[FULL_SIZE];

        @Nullable
        @Override
        FlatFace get(short index) {
            if (index < 0) return null;
            if (index >= FULL_SIZE) return null;
            return data[index];
        }

        @Nonnull
        @Override
        Data set(short index, @Nullable FlatFace face, Coord at) {
            if (index < 0) return this;
            if (index >= FULL_SIZE) return this;
            replaced(data[index], at, index);
            data[index] = face;
            return this;
        }

        @Override
        public void iterate(IterateContext visitor) {
            for (int index = 0; index < data.length; index++) {
                FlatFace face = data[index];
                if (face == null) continue;
                visitor.visit(index, face);
            }
        }
    }

    final Data[] slabs = new Data[16];

    public FlatChunkLayer() {
        for (int i = 0; i < slabs.length; i++) {
            slabs[i] = NoData.INSTANCE;
        }
    }

    private Data slabIndex(int localY) {
        int y = localY >> 4;
        if (y < 0 || y >= slabs.length) return NoData.INSTANCE;
        return slabs[y];
    }

    FlatFace get(Coord at, EnumFacing dir) {
        int localY = at.y;
        int localX = at.x >> 4;
        int localZ = at.z >> 4;
        Data slab = slabIndex(localY);
        short index = index(localX, localY, localZ, dir);
        return slab.get(index);
    }

    FlatFace set(Coord at, EnumFacing dir, FlatFace face) {
        int localY = at.y;
        int localX = at.x >> 4;
        int localZ = at.z >> 4;
        Data slab = slabIndex(localY);
        short index = index(localX, localY, localZ, dir);
        Data newSlab = slab.set(index, face, at);
        if (slab != newSlab) {
            slabs[localY >> 4] = newSlab;
        }
        return slab.get(index);
    }

    public void iterate(Chunk chunk, IFlatVisitor visitor) {
        for (int slabY = 0; slabY < slabs.length; slabY++) {
            Data slab = slabs[slabY];
            slab.iterate(new IterateContext(chunk, slabY, visitor));
        }
    }

    transient int set = 0;
    public boolean isEmpty() {
        return set == 0;
    }
}
