package factorization.flat;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import factorization.algos.FastBag;
import factorization.api.Coord;
import factorization.flat.api.*;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;

import static net.minecraft.util.EnumFacing.*;

public abstract class AbstractFlatWire extends FlatFace {
    private static final EnumFacing[][] edgesOfDirection = new EnumFacing[][]{
            {NORTH, EAST, SOUTH, WEST}, // DOWN
            {NORTH, EAST, SOUTH, WEST}, // UP
            {UP, WEST, DOWN, EAST}, // NORTH
            {UP, EAST, DOWN, WEST}, // SOUTH
            {UP, SOUTH, DOWN, NORTH}, // WEST
            {UP, NORTH, DOWN, SOUTH} // EAST
            /* Actually only half of these can ever be used; we pretty much use only positive faces */
    };
    protected WireModelGroup models;

    public static EnumFacing getEdgeOfFace(EnumFacing dir, int hour) {
        return edgesOfDirection[dir.ordinal()][hour];
    }

    protected static String bin(int i) {
        return Integer.toString(0x10 + i, 2).substring(1);
    }

    @Nullable
    @Override
    public IFlatModel getModel(Coord at, EnumFacing side) {
        // Construct a 4-bit number representing the connections. This number then indexes into the permutations array.
        int connections = connectionInfo(at, side);
        if (models == null) return null;
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
    public abstract int getSpecies();

    protected boolean isWire(Coord at, EnumFacing side) {
        return getSpecies() == Flat.get(at, side).getSpecies();
    }

    protected boolean hasAnyWire(Coord at, EnumFacing side, EnumFacing hand) {
        if (isWire(at, hand)) return true; // Wrap around to wire on same block
        if (isWire(at.add(hand), side)) return true; // Connect to wire on hand-wards block
        if (isWire(at.add(side), hand)) return true; // Cross over to block in front of this wire (opposite of 1st case)
        return false;
    }

    protected abstract String getModelGroupName();

    @Override
    public void loadModels(IModelMaker maker) {
        models = WireModelGroup.get(getModelGroupName(), maker);
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

    public static class WireModelGroup {
        static HashMap<String, AbstractFlatWire.WireModelGroup> instances = Maps.newHashMap();
        IFlatModel[] permutations;

        public static WireModelGroup get(String name, IModelMaker maker) {
            WireModelGroup ret = instances.get(name);
            if (ret != null) return ret;
            ret = new WireModelGroup();
            instances.put(name, ret);
            ret.permutations = new IFlatModel[0x10];
            for (int i = 0x0; i < ret.permutations.length; i++) {
                ret.permutations[i] = maker.getModel(new ResourceLocation(name + bin(i)));
            }
            return ret;
        }
    }

    public static void probe(final AbstractFlatWire seedFace, Coord at, EnumFacing side, final int max_dist, Function<FlatCoord, Boolean> visitor) {
        final FlatCoord seed = new FlatCoord(at, side);
        final HashSet<FlatCoord> visited = new HashSet<FlatCoord>();
        final FastBag<FlatCoord> frontier = new FastBag<FlatCoord>();
        visited.add(seed);
        frontier.add(seed);
        Function<FlatCoord, Void> add = new Function<FlatCoord, Void>() {
            @Override
            public Void apply(FlatCoord input) {
                if (seed.at.distanceManhatten(input.at) > max_dist) return null;
                if (!seedFace.isWire(input.at, input.side)) return null;
                if (visited.add(input)) {
                    frontier.add(input);
                }
                return null;
            }
        };
        while (!frontier.isEmpty()) {
            FlatCoord here = frontier.removeAny();
            if (visitor.apply(here) == Boolean.TRUE) return;
            iterateConnectable(here, add);
        }
    }

    public static void iterateConnectable(FlatCoord root, Function<FlatCoord, Void> iter) {
        Coord oppositeBlock = root.at.add(root.side);
        for (int hour = 0; hour < 4; hour++) {
            EnumFacing hand = getEdgeOfFace(root.side, hour);
            {
                FlatCoord cozySide = root.add(hand);
                iter.apply(cozySide);
            }
            {
                FlatCoord backAndSide = root.atFace(hand);
                iter.apply(backAndSide);
            }
            {
                FlatCoord frontAndSide = new FlatCoord(oppositeBlock, hand);
                iter.apply(frontAndSide);
            }
        }
    }
}
