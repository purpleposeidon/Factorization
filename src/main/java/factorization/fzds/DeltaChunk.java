package factorization.fzds;

import factorization.api.Coord;
import factorization.api.DeltaCoord;
import factorization.api.ICoordFunction;
import factorization.coremodhooks.IExtraChunkData;
import factorization.fzds.interfaces.IDimensionSlice;
import factorization.fzds.interfaces.transform.Pure;
import factorization.fzds.interfaces.transform.TransformData;
import factorization.fzds.network.InteractionLiason;
import factorization.fzds.network.PacketProxyingPlayer;
import factorization.util.NORELEASE;
import gnu.trove.set.hash.THashSet;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;

import java.util.*;

public class DeltaChunk {
    public static boolean enabled() {
        NORELEASE.fixme("Rename this class to FZDS?");
        return HammerEnabled.ENABLED;
    }

    public static void assertEnabled() {
        if (!enabled()) {
            throw new AssertionError("Hammer has been disabled by configuration");
        }
    }

    static DeltaChunkMap getSlices(World w) {
        if (w == null) {
            if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
                return Hammer.clientSlices;
            } else {
                return Hammer.serverSlices;
            }
        }
        return w.isRemote ? Hammer.clientSlices : Hammer.serverSlices;
    }

    public static Iterable<IDimensionSlice> getAllSlices(World w) {
        DeltaChunkMap dm = getSlices(w);
        ArrayList<IDimensionSlice> ret = new ArrayList<IDimensionSlice>();
        if (dm == null) return ret;
        for (IDimensionSlice[] array : dm.getIdcs()) {
            Collections.addAll(ret, array);
        }
        return ret;
    }
    
    public static IDimensionSlice[] getSlicesContainingPoint(Coord at) {
        return getSlices(at.w).get(at);
    }
    
    static boolean addSlice(IDimensionSlice dse) {
        return getSlices(dse.getRealWorld()).add(dse);
    }
    
    static Set<IDimensionSlice> getSlicesInRange(World w, int lx, int ly, int lz, int hx, int hy, int hz) {
        THashSet<IDimensionSlice> found_deltachunks = new THashSet<IDimensionSlice>(10);
        DeltaChunkMap map = DeltaChunk.getSlices(w); // NORELEASE: This guy keeps hold of dead DSEs? Such as after save reload.
        IDimensionSlice last_found = null;
        for (int x = lx; x <= hx; x += 16) {
            for (int z = lz; z <= hz; z += 16) {
                IDimensionSlice new_idcs[] = map.get(x/16, z/16);
                for (IDimensionSlice idc : new_idcs) {
                    if (idc == last_found) continue;
                    found_deltachunks.add(idc);
                    last_found = idc;
                }
            }
        }
        return found_deltachunks;
    }
    
    public static World getClientShadowWorld() {
        World ret = Hammer.worldClient;
        if (ret == null) {
            Hammer.proxy.createClientShadowWorld();
            return Hammer.worldClient;
        }
        return ret;
    }
    
    public static World getServerShadowWorld() {
        return DimensionManager.getWorld(getDimensionId());
    }
    
    public static World getClientRealWorld() {
        return Hammer.proxy.getClientRealWorld();
    }
    
    /***
     * @return the thread-appropriate shadow world
     */
    public static World getWorld(World realWorld) {
        boolean remote = realWorld == null ? FMLCommonHandler.instance().getEffectiveSide().isClient() : realWorld.isRemote;
        return remote ? getClientShadowWorld() : getServerShadowWorld();
    }
    
    public static IDimensionSlice allocateSlice(World spawnWorld, int channel, DeltaCoord size) {
        if (spawnWorld.isRemote) throw new IllegalArgumentException("Attempted client-side DSE allocation!");
        Coord base, end;
        if (NORELEASE.off) {
            base = new Coord(getServerShadowWorld(), 0, 4, 0);
            end = base.add(size);
        } else {
            base = Hammer.hammerInfo.takeCell(channel, size);
            end = base.add(size);
            wipeRegion(base, end);
        }
        return new DimensionSliceEntity(spawnWorld, base, end);
    }
    
    public static IDimensionSlice findClosest(Entity target, Coord pos) {
        if (target == null) {
            return null;
        }
        World real_world = target.worldObj;
        IDimensionSlice closest = null;
        double dist = Double.POSITIVE_INFINITY;
        for (IDimensionSlice here : DeltaChunk.getSlicesContainingPoint(pos)) {
            if (here.getRealWorld() != real_world && !pos.inside(here.getMinCorner(), here.getMaxCorner())) {
                continue;
            }
            if (closest == null) {
                closest = here;
                continue;
            }
            double here_dist = target.getDistanceSqToEntity(here.getEntity());
            if (here_dist < dist) {
                dist = here_dist;
                closest = here;
            }
        }
        return closest;
    }

    public static Iterable<IDimensionSlice> getAround(Coord pos, int radius) {
        final HashSet<IDimensionSlice> nearbyChunks = new HashSet<IDimensionSlice>(); // This should be extracted.
        Coord.iterateChunksAround(pos, radius, new ICoordFunction() {
            @Override
            public void handle(Coord here) {
                IExtraChunkData c = (IExtraChunkData) here.getChunk();
                Entity[] ccs = c.getConstantColliders();
                if (ccs == null) return;
                for (Entity ent : ccs) {
                    if (ent instanceof IDimensionSlice) {
                        nearbyChunks.add((IDimensionSlice) ent);
                    }
                }
            }
        });
        return nearbyChunks;
    }

    public static BlockPos shadow2nearestReal(Entity player, BlockPos pos) {
        return new BlockPos(shadow2nearestReal(player, new Vec3(pos)));
    }

    public static Vec3 shadow2nearestReal(Entity player, Vec3 vec) {
        //The JVM sometimes segfaults in this function.
        IDimensionSlice closest = findClosest(player, new Coord(player.worldObj, vec));
        if (closest == null) {
            return null;
        }
        return closest.shadow2real(vec);
    }

    public static int getDimensionId() {
        assertEnabled();
        return HammerInfo.dimension_slice_dimid;
    }

    public interface AreaMap {
        void fillDse(DseDestination destination);
    }
    
    public interface DseDestination {
        void include(Coord c);
    }
    
    public static void wipeRegion(final Coord min, final Coord max) {
        Coord.sort(min, max);
        Coord at = min.copy();
        for (int x = min.x; x <= max.x; x++) {
            at.x = x;
            for (int y = min.y; y <= max.y; y++) {
                at.y = y;
                for (int z = min.z; z <= max.z; z++) {
                    at.z = z;
                    at.setAir();
                }
            }
        }
    }

    public static IDimensionSlice makeSlice(int channel, Coord min, Coord max, Collection<Coord> coords, final boolean wipeSrc) {
        min = min.copy();
        max = max.copy();
        DeltaCoord size = max.difference(min);
        final IDimensionSlice dse = allocateSlice(min.w, channel, size);
        Vec3 vrm = min.centerVec(max);
        dse.getTransform().setPos(vrm);
        TransformData<Pure> t = dse.getTransform();
        NORELEASE.println(t);
        NORELEASE.println("Shadow min corner:", dse.getMinCorner());
        NORELEASE.println("Real Min corner:", min);
        NORELEASE.println("Shadow2real min:", dse.shadow2real(dse.getMinCorner()));
        NORELEASE.println("Real2shadow min:", dse.real2shadow(min));
        final HashSet<Chunk> chunks = new HashSet<Chunk>();
        for (Coord real : coords) {
            Coord shadow = dse.real2shadow(real);
            TransferLib.move(real, shadow, false, true);
            chunks.add(real.getChunk());
        }
        {
            // Force-load chunks to ensure that lighting updates happen
            // shadow.updateLight requires that chunks 17 blocks away be loaded...
            outsetChunks(chunks);
            outsetChunks(chunks);
        }
        for (Coord real : coords) {
            if (wipeSrc) {
                TransferLib.rawErase(real);
            }
            Coord shadow = dse.real2shadow(real);
            shadow.markBlockForUpdate();
            //shadow.updateLight(); // This may be too slow to actually use. :/
        }
        if (wipeSrc) {
            for (Coord real : coords) {
                real.markBlockForUpdate();
                real.notifyBlockChange();
            }
        }
        return dse;
    }

    public static IDimensionSlice makeSlice(int channel, Coord min, Coord max, AreaMap mapper, final boolean wipeSrc) {
        final ArrayList<Coord> buffer = new ArrayList<Coord>();
        mapper.fillDse(new DseDestination() {
            @Override
            public void include(Coord c) {
                buffer.add(c.copy());
            }
        });
        return makeSlice(channel, min, max, buffer, wipeSrc);
    }
    
    static void outsetChunks(Collection<Chunk> chunks) {
        ArrayList<Chunk> edges = new ArrayList<Chunk>();
        for (Chunk chunk : chunks) {
            for (EnumFacing fd : EnumFacing.VALUES) {
                if (fd.getDirectionVec().getY() != 0) continue;
                edges.add(chunk.getWorld().getChunkFromChunkCoords(chunk.xPosition + fd.getDirectionVec().getX(), chunk.zPosition + fd.getDirectionVec().getZ()));
            }
        }
        chunks.addAll(edges);
    }
    
    public static IDimensionSlice construct(World inWorld, final Coord min, final Coord max) {
        return new DimensionSliceEntity(inWorld, min, max);
    }
    
    public static void paste(IDimensionSlice selected, boolean overwriteDestination) {
        Coord a = new Coord(DeltaChunk.getServerShadowWorld(), 0, 0, 0);
        Coord b = a.copy();
        Vec3 vShadowMin = selected.getMinCorner().toVector();
        Vec3 vShadowMax = selected.getMaxCorner().toVector();
        a.set(vShadowMin);
        b.set(vShadowMax);
        Coord dest = new Coord(selected.getEntity());
        Coord c = new Coord(a.w, 0, 0, 0);
        
        int minX = 0, minY = 0, minZ = 0, maxX = 0, maxY = 0, maxZ = 0;
        boolean first = true;

        BlockPos.MutableBlockPos tmp = new BlockPos.MutableBlockPos();
        for (int x = a.x; x <= b.x; x++) {
            for (int y = a.y; y <= b.y; y++) {
                for (int z = a.z; z <= b.z; z++) {
                    c.set(a.w, x, y, z);
                    if (c.isAir()) continue;
                    dest.set(c);
                    selected.shadow2real(dest);
                    TransferLib.move(c, dest, false, overwriteDestination);
                    dest.w.markBlockForUpdate(dest.copyTo(tmp));
                    if (first) {
                        minX = maxX = x;
                        minY = maxY = y;
                        minZ = maxZ = z;
                        first = false;
                    } else {
                        minX = Math.min(minX, x);
                        minY = Math.min(minY, y);
                        minZ = Math.min(minZ, z);
                        maxX = Math.max(maxX, x);
                        maxY = Math.max(maxY, y);
                        maxZ = Math.max(maxZ, z);
                    }
                }
            }
        }
    }
    
    public static void clear(IDimensionSlice selected) {
        Coord a = new Coord(DeltaChunk.getServerShadowWorld(), 0, 0, 0);
        Coord b = a.copy();
        Vec3 vShadowMin = selected.getMinCorner().toVector();
        Vec3 vShadowMax = selected.getMaxCorner().toVector();
        a.set(vShadowMin);
        b.set(vShadowMax);
        
        Coord c = new Coord(a.w, 0, 0, 0);
        BlockPos.MutableBlockPos tmp = new BlockPos.MutableBlockPos();
        for (int x = a.x; x < b.x; x++) {
            for (int y = a.y; y < b.y; y++) {
                for (int z = a.z; z < b.z; z++) {
                    c.set(a.w, x, y, z);
                    selected.shadow2real(c).markBlockForUpdate();
                }
            }
        }
    }
    
    public static HammerInfo getHammerRegistry() {
        return Hammer.hammerInfo;
    }

    /**
     * Attempts to get the real player behind a proxing player.
     * @param player The player object
     * @return The true player. Might return null. Returns the argument if it is not an FZDS fake player
     */
    public static EntityPlayer getRealPlayer(EntityPlayer player) {
        if (player == Hammer.proxy.getFakePlayerWhileInShadow()) {
            return Hammer.proxy.getRealPlayerWhileInShadow();
        }
        if (player instanceof InteractionLiason) {
            return ((InteractionLiason) player).getRealPlayer();
        }
        if (player instanceof PacketProxyingPlayer) {
            return null;
        }
        return player;
    }
}
