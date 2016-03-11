package factorization.charge.enet;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;
import factorization.flat.FlatChunkLayer;
import factorization.flat.api.Flat;
import factorization.flat.api.FlatCoord;
import factorization.flat.api.FlatFace;
import factorization.shared.Core;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import java.io.IOException;
import java.util.*;

public class WireLeader extends FlatFaceWire {
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
    boolean disturbed = true;

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

    static boolean working = false;
    void tick(FlatCoord at) {
        if (members.isEmpty()) {
            Core.logSevere("I'm not in my own network! " + at);
            return;
        }
        boolean anyEating = false;
        World w = at.at.w;
        Random rng = w.rand;
        ChunkCoordIntPair chunkPos = at.at.getChunk().getChunkCoordIntPair();
        working = true;
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
        working = false;
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
