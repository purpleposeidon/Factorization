package factorization.charge;

import com.google.common.collect.Maps;
import factorization.api.Coord;
import factorization.flat.api.Flat;
import factorization.flat.api.FlatFace;
import factorization.flat.api.IFlatModel;
import factorization.flat.api.IModelMaker;
import factorization.notify.Notice;
import factorization.notify.Style;
import factorization.shared.Core;
import factorization.util.FzUtil;
import factorization.util.PlayerUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.HashMap;

import static net.minecraft.util.EnumFacing.*;

public class FlatFaceWire extends FlatFace {
    private static final EnumFacing[][] edgesOfDirection = new EnumFacing[][]{
            {NORTH, EAST, SOUTH, WEST}, // DOWN
            {NORTH, EAST, SOUTH, WEST}, // UP
            {UP, WEST, DOWN, EAST}, // NORTH
            {UP, EAST, DOWN, WEST}, // SOUTH
            {UP, SOUTH, DOWN, NORTH}, // WEST
            {UP, NORTH, DOWN, SOUTH} // EAST
            /* Actually only half of these can ever be used; we pretty much use only positive faces */
    };

    public static EnumFacing getEdgeOfFace(EnumFacing dir, int hour) {
        return edgesOfDirection[dir.ordinal()][hour];
    }



    public final byte powerLevel;
    public static class WireModelGroup {
        static HashMap<String, WireModelGroup> instances = Maps.newHashMap();
        IFlatModel[] permutations;

        public static WireModelGroup get(String name, IModelMaker maker) {
            WireModelGroup ret = instances.get(name);
            if (ret != null) return ret;
            ret = new WireModelGroup();
            instances.put(name, ret);
            ret.permutations = new IFlatModel[0x10];
            for (int i = 0x0; i < ret.permutations.length; i++) {
                ret.permutations[i] = maker.getModel(new ResourceLocation("factorization:flat/" + name + bin(i)));
            }
            return ret;
        }
    }

    public FlatFaceWire(int powerLevel) {
        this.powerLevel = (byte) powerLevel;
    }

    protected WireModelGroup models;

    @Nullable
    @Override
    public IFlatModel getModel(Coord at, EnumFacing side) {
        // Construct a 4-bit number representing the connections. This number then indexes into the permutations array.
        int connections = connectionInfo(at, side);
        return models.permutations[connections];
    }

    protected int connectionInfo(Coord at, EnumFacing side) {
        int connections = 0; // Look at keyboard numpad. First it stores (8???) (68??) (268?) (4268)
        for (int hour = 0; hour < 4; hour++) {
            connections >>= 1;
            EnumFacing hand = getEdgeOfFace(side, hour);
            if (hasAnyWire(at, side, hand)) {
                connections |= 0x8;
            }
        }
        return connections;
    }

    @Override
    public void onActivate(Coord at, EnumFacing side, EntityPlayer player) {
        if (Core.dev_environ) {
            if (at.w.isRemote) return;
            if (player.getHeldItem() == null) {
                new Notice(at, bin(connectionInfo(at, side))).withStyle(Style.FORCE).sendTo(player);
            }
        }
    }

    /** If two FlatFaces have the same species, then the wire'll try to connect. If you're subclassing you may want to
     * change the species so things correct appropriately. */
    protected static transient int SPECIES = Flat.nextSpeciesId();

    @Override
    public int getSpecies() {
        return SPECIES;
    }

    boolean isWire(Coord at, EnumFacing side) {
        return getSpecies() == Flat.get(at, side).getSpecies();
    }

    boolean hasAnyWire(Coord at, EnumFacing side, EnumFacing hand) {
        if (isWire(at, hand)) return true; // Wrap around to wire on same block
        if (isWire(at.add(hand), side)) return true; // Connect to wire on hand-wards block
        if (isWire(at.add(side), hand)) return true; // Cross over to block in front of this wire (opposite of 1st case)
        return false;
    }

    @Override
    public void loadModels(IModelMaker maker) {
        models = WireModelGroup.get("wire/m", maker);
    }

    private static String bin(int i) {
        return Integer.toString(0x10 + i, 2).substring(1);
    }

    @Override
    public boolean isValidAt(Coord at, EnumFacing side) {
        if (at.isSolidOnSide(side)) {
            return true;
        }
        Coord op = at.add(side);
        if (op.isSolidOnSide(side.getOpposite()) || !op.blockExists()) {
            return true;
        }
        return false;
    }

    public ItemStack getItem(Coord at, EnumFacing side) {
        return new ItemStack(Core.registry.wirePlacer);
    }

    public void dropItem(Coord at, EnumFacing side) {
        if (at.w.isRemote) return;
        if (FzUtil.doTileDrops(at.w)) {
            if (at.isSolid()) {
                Coord n = at.add(side);
                if (!n.isSolid()) {
                    at = n;
                }
            }
            at.spawnItem(getItem(at, side));
        }
    }

    @Override
    public void onNeighborBlockChanged(Coord at, EnumFacing side) {
        if (at.w.isRemote) return;
        if (isValidAt(at, side)) return;
        dropItem(at, side);
        Flat.setAir(at, side);
    }

    @Override
    public void onHit(final Coord at, EnumFacing side, EntityPlayer player) {
        if (at.w.isRemote) return;
        if (!PlayerUtil.isPlayerCreative(player)) {
            dropItem(at, side);
        }
        Flat.setAir(at, side);
        Flat.playSound(at, side, this);
        Flat.emitParticle(at, side, this);
    }
}
