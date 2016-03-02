package factorization.charge;

import factorization.api.Coord;
import factorization.flat.api.Flat;
import factorization.flat.api.FlatFace;
import factorization.flat.api.IFlatModel;
import factorization.flat.api.IModelMaker;
import factorization.shared.Core;
import factorization.util.NORELEASE;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

import static net.minecraft.util.EnumFacing.*;

public class FlatFaceWire extends FlatFace {
    /* DOWN UP NORTH SOUTH WEST EAST */
    private static EnumFacing[][] edgesOfDirection = new EnumFacing[][]{
            {NORTH, EAST, SOUTH, WEST},
            {NORTH, EAST, SOUTH, WEST},
            {UP, WEST, DOWN, EAST},
            {UP, EAST, DOWN, WEST},
            {UP, SOUTH, DOWN, NORTH},
            {UP, NORTH, DOWN, SOUTH}
            /* Actually only half of these can ever be used; getModel()'s caused only w/ positive faces */
    };

    public static EnumFacing getEdgeOfFace(EnumFacing dir, int hour) {
        return edgesOfDirection[dir.ordinal()][hour];
    }



    public final byte powerLevel;
    IFlatModel[] permutations;

    public FlatFaceWire(int powerLevel) {
        this.powerLevel = (byte) powerLevel;
    }

    @Nullable
    @Override
    public IFlatModel getModel(Coord at, EnumFacing side) {
        if (NORELEASE.on) return permutations[0];
        // Construct a 4-bit number representing the connections. This number then indexes into the permutations array.
        int connections = 0; // Look at keyboard numpad. First it stores (8???) (68??) (268?) (4268)
        for (int hour = 0; hour < 4; hour++) {
            EnumFacing hand = getEdgeOfFace(side, hour);
            if (hasAnyWire(at, side, hand)) {
                connections |= 0x8;
            }
            connections >>= 1;
        }
        return permutations[connections];
    }

    public static final int SPECIES = Flat.nextSpeciesId();

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
        permutations = new IFlatModel[0x10];
        for (int i = 0x0; i < permutations.length; i++) {
            permutations[i] = maker.getModel(new ResourceLocation("factorization:flat/wire/m" + bin(i)));
        }
    }

    private static String bin(int i) {
        return Integer.toString(0x10 + i, 2).substring(1);
    }

    @Override
    public void onNeighborBlockChanged(Coord at, EnumFacing side) {
        if (at.w.isRemote) return;
        if (at.isSolidOnSide(side)) {
            return;
        }
        Coord op = at.add(side);
        if (op.isSolidOnSide(side.getOpposite()) || !op.blockExists()) {
            return;
        }
        Flat.setAir(at, side);
        at.spawnItem(Core.registry.wirePlacer);
    }
}
