package factorization.flat;

import factorization.api.Coord;
import factorization.api.ICoordFunction;
import factorization.flat.api.AtSide;
import factorization.flat.api.FlatFace;
import factorization.flat.api.IFlatRenderInfo;
import factorization.flat.api.IFlatVisitor;
import factorization.net.FzNetDispatch;
import factorization.util.SpaceUtil;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.registry.FMLControlledNamespacedRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FlatChunkLayer {
    public static short index(int slabX, int slabY, int slabZ, EnumFacing face) {
        if (slabX < 0 || slabY < 0 || slabZ < 0) return -1;
        if (slabX > 0xF || slabY > 0xF || slabZ > 0xF) return -1;
        if (SpaceUtil.sign(face) != 1) return -1;
        int ret = (slabX << 2) | (slabY << 6) | (slabZ << 10) | FlatMod.side2byte(face);
        return (short) ret;
    }

    static class IterateContext {
        final Chunk chunk;
        final IFlatVisitor visitor;
        final Coord at;
        EnumFacing side;
        final int minX, minY, minZ;
        final int maxX, maxY, maxZ;

        IterateContext(Chunk chunk, int slabY, IFlatVisitor visitor) {
            this.chunk = chunk;
            this.visitor = visitor;
            this.at = new Coord(chunk.getWorld(), 0, 0, 0);
            this.minX = chunk.xPosition << 4;
            this.minY = slabY << 4;
            this.minZ = chunk.zPosition << 4;
            this.maxX = minX + 0xF;
            this.maxY = minY + 0xF;
            this.maxZ = minZ + 0xF;
        }

        void visit(int index, @Nullable FlatFace face) {
            if (unpack(index, face)) return;
            doVisit(face);
        }

        void doVisit(FlatFace face) {
            visitor.visit(at, side, face);
        }

        boolean unpack(int index, @Nullable FlatFace face) {
            if (face == null) return true;
            if (face.isNull()) return true;
            side = FlatMod.byte2side(index);
            int slX = (index >> 2) & 0xF;
            int slY = (index >> 6) & 0xF;
            int slZ = (index >> 10) & 0xF;
            at.x = minX + slX;
            at.y = minY + slY;
            at.z = minZ + slZ;
            return false;
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
            EnumFacing face = FlatMod.byte2side(index);
            orig.onReplaced(at, face);
        }

        public final boolean isEmpty() {
            return set == 0;
        }

        public abstract void iterate(IterateContext context);

        public abstract void iterateBounded(Coord min, Coord max, IterateContext context);
    }

    static final FMLControlledNamespacedRegistry<FlatFace> registry = FlatMod.staticReg;

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
        public void iterate(IterateContext context) {
        }

        @Override
        public void iterateBounded(Coord min, Coord max, IterateContext context) {

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
            int iFirstBlank = -1;
            for (int i = 0; i < L; i++) {
                int indexI = indices[i];
                if (indexI != index) {
                    if (indexI == 0 && iFirstBlank == -1) {
                        iFirstBlank = i;
                    }
                    continue;
                }
                doSet(index, face, at, i);
                return this;
            }
            if (iFirstBlank != -1) {
                doSet(index, face, at, iFirstBlank);
                return this;
            }
            return upsize().set(index, face, at);
        }

        private void doSet(short index, FlatFace face, Coord at, int i) {
            indices[i] = index;
            replaced(faces[i], at, index);
            faces[i] = face;
        }

        @Override
        public void iterate(IterateContext context) {
            for (int i = 0; i < indices.length; i++) {
                int index = indices[i];
                FlatFace face = faces[i];
                context.visit(index, face);
            }
        }

        @Override
        public void iterateBounded(Coord min, Coord max, IterateContext context) {
            for (int i = 0; i < indices.length; i++) {
                int index = indices[i];
                FlatFace face = faces[i];
                if (context.unpack(index, face)) continue;
                if (!context.at.inside(min, max)) continue;
                context.doVisit(face);
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
        Vanilla-style.
        1 cache miss for static; not sure for dynamic; maybe 5.
        The IDs of static faces are stored.
        Dynamic faces have a sentinel value, and are stored in a hashmap.
         */
        final char[] data = new char[FULL_SIZE];
        HashMap<Integer, FlatFace> dynamic = new HashMap<>();
        static final int MAX_DYNAMIC = 16 * 16 * 16 / 4;

        FlatFace lookup(char id, short index) {
            if (id == FlatMod.NO_FACE) {
                return null;
            }
            if (id == FlatMod.DYNAMIC_SENTINEL) {
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
                data[index] = FlatMod.NO_FACE;
            } else if (face.isStatic()) {
                data[index] = face.staticId;
            } else {
                data[index] = FlatMod.DYNAMIC_SENTINEL;
                dynamic.put((int) index, face);
                if (dynamic.size() > MAX_DYNAMIC) {
                    return upsize();
                }
            }
            return this;
        }

        @Override
        public void iterate(IterateContext context) {
            for (int i = 0; i < FULL_SIZE; i++) {
                int index = data[i];
                if (index == FlatMod.NO_FACE) continue;
                if (index == FlatMod.DYNAMIC_SENTINEL) continue;
                FlatFace face = registry.getObjectById(index);
                context.visit(index, face);
            }
            for (Map.Entry<Integer, FlatFace> e : dynamic.entrySet()) {
                int index = e.getKey();
                FlatFace face = e.getValue();
                context.visit(index, face);
            }
        }

        @Override
        public void iterateBounded(Coord min, Coord max, IterateContext context) {
            dumbIterate(this, min, max, context);
        }

        Data upsize() {
            Data ret = new JumboData();
            for (short index = 0; index < data.length; index++) {
                int id = data[index];
                if (id == FlatMod.NO_FACE || id == FlatMod.DYNAMIC_SENTINEL) {
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
        public void iterate(IterateContext context) {
            for (int index = 0; index < data.length; index++) {
                FlatFace face = data[index];
                context.visit(index, face);
            }
        }

        @Override
        public void iterateBounded(Coord min, Coord max, IterateContext context) {
            dumbIterate(this, min, max, context);
        }
    }

    static void dumbIterate(final Data data, Coord min, Coord max, final IterateContext context) {
        min = min.copy();
        max = max.copy();
        if (min.x < context.minX) min.x = context.minX;
        if (min.y < context.minY) min.y = context.minY;
        if (min.z < context.minZ) min.z = context.minZ;
        if (max.x > context.maxX) max.x = context.maxX;
        if (max.y > context.maxY) max.y = context.maxY;
        if (max.z > context.maxZ) max.z = context.maxZ;
        Coord.iterateCube(min, max, new ICoordFunction() {
            @Override
            public void handle(Coord at) {
                int localY = at.y & 0xF;
                int localX = at.x & 0xF;
                int localZ = at.z & 0xF;
                for (EnumFacing dir : POS_FACES) {
                    short index = index(localX, localY, localZ, dir);
                    FlatFace face = data.get(index);
                    context.visit(index, face);
                }
            }
        });
    }

    public static final EnumFacing[] POS_FACES = new EnumFacing[] { EnumFacing.UP, EnumFacing.SOUTH, EnumFacing.EAST };

    final Chunk chunk;
    final Data[] slabs = new Data[16];
    @Nullable
    final ArrayList<AtSide> changed; // Might've packed bits into an int[], but too difficult to be certain w/ Java
    public static final int MAX_CHANGES = 32;
    public final IFlatRenderInfo renderInfo = FlatMod.proxy.constructRenderInfo();

    public FlatChunkLayer(Chunk chunk) {
        this.chunk = chunk;
        for (int i = 0; i < slabs.length; i++) {
            slabs[i] = NoData.INSTANCE;
        }
        changed = chunk.getWorld().isRemote ? null : new ArrayList<AtSide>();
    }

    private Data slabIndex(int localY) {
        int y = localY >> 4;
        if (y < 0 || y >= slabs.length) return NoData.INSTANCE;
        return slabs[y];
    }

    public FlatFace get(Coord at, EnumFacing dir) {
        int localY = at.y & 0xF;
        int localX = at.x & 0xF;
        int localZ = at.z & 0xF;
        Data slab = slabIndex(at.y >> 4);
        short index = index(localX, localY, localZ, dir);
        if (index == -1) return FlatFaceAir.INSTANCE;
        FlatFace ret = slab.get(index);
        if (ret == null) {
            ret = FlatFaceAir.INSTANCE;
        }
        return ret;
    }

    public FlatFace set(Coord at, EnumFacing dir, FlatFace face) {
        if (face == FlatFaceAir.INSTANCE) {
            face = null;
        }
        int localY = at.y & 0xF;
        int localX = at.x & 0xF;
        int localZ = at.z & 0xF;
        Data slab = slabIndex(at.y >> 4);
        short index = index(localX, localY, localZ, dir);
        if (index == -1) {
            return FlatFaceAir.INSTANCE;
        }
        int oSet = slab.set;
        Data newSlab = slab.set(index, face, at);
        if (slab != newSlab) {
            slabs[localY >> 4] = newSlab;
        }
        set += newSlab.set - oSet;
        FlatFace ret = slab.get(index);
        renderInfo.markDirty(at);
        addChange(at, dir);
        chunk.setChunkModified();
        return ret;
    }

    public void iterate(IFlatVisitor visitor) {
        for (int slabY = 0; slabY < slabs.length; slabY++) {
            Data slab = slabs[slabY];
            IterateContext context = new IterateContext(chunk, slabY, visitor);
            slab.iterate(context);
        }
    }

    public void iterateBounded(Coord min, Coord max, IFlatVisitor visitor) {
        int minSlab = min.y >> 4;
        int maxSlab = 1 + ((max.y + 0xF) >> 4);
        if (minSlab < 0) minSlab = 0;
        if (maxSlab > slabs.length) maxSlab = slabs.length;
        for (int slabY = minSlab; slabY < maxSlab; slabY++) {
            Data slab = slabs[slabY];
            IterateContext context = new IterateContext(chunk, slabY, visitor);
            slab.iterateBounded(min, max, context);
        }
    }

    transient int set = 0;
    public boolean isEmpty() {
        return set == 0;
    }

    void discard() {
        renderInfo.discard();
    }

    void addChange(Coord at, EnumFacing side) {
        if (changed == null) return;
        if (changed.size() > MAX_CHANGES) return;
        changed.add(new AtSide(at, side));
        FlatNet.pending.add(this);
    }

    void updateClients() {
        if (changed == null) return;
        FlatNet.SyncWrite sw = new FlatNet.SyncWrite();
        if (changed.size() > MAX_CHANGES) {
            iterate(sw);
        } else {
            for (AtSide as : changed) {
                sw.visit(as.at, as.side, get(as.at, as.side));
            }
        }
        changed.clear();
        FzNetDispatch.addPacketFrom(FlatNet.build(sw.finish()), chunk);
    }
}
