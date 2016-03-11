package factorization.charge.enet;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import factorization.api.Coord;
import factorization.api.ICoordFunction;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;
import factorization.flat.FlatChunkLayer;
import factorization.flat.api.Flat;
import factorization.flat.api.FlatCoord;
import factorization.flat.api.FlatFace;
import factorization.flat.api.IFlatVisitor;
import factorization.shared.Core;
import factorization.util.FzUtil;
import factorization.util.NORELEASE;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;

public class WireLeader extends WireCharge {
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
        NBTTagList membersTag = new NBTTagList();
        for (MemberPos member : list) {
            membersTag.appendTag(member.toArray());
        }
        out.setTag(key, membersTag);
    }

    private static void readCollection(NBTTagCompound in, String key, Collection<MemberPos> list) {
        NBTTagList membersTag = in.getTagList(key, Constants.NBT.TAG_INT_ARRAY);
        int mlen = membersTag.tagCount();
        for (int i = 0; i < mlen; i++) {
            int t[] = membersTag.getIntArrayAt(i);
            list.add(new MemberPos(t));
        }
    }

    final Set<MemberPos> members = Sets.newHashSet();
    final List<MemberPos> neighbors = Lists.newArrayList();
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
    public void onSet(Coord at, EnumFacing side) {
        addMember(at, side);
        iterateConnectable(new FlatCoord(at, side), new Function<FlatCoord, Void>() {
            @Override
            public Void apply(FlatCoord input) {
                FlatFace face = input.get();
                if (face.getSpecies() == SPECIES) {
                    if (face instanceof WireLeader) {
                        if (face == WireLeader.this) return null;
                        neighbors.add(new MemberPos(input));
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
        final MemberPos me = new MemberPos(at);
        final List<MemberPos> oldNeighbors = ImmutableList.copyOf(neighbors);
        NORELEASE.println("Recalculate neighbors", at);

        if (!neighbors.isEmpty()) {
            // Unlink ourselves from the neighbor lists
            for (MemberPos npos : neighbors) {
                FlatFace ff = npos.get(at);
                if (ff.getSpecies() != SPECIES || !(ff instanceof WireLeader)) {
                    continue;
                }
                ((WireLeader) ff).neighbors.remove(me);
            }
            neighbors.clear();
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
                    if (!members.contains(e)) return null;
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
                    neighbors.add(npos);
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
                        neighbors.add(new MemberPos(at, side));
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
        // It's, like, O(1) vs O(N^2) or something. But is f(MAX_SIZE) anyways.
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

        // And complete the reverse-linkage
        for (MemberPos n : neighbors) {
            FlatFace ff = n.get(at.at);
            if (!(ff instanceof WireLeader)) continue;
            WireLeader neighbor = (WireLeader) ff;
            neighbor.neighbors.add(me);
        }
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
        neighbor.neighbors.clear();
        neighbor.members.clear();
        powerSum += neighbor.powerSum;
        npos.set(world, ChargeEnetSubsys.instance.wire0, FlatChunkLayer.FLAGS_SEAMLESS);
        return true;
    }

    public void scatter(Coord at, EnumFacing side) {
        // Not the most efficient operation, but probably the simplest way of implementing it.
        List<MemberPos> membersCopy = ImmutableList.copyOf(members);
        members.clear();
        for (MemberPos pos : membersCopy) {
            if (pos.get(at).getSpecies() != SPECIES) continue;
            WireLeader toSet = new WireLeader();
            Coord pat = pos.getCoord(at.w);
            EnumFacing pside = EnumFacing.getFront(pos.side);
            if (toSet.isValidAt(pat, pside)) {
                int flags = ~(FlatChunkLayer.FLAG_SYNC | FlatChunkLayer.FLAG_CALL_REPLACE);
                Flat.setWithNotification(pat, pside, toSet, (byte) flags);
            }
        }
        MemberPos myPos = new MemberPos(at, side);
        for (MemberPos npos : neighbors) {
            FlatFace nface = npos.get(at);
            if (nface.getSpecies() != SPECIES) continue;
            if (!(nface instanceof WireLeader)) continue;
            WireLeader neighbor = (WireLeader) nface;
            neighbor.neighbors.remove(myPos);
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
}
