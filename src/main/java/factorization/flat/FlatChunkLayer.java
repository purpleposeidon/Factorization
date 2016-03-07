package factorization.flat;

import factorization.api.Coord;
import factorization.api.ICoordFunction;
import factorization.flat.api.AtSide;
import factorization.flat.api.FlatFace;
import factorization.flat.api.IFlatRenderInfo;
import factorization.flat.api.IFlatVisitor;
import factorization.net.FzNetDispatch;
import factorization.shared.Core;
import factorization.util.SpaceUtil;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraftforge.fml.common.registry.FMLControlledNamespacedRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class FlatChunkLayer {
    public static short index(int slabX, int slabY, int slabZ, EnumFacing face) {
        if (slabX < 0 || slabY < 0 || slabZ < 0) return -1;
        if (slabX > 0xF || slabY > 0xF || slabZ > 0xF) return -1;
        if (SpaceUtil.sign(face) != 1) return -1;
        int ret = (slabX) | (slabY << 4) | (slabZ << 8);
        ret *= 3;
        ret += FlatMod.side2byte(face);
        return (short) ret;
    }

    static class IterateContext {
        final Chunk chunk;
        final IFlatVisitor visitor;
        final Coord at;
        EnumFacing side;
        final int slabX, slabY, slabZ;
        final int minX, minY, minZ;
        final int maxX, maxY, maxZ;

        IterateContext(Chunk chunk, int slabY, IFlatVisitor visitor) {
            this.chunk = chunk;
            this.visitor = visitor;
            this.at = new Coord(chunk.getWorld(), 0, 0, 0);
            this.slabX = this.minX = chunk.xPosition << 4;
            this.slabY = this.minY = slabY << 4;
            this.slabZ = this.minZ = chunk.zPosition << 4;
            this.maxX = minX + 0xF;
            this.maxY = minY + 0xF;
            this.maxZ = minZ + 0xF;
        }

        IterateContext(Chunk chunk, int slabY, IFlatVisitor visitor, Coord min, Coord max) {
            this.chunk = chunk;
            this.visitor = visitor;
            this.at = new Coord(chunk.getWorld(), 0, 0, 0);
            this.slabX = chunk.xPosition << 4;
            this.slabY = slabY << 4;
            this.slabZ = chunk.zPosition << 4;
            this.minX = Math.max(min.x, slabX);
            this.minY = Math.max(this.slabY, min.y);
            this.minZ = Math.max(min.z, slabZ);
            this.maxX = Math.min(max.x, slabX + 0xF);
            this.maxY = max.y;
            this.maxZ = Math.min(max.z, slabZ + 0xF);
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
            unpackIndex(index);
            return false;
        }

        void unpackIndex(int index) {
            side = FlatMod.byte2side(index);
            final int i = (index - FlatMod.side2byte(side)) / 3;
            int slX = (i) & 0xF;
            int slY = (i >> 4) & 0xF;
            int slZ = (i >> 8) & 0xF;
            at.x = slabX + slX;
            at.y = slabY + slY;
            at.z = slabZ + slZ;
        }
    }

    static abstract class Slab {
        static final short FULL_SIZE = 16 * 16 * 16 * 3;
        transient int set = 0;

        @Nullable
        abstract FlatFace get(short index);

        @Nonnull
        abstract Slab set(short index, @Nullable FlatFace face, Coord at);

        final void replaced(@Nullable FlatFace orig, @Nullable FlatFace repl, @Nullable Coord at, short index) {
            if (orig == null && repl != null) set++;
            if (orig != null && repl == null) set--;
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

    static class NoSlab extends Slab {
        static final Slab INSTANCE = new NoSlab();
        // 0 cache misses.
        @Override
        FlatFace get(short index) {
            return null;
        }

        @Nonnull
        @Override
        Slab set(short index, FlatFace face, Coord at) {
            return new TinySlab().set(index, face, at);
        }

        @Override
        public void iterate(IterateContext context) {
        }

        @Override
        public void iterateBounded(Coord min, Coord max, IterateContext context) {

        }
    }

    static class TinySlab extends Slab {
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
        Slab set(short index, FlatFace face, Coord at) {
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
            FlatFace orig = faces[i];
            indices[i] = face == null ? 0 : index;
            faces[i] = face;
            replaced(orig, face, at, index);
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

        Slab upsize() {
            Slab ret = nextSize();
            int L = indices.length;
            for (int i = 0; i < L; i++) {
                short index = indices[i];
                if (index <= 0) continue;
                ret = ret.set(index, faces[i], null);
            }
            return ret;
        }

        Slab nextSize() {
            return new SmallSlab();
        }
    }

    static class SmallSlab extends TinySlab {
        @Override
        int size() {
            return 16 * 4;
        }

        @Override
        Slab nextSize() {
            return new LargeSlab();
        }
    }

    /*
    static class PaletteSlab extends Slab {
        static final int COLUMNS = 16 * 16;
        static final int COL_LENGTH = FULL_SIZE / COLUMNS;
        static {
            //noinspection ConstantConditions
            assert FULL_SIZE % COLUMNS == 0;
        }
        private static class Palette {
            final ToothArray data = new ToothArray(COL_LENGTH);
            final FlatFace[] faces = new FlatFace[3];
            private static final byte DYNAMIC_SENTINEL = 0x3;

            @Nullable
            FlatFace get(Map<Integer, FlatFace> dynamics, short slabIndex) {
                int index = slabIndex % COL_LENGTH;
                int tooth = data.get(index);
                if (tooth == DYNAMIC_SENTINEL) {
                    return dynamics.get((int) slabIndex);
                }
                return faces[tooth];
            }

            // Return true if setable, else false.
            void set(Map<Integer, FlatFace> dynamics, short slabIndex, @Nullable FlatFace face) {
                int index = slabIndex % COL_LENGTH;
                if (face != null && face.isDynamic()) {
                    data.set(index, DYNAMIC_SENTINEL);
                    dynamics.put((int) slabIndex, face);
                    return;
                }
                if (data.get(index) == DYNAMIC_SENTINEL) {
                    dynamics.remove((int) slabIndex);
                }
                byte first_empty = -1;
                for (byte tooth = 0; tooth < DYNAMIC_SENTINEL; tooth++) {
                    FlatFace knownType = faces[tooth];
                    if (knownType == face) {
                        data.set(index, tooth);
                    } else if (knownType == null && first_empty == -1) {
                        first_empty = tooth;
                    }
                }
                if (first_empty == -1) {
                    data.set(index, DYNAMIC_SENTINEL);
                    dynamics.put((int) slabIndex, face);
                    return;
                }
                faces[first_empty] = face;
                data.set(index, first_empty);
            }
        }

        final Palette[] data = new Palette[COLUMNS];
        final HashMap<Integer, FlatFace> dynamics = new HashMap<Integer, FlatFace>(0);
        transient int true_dynamic_count = 0;
        static final int MAX_DYNAMIFIED_STATIC = 16 * 16 * 4;

        Palette getPalette(int index, boolean create) {
            int col = index / COL_LENGTH;
            if (data[col] == null) {
                if (!create) return null;
                return data[col] = new Palette();
            }
            return data[col];
        }

        @Nullable
        @Override
        FlatFace get(short index) {
            Palette p = getPalette(index, false);
            if (p == null) return null;
            return p.get(dynamics, index);
        }

        @Nonnull
        @Override
        Slab set(short index, @Nullable FlatFace face, Coord at) {
            Palette p = getPalette(index, true);
            FlatFace orig = p.get(dynamics, index);
            p.set(dynamics, index, face);
            replaced(orig, face, at, index);
            boolean oldDynamic = orig != null && orig.isDynamic();
            boolean newDynamic = face != null && face.isDynamic();
            if (oldDynamic && !newDynamic) true_dynamic_count--;
            if (!oldDynamic && newDynamic) true_dynamic_count++;

            // Note that we don't call upsize.set because this slab already knows
            if (true_dynamic_count > MAX_DYNAMIFIED_STATIC) {
                return upsize(new LargeSlab());
            }
            int true_dynamic = dynamics.size() - true_dynamic_count;
            if (true_dynamic > MAX_DYNAMIFIED_STATIC) {
                return upsize(new JumboSlab());
            }
            return this;
        }

        private Slab upsize(Slab ret) {
            short index = 0;
            while (index < FULL_SIZE) {
                Palette p = getPalette(index, false);
                if (p == null) {
                    index += COL_LENGTH;
                    continue;
                }
                FlatFace face = get(index);
                if (face != null) {
                    ret = ret.set(index, face, null);
                }
                index++;
            }
            return ret;
        }

        @Override
        public void iterate(IterateContext context) {
            short index = 0;
            while (index < FULL_SIZE) {
                Palette p = getPalette(index, false);
                if (p == null) {
                    index += COL_LENGTH;
                    continue;
                }
                FlatFace face = get(index);
                context.visit(index, face);
                index++;
            }
        }

        @Override
        public void iterateBounded(Coord min, Coord max, IterateContext context) {
            dumbIterate(this, min, max, context);
        }
    }*/

    static class LargeSlab extends Slab {
        /*
        Vanilla-style.
        1 cache miss for static; not sure for dynamic; maybe 5.
        The IDs of static faces are stored.
        Dynamic faces have a sentinel value, and are stored in a hashmap.
         */
        final byte[] data = new byte[FULL_SIZE];
        HashMap<Integer, FlatFace> dynamic = new HashMap<>();
        static final int MAX_DYNAMIC = 16 * 16 * 16 / 4;

        FlatFace lookup(byte id, short index) {
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
            byte id = data[index];
            return lookup(id, index);
        }

        @Nonnull
        @Override
        Slab set(short index, @Nullable FlatFace face, Coord at) {
            if (index < 0) return this;
            if (index >= data.length) return this;
            byte oldId = data[index];
            FlatFace oldFace = lookup(data[index], index);
            if (oldId != 0) {
                if (oldFace.isDynamic() && (face == null || face.isStatic())) {
                    dynamic.remove((int) index);
                }
            }
            boolean checkDynamic = false;
            if (face == null) {
                data[index] = FlatMod.NO_FACE;
            } else if (face.isStatic()) {
                data[index] = face.staticId;
            } else {
                data[index] = FlatMod.DYNAMIC_SENTINEL;
                dynamic.put((int) index, face);
                checkDynamic = true;
            }
            replaced(oldFace, face, at, index);
            if (checkDynamic && dynamic.size() > MAX_DYNAMIC) {
                return upsize();
            }
            return this;
        }

        @Override
        public void iterate(IterateContext context) {
            for (int i = 0; i < FULL_SIZE; i++) {
                int id = data[i];
                if (id == FlatMod.NO_FACE) continue;
                if (id == FlatMod.DYNAMIC_SENTINEL) continue;
                FlatFace face = registry.getObjectById(id);
                context.visit(i, face);
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

        Slab upsize() {
            Slab ret = new JumboSlab();
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

    static class JumboSlab extends Slab {
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
        Slab set(short index, @Nullable FlatFace face, Coord at) {
            if (index < 0) return this;
            if (index >= FULL_SIZE) return this;
            FlatFace orig = data[index];
            data[index] = face;
            replaced(orig, face, at, index);
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

    static class TestSlab extends Slab {
        Slab[] all = new Slab[] {
                new JumboSlab(),
                new LargeSlab(),
                //new PaletteSlab(),
                new SmallSlab(),
                new TinySlab(),
        };

        @Nullable
        @Override
        FlatFace get(short index) {
            FlatFace b = null;
            boolean first = true;
            for (Slab slab : all) {
                if (first) {
                    b = slab.get(index);
                    first = false;
                } else {
                    if (b != slab.get(index)) {
                        throw new AssertionError("Mismatch! >:O");
                    }
                }
            }
            return b;
        }

        @Nonnull
        @Override
        Slab set(short index, @Nullable FlatFace face, Coord at) {
            for (int i = 0; i < all.length; i++) {
                all[i] = all[i].set(index, face, at);
            }
            return this;
        }

        @Override
        public void iterate(IterateContext context) {
            all[0].iterate(context);
        }

        @Override
        public void iterateBounded(Coord min, Coord max, IterateContext context) {
            all[0].iterateBounded(min, max, context);
        }
    }

    private static void dumbIterate(final Slab slab, Coord min, Coord max, final IterateContext context) {
        min = min.copy();
        max = max.copy();
        min.x = context.minX;
        min.y = context.minY;
        min.z = context.minZ;
        max.x = context.maxX;
        max.y = context.maxY;
        max.z = context.maxZ;
        Coord.iterateCube(min, max, new ICoordFunction() {
            @Override
            public void handle(Coord at) {
                int localY = at.y & 0xF;
                int localX = at.x & 0xF;
                int localZ = at.z & 0xF;
                for (EnumFacing dir : POS_FACES) {
                    short index = index(localX, localY, localZ, dir);
                    FlatFace face = slab.get(index);
                    context.visit(index, face);
                }
            }
        });
    }

    public static final EnumFacing[] POS_FACES = new EnumFacing[] { EnumFacing.UP, EnumFacing.SOUTH, EnumFacing.EAST };

    final Chunk chunk;
    final Slab[] slabs = new Slab[16];
    @Nullable
    final Queue<AtSide> changed; // Might've packed bits into an int[], but too difficult to be certain w/ Java
    public static final int MAX_CHANGES = 32;
    public final IFlatRenderInfo renderInfo = FlatMod.proxy.constructRenderInfo();

    public FlatChunkLayer(Chunk chunk) {
        this.chunk = chunk;
        for (int i = 0; i < slabs.length; i++) {
            slabs[i] = NoSlab.INSTANCE;
        }
        changed = chunk.getWorld().isRemote ? null : new ConcurrentLinkedQueue<AtSide>();
    }

    private Slab slabIndex(int localY) {
        int y = localY >> 4;
        if (y < 0 || y >= slabs.length) return NoSlab.INSTANCE;
        return slabs[y];
    }

    public FlatFace get(Coord at, EnumFacing dir) {
        int localY = at.y & 0xF;
        int localX = at.x & 0xF;
        int localZ = at.z & 0xF;
        Slab slab = slabIndex(at.y >> 4);
        short index = index(localX, localY, localZ, dir);
        if (index == -1) return FlatFaceAir.INSTANCE;
        FlatFace ret = slab.get(index);
        if (ret == null) {
            ret = FlatFaceAir.INSTANCE;
        }
        return ret;
    }

    public FlatFace set(Coord at, EnumFacing dir, FlatFace face) {
        if (chunk instanceof EmptyChunk) {
            Core.logWarning("FlatLayer.set called on empty chunk", chunk);
        }
        if (face == FlatFaceAir.INSTANCE) {
            face = null;
        }
        int localY = at.y & 0xF;
        int localX = at.x & 0xF;
        int localZ = at.z & 0xF;
        Slab slab = slabIndex(at.y >> 4);
        short index = index(localX, localY, localZ, dir);
        if (index == -1) {
            return FlatFaceAir.INSTANCE;
        }
        int oSet = slab.set;
        Slab newSlab = slab.set(index, face, at);
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
            Slab slab = slabs[slabY];
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
            Slab slab = slabs[slabY];
            if (slab.isEmpty()) continue;
            IterateContext context = new IterateContext(chunk, slabY, visitor, min, max);
            // FIXME: Check if we are entirely contained within min & max. If so, use unbounded iterate.
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
            while (true) {
                AtSide as = changed.poll();
                if (as == null) return;
                sw.visit(as.at, as.side, get(as.at, as.side));
            }
        }
        changed.clear();
        FzNetDispatch.addPacketFrom(FlatNet.build(sw.finish()), chunk);
    }

}
