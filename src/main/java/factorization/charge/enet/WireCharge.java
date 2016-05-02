package factorization.charge.enet;

import com.google.common.base.Function;
import factorization.api.Coord;
import factorization.flat.AbstractFlatWire;
import factorization.flat.api.Flat;
import factorization.flat.api.FlatCoord;
import factorization.flat.api.FlatFace;
import factorization.notify.Notice;
import factorization.notify.NoticeUpdater;
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
    public static transient int SPECIES = Flat.nextSpeciesId();

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
        if (at.w.isRemote) return;
        if (Core.dev_environ) {
            FlatFace newVal = Flat.get(at, side);
            if (newVal == this) {
                NORELEASE.println("I'm not gone", at, side);
                return;
            }
        }
        ChargeEnetSubsys.instance.dirtyCache(at.w, new MemberPos(at, side));
        if (this instanceof WireLeader) {
            ((WireLeader) this).scatter(at, side);
            return;
        }
        new LeaderSearch(this, at, side) {
            @Override
            boolean onFound(WireLeader leader, FlatCoord input) {
                leader.scatter(input.at, input.side);
                return true;
            }
        }.search();
        // This should be more efficient than it looks. We only have to search for the leader once.
        // This method does not get invoked when the other members are replaced with leaders.
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
        class Noticer extends LeaderSearch {
            boolean found = false;
            Noticer() {
                super(WireCharge.this, at, side);
            }

            @Override
            boolean onFound(WireLeader leader, final FlatCoord input) {
                if (Core.dev_environ) {
                    new Notice(input.at, "Leader").sendTo(player);
                }
                new Notice(at, new NoticeUpdater() {
                    @Override
                    public void update(Notice msg) {
                        int memberCount = leader.members.size();
                        int powerSum = leader.powerSum;
                        int neighborCount = leader.neighbors.size();
                        if (memberCount == 0) {
                            if (Core.dev_environ) {
                                msg.setMessage("<removed>");
                                return;
                            }
                            FlatFace replacement = input.get();
                            if (replacement.getSpecies() == SPECIES) {
                                replacement.onActivate(at, side, player);
                                msg.cancel();
                            }
                            return;
                        }

                        int wirePower = powerSum / memberCount;
                        if (powerSum % memberCount > 0) {
                            wirePower++;
                        }

                        if (!Core.dev_environ) {
                            msg.setMessage("factorization:charge/flat/measure", "" + wirePower);
                            return;
                        }

                        msg.setMessage("Power: %s\n\nMembers: %s\nPowerSum: %s\nNeighbors: %s\n",
                                "" + wirePower,
                                "" + memberCount,
                                "" + powerSum,
                                "" + neighborCount);
                    }
                }).sendTo(player);
                found = true;
                return true;
            }
        };
        Noticer noticer = new Noticer();
        noticer.search();
        if (!noticer.found) {
            new Notice(at, "No leader!? Bug!\nReplace these wires.").sendTo(player);
        }
    }

    public static abstract class LeaderSearch implements Function<FlatCoord, Boolean> {
        private final MemberPos mePos;
        private final Coord at;
        private final EnumFacing side;
        private final WireCharge me;

        public LeaderSearch(WireCharge me, Coord at, EnumFacing side) {
            this.me = me;
            this.mePos = new MemberPos(at, side);
            this.at = at;
            this.side = side;
        }

        public void search() {
            WireLeader.probe(me, at, side, SEARCH_SIZE, this);
        }

        @Override
        public Boolean apply(FlatCoord input) {
            FlatFace face = input.get();
            if (face instanceof WireLeader) {
                WireLeader leader = (WireLeader) face;
                if (leader.members.contains(mePos)) {
                    return onFound(leader, input);
                }
            }
            return false;
        }

        abstract boolean onFound(WireLeader leader, FlatCoord input);
    }
}
