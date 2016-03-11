package factorization.charge.enet;

import com.google.common.base.Function;
import factorization.api.Coord;
import factorization.flat.AbstractFlatWire;
import factorization.flat.api.Flat;
import factorization.flat.api.FlatCoord;
import factorization.flat.api.FlatFace;
import factorization.notify.Notice;
import factorization.notify.Style;
import factorization.shared.Core;
import factorization.util.ItemUtil;
import factorization.util.NORELEASE;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

public class WireCharge extends AbstractFlatWire {
    @Override
    protected String getModelGroupName() {
        return "factorization:flat/wire/m";
    }

    /**
     * If two FlatFaces have the same species, then the wire'll try to connect. If you're subclassing you may want to
     * change the species so things correct appropriately.
     */
    protected static transient int SPECIES = Flat.nextSpeciesId();

    @Override
    public int getSpecies() {
        return SPECIES;
    }

    public ItemStack getItem(Coord at, EnumFacing side) {
        return new ItemStack(Core.registry.wirePlacer);
    }

    public static final int MAX_SIZE = 24;
    public static final int SEARCH_SIZE = 26;

    @Override
    public void onReplaced(Coord at, EnumFacing side) {
        if (Core.dev_environ) {
            FlatFace newVal = Flat.get(at, side);
            if (newVal == this) {
                NORELEASE.println("I'm not gone");
                return;
            } else {
                NORELEASE.println("I've been replaced properly!", newVal);
            }
        }
        final MemberPos me = new MemberPos(at, side);
        new LeaderSearch(at, side) {
            @Override
            boolean onFound(WireLeader leader, FlatCoord input) {
                leader.scatter(input.at, input.side);
                return true;
            }
        };
    }

    boolean isMeter(ItemStack is) {
        // OreDictionary?
        return ItemUtil.is(is, Core.registry.charge_meter);
    }

    @Override
    public boolean canInteract(Coord at, EnumFacing side, EntityPlayer player) {
        if (isMeter(player.getHeldItem())) {
            return true;
        }
        return super.canInteract(at, side, player);
    }

    @Override
    public void onActivate(final Coord at, final EnumFacing side, final EntityPlayer player) {
        if (!isMeter(player.getHeldItem())) return;
        new LeaderSearch(at, side) {
            @Override
            boolean onFound(WireLeader leader, FlatCoord input) {
                int memberCount = leader.members.size();
                int powerSum = leader.powerSum;
                int neighborCount = leader.neighbors.size();
                new Notice(input.at, String.format("Members: %s\nPower: %s\nNeighbors: %s\n", memberCount, powerSum, neighborCount)).sendTo(player);
                return true;
            }
        };
    }

    protected abstract class LeaderSearch implements Function<FlatCoord, Boolean> {
        private final MemberPos me;

        public LeaderSearch(Coord at, EnumFacing side) {
            this.me = new MemberPos(at, side);
            WireLeader.probe(WireCharge.this, at, side, SEARCH_SIZE, this);
        }

        @Override
        public Boolean apply(FlatCoord input) {
            FlatFace face = input.get();
            if (face instanceof WireLeader) {
                WireLeader leader = (WireLeader) face;
                if (leader.members.contains(me)) {
                    return onFound(leader, input);
                }
            }
            return false;
        }

        abstract boolean onFound(WireLeader leader, FlatCoord input);
    }
}
