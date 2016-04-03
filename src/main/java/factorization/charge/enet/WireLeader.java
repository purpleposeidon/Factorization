package factorization.charge.enet;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import factorization.algos.FastBag;
import factorization.api.Coord;
import factorization.api.ICoordFunction;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;
import factorization.charge.ISuperChargeable;
import factorization.charge.sparkling.EntitySparkling;
import factorization.common.FzConfig;
import factorization.flat.FlatChunkLayer;
import factorization.flat.api.*;
import factorization.shared.Core;
import factorization.util.NORELEASE;
import factorization.util.SpaceUtil;
import net.minecraft.client.particle.EntitySpellParticleFX;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;

public class WireLeader extends WireCharge implements ISuperChargeable {
    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        super.serialize(prefix, data);
        powerSum = data.as(Share.PRIVATE, "powerSum").putInt(powerSum);
        if (data.isNBT()) {
            NBTTagCompound tag = data.getTag();
            if (data.isWriter()) {
                writeCollection(tag, "members", members);
                writeCollection(tag, "neighbors", neighbors);
            } else {
                readCollection(tag, "members", members);
                readCollection(tag, "neighbors", neighbors);
            }
        }
        return this;
    }

    private static void writeCollection(NBTTagCompound out, String key, Collection<MemberPos> list) {
        int[] data = new int[4 * list.size()];
        int i = 0;
        for (MemberPos member : list) {
            data[i++] = member.x;
            data[i++] = member.y;
            data[i++] = member.z;
            data[i++] = member.side;
        }
        out.setTag(key, new NBTTagIntArray(data));
    }

    private static void readCollection(NBTTagCompound in, String key, Collection<MemberPos> list) {
        int[] data = in.getIntArray(key);
        int i = 0;
        while (i < data.length) {
            int x = data[i++];
            int y = data[i++];
            int z = data[i++];
            int side = data[i++];
            list.add(new MemberPos(x, y, z, side));
        }
    }

    static void connect(WireLeader a, MemberPos aPos, WireLeader b, MemberPos bPos) {
        a.neighbors.add(bPos);
        b.neighbors.add(aPos);
    }

    static void disconnect(WireLeader a, MemberPos aPos, WireLeader b, MemberPos bPos) {
        a.neighbors.remove(bPos);
        b.neighbors.remove(aPos);
    }


    final Set<MemberPos> members = Sets.newHashSet();
    final List<MemberPos> neighbors = new FastBag<MemberPos>();
    int powerSum = 0;
    boolean disturbed = true; // Recalculate neighbors on load. Our neighbors may have broken themselves.

    boolean isFull() {
        return members.size() >= MAX_SIZE;
    }

    public void addMember(Coord at, EnumFacing side) {
        disturbed = true;
        members.add(new MemberPos(at, side));
    }

    @Override
    public void onSet(final Coord at, final EnumFacing side) {
        addMember(at, side);
        final MemberPos myPos = new MemberPos(at, side);
        iterateConnectable(new FlatCoord(at, side), new Function<FlatCoord, Void>() {
            @Override
            public Void apply(FlatCoord input) {
                FlatFace face = input.get();
                if (face.getSpecies() == SPECIES) {
                    if (face instanceof WireLeader) {
                        if (face == WireLeader.this) return null;
                        WireLeader neighbor = (WireLeader) face;
                        MemberPos neighborPos = new MemberPos(input);
                        connect(WireLeader.this, myPos, neighbor, neighborPos);
                    }
                }
                return null;
            }
        });
        ChargeEnetSubsys.instance.registerLeader(new FlatCoord(at, side));
    }

    @Override
    public void onNeighborFaceChanged(Coord at, EnumFacing side) {
        disturbed = true;
    }

    void tick(FlatCoord at) {
        if (members.isEmpty()) {
            Core.logSevere("I'm not in my own network! " + at);
            return;
        }
        if (disturbed) {
            recalculateNeighbors(at);
        }
        boolean anyEating = false;
        World w = at.at.w;
        Random rng = w.rand;
        ChunkCoordIntPair chunkPos = at.at.getChunk().getChunkCoordIntPair();
        for (Iterator<MemberPos> iter = neighbors.iterator(); iter.hasNext(); ) {
            MemberPos npos = iter.next();
            Coord nat = npos.getCoord(w);
            if (!nat.blockExists()) {
                continue;
            }
            FlatFace nface = npos.get(at);
            if (nface.getSpecies() != SPECIES || !(nface instanceof WireLeader)) {
                iter.remove();
                continue;
            }
            WireLeader neighbor = (WireLeader) nface;
            if (eatNeighbor(w, chunkPos, npos, neighbor, at)) {
                // This has modified the list, so we have to stop early.
                anyEating = true;
                break;
            }
            final int totalPower = powerSum + neighbor.powerSum;
            final int wires = members.size() + neighbor.members.size();
            final int powerPerWire = totalPower / wires;
            final int extraPower = totalPower % wires;
            int mine = this.members.size() * powerPerWire;
            int yours = neighbor.members.size() * powerPerWire;
            if (rng.nextBoolean()) {
                mine += extraPower;
            } else {
                yours += extraPower;
            }
            powerSum = mine;
            neighbor.powerSum = yours;
        }
        disturbed = anyEating;
    }


    private void recalculateNeighbors(FlatCoord at) {
        final List<MemberPos> oldNeighbors = ImmutableList.copyOf(neighbors);

        final MemberPos myPos = new MemberPos(at);
        if (!neighbors.isEmpty()) {
            // Unlink ourselves from the neighbor lists
            while (!neighbors.isEmpty()) {
                MemberPos npos = neighbors.remove(0);
                FlatFace ff = npos.get(at);
                if (ff.getSpecies() != SPECIES || !(ff instanceof WireLeader)) {
                    continue;
                }
                disconnect(this, myPos, (WireLeader) ff, npos);
            }
        }

        // Check every member for unknown coordinates
        Set<MemberPos> foreigners = Sets.newHashSet();
        for (MemberPos member : members) {
            iterateConnectable(member.getFlatCoord(at), new Function<FlatCoord, Void>() {
                @Override
                public Void apply(FlatCoord input) {
                    FlatFace ff = input.get();
                    if (ff.getSpecies() != SPECIES) return null;
                    MemberPos e = new MemberPos(input);
                    if (members.contains(e)) return null;
                    foreigners.add(e);
                    return null;
                }
            });
        }

        // Locality suggests that neighbors rarely move
        for (MemberPos npos : oldNeighbors) {
            FlatFace ff = npos.get(at);
            if (ff instanceof WireLeader) {
                WireLeader neighbor = (WireLeader) ff;
                if (foreigners.removeAll(neighbor.members)) {
                    connect(this, myPos, neighbor, npos);
                }
            }
        }
        if (foreigners.isEmpty()) return;

        // Check all neighbors in the region for ownership of the foreigners
        { // visit-all-dynamics
            final IFlatVisitor checkLeaders = new IFlatVisitor() {
                @Override
                public void visit(Coord at, EnumFacing side, @Nonnull FlatFace face) {
                    if (face.getSpecies() != SPECIES) return;
                    if (!(face instanceof WireLeader)) return;
                    WireLeader nl = (WireLeader) face;
                    if (foreigners.removeAll(nl.members)) {
                        connect(WireLeader.this, myPos, nl, new MemberPos(at, side));
                    }
                }
            };
            Coord min = at.at.add(-16, 0, -16); // dy'd really be MAX_SIZE * 2, if we had iterateDynamicsBounded
            Coord max = at.at.add(+16, 0, +16);
            Coord.iterateChunks(min, max, new ICoordFunction() {
                @Override
                public void handle(Coord here) {
                    Flat.iterateDynamics(here.getChunk(), checkLeaders);
                }
            });
        }
        // An alternative implementation of the above block:
        // It's, like, O(1) vs O(N^2) or something [What? -neptune, a month later]. But is f(MAX_SIZE) anyways.
        // So what are the constants? FIXME: Profile the two?
        // Could also choose based on the complexity of the slabs. Blegh.
        // visit-all-dynamics would probably have terrible performance on JumboSlabs.
        /*{ // search-for-leaders
            while (!foreigners.isEmpty()) {
                MemberPos foreigner = FzUtil.chooseOne(foreigners);
                assert foreigner != null;
                FlatCoord fat = foreigner.getFlatCoord(at);
                new LeaderSearch(fat.at, fat.side) {
                    @Override
                    boolean onFound(WireLeader leader, FlatCoord input) {
                        neighbors.add(new MemberPos(input));
                        foreigners.removeAll(leader.members);
                        return true;
                    }
                };
            }
        }*/
    }

    private boolean eatNeighbor(World world, ChunkCoordIntPair chunkPos, MemberPos npos, WireLeader neighbor, FlatCoord mypos) {
        if (!disturbed) return false;
        int c = members.size() + neighbor.members.size();
        if (c >= MAX_SIZE) return false;
        if (chunkPos.chunkXPos != (npos.x >> 4)) return false;
        if (chunkPos.chunkZPos != (npos.z >> 4)) return false;
        members.addAll(neighbor.members);
        HashSet<MemberPos> newNeighbors = new HashSet<MemberPos>(neighbors.size() + neighbor.neighbors.size());
        newNeighbors.addAll(neighbors);
        newNeighbors.addAll(neighbor.neighbors);
        newNeighbors.remove(npos);
        newNeighbors.remove(new MemberPos(mypos));
        neighbors.clear();
        neighbors.addAll(newNeighbors);
        members.addAll(neighbor.members);
        neighbor.disconnectAll(world, npos);
        neighbor.members.clear();
        powerSum += neighbor.powerSum;
        npos.set(world, ChargeEnetSubsys.instance.wire0, FlatChunkLayer.FLAGS_SEAMLESS);
        return true;
    }

    void disconnectAll(World w, MemberPos myPos) {
        for (MemberPos neighborPos : neighbors) {
            FlatFace ff = neighborPos.get(w);
            if (ff instanceof WireLeader) {
                WireLeader neighbor = (WireLeader) ff;
                disconnect(this, myPos, neighbor, neighborPos);
            }
        }
    }

    public void scatter(Coord at, EnumFacing side) {
        // Not the most efficient operation, but probably the simplest way of implementing it.
        int size = members.size();
        List<MemberPos> membersCopy = ImmutableList.copyOf(members);
        members.clear();
        for (MemberPos pos : membersCopy) {
            if (pos.get(at).getSpecies() != SPECIES) continue;
            Coord pat = pos.getCoord(at.w);
            EnumFacing pside = EnumFacing.getFront(pos.side);
            WireLeader toSet = new WireLeader();
            if (toSet.isValidAt(pat, pside)) {
                int toGive = powerSum / size;
                toSet.powerSum = toGive;
                powerSum -= toGive;
                int flags = ~(FlatChunkLayer.FLAG_SYNC | FlatChunkLayer.FLAG_CALL_REPLACE);
                Flat.setWithNotification(pat, pside, toSet, (byte) flags);
                ChargeEnetSubsys.instance.dirtyCache(at.w, pos);
            }
            size--;
        }
        MemberPos myPos = new MemberPos(at, side);
        for (MemberPos npos : neighbors) {
            FlatFace nface = npos.get(at);
            if (nface.getSpecies() != SPECIES) continue;
            if (!(nface instanceof WireLeader)) continue;
            WireLeader neighbor = (WireLeader) nface;
            disconnect(this, myPos, neighbor, npos);
        }
        neighbors.clear();
    }

    @Override
    public FlatFace cloneDynamic(Coord at, EnumFacing side) {
        return new WireLeader();
    }

    @Override
    public void onReplaced(Coord at, EnumFacing side) {
        members.remove(new MemberPos(at, side));
        super.onReplaced(at, side);
    }

    @Override
    public FlatFace getForClient() {
        return ChargeEnetSubsys.instance.wire0;
    }

    public boolean injectPower(Coord at, EnumFacing dir) {
        if (powerSum >= members.size() * 3) return injectSparkling(at, dir);
        powerSum++;
        return true;
    }

    private transient EntitySparkling last_spark_cache = null;
    private transient MemberPos last_spark_pos = null;
    private boolean injectSparkling(Coord at, EnumFacing dir) {
        if (at.w.getDifficulty() == EnumDifficulty.PEACEFUL || !FzConfig.spawn_sparklings) return false;
        if (members.isEmpty()) return false;

        if (last_spark_cache != null && last_spark_pos != null && members.contains(last_spark_pos)) {
            if (last_spark_cache.getHealth() >= 0 && expand(at).intersectsWith(last_spark_cache.getEntityBoundingBox())) {
                feed(last_spark_cache);
                return true;
            } else {
                last_spark_cache = null;
                last_spark_pos = null;
            }
        }

        ArrayList<MemberPos> shuffled = new ArrayList<MemberPos>(members);
        Collections.shuffle(shuffled);
        for (MemberPos mp : shuffled) {
            if (mp == last_spark_pos) continue;
            Coord mat = mp.getCoord(at.w);
            EnumFacing md = mp.getSide();
            if (injectAt(mat, md)) {
                last_spark_pos = mp;
                return true;
            }
        }

        return false;
    }

    AxisAlignedBB expand(Coord at) {
        int d = 2;
        Coord min = at.add(-d, -d, -d);
        Coord max = at.add(+d, +d, +d);
        AxisAlignedBB box = SpaceUtil.newBox(min, max);
        return box;
    }

    private boolean injectAt(Coord at, EnumFacing dir) {
        class Findit implements Predicate<EntitySparkling> {
            boolean found = false;
            @Override
            public boolean apply(EntitySparkling input) {
                if (found) return false;
                feed(input);
                found = true;
                last_spark_cache = input;
                return false;
            }
        }
        Findit findit = new Findit();
        AxisAlignedBB box = expand(at);
        at.w.getEntitiesWithinAABB(EntitySparkling.class, box, findit);
        if (findit.found) return true;

        {
            if (at.isSolid()) {
                at = at.add(dir);
                if (at.isSolid()) {
                    return false; // Hrm. :/
                }
            }
            last_spark_cache = new EntitySparkling(at.w);
            at.setAsEntityLocation(last_spark_cache);
            last_spark_cache.setSurgeLevel(1, true);
            at.w.spawnEntityInWorld(last_spark_cache);
        }
        return true;
    }

    void feed(EntitySparkling spark) {
        spark.setSurgeLevel(spark.getSurgeLevel() + 1, true);
    }

    @Nullable
    @Override
    public IFlatModel getModel(Coord at, EnumFacing side) {
        return ChargeEnetSubsys.instance.wire0.getModel(at, side);
    }

    @Override
    public void loadModels(IModelMaker maker) {
    }

    @Override
    public void superCharge() {
        powerSum = members.size() * 3;
    }
}
