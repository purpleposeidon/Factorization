package factorization.charge.enet;

import com.google.common.base.Function;
import factorization.api.Coord;
import factorization.flat.AbstractFlatWire;
import factorization.flat.api.Flat;
import factorization.flat.api.FlatCoord;
import factorization.flat.api.FlatFace;
import factorization.shared.Core;
import factorization.util.NORELEASE;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

public class FlatFaceWire extends AbstractFlatWire {
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
        WireLeader.probe(this, at, side, SEARCH_SIZE, new Function<FlatCoord, Boolean>() {
            @Override
            public Boolean apply(FlatCoord input) {
                FlatFace face = input.get();
                if (face instanceof WireLeader) {
                    WireLeader leader = (WireLeader) face;
                    if (leader.members.contains(me)) {
                        leader.scatter(input.at, input.side);
                        return true;
                    }
                }
                return false;
            }
        });
    }
}
