package factorization.mechanisms;

import factorization.api.Coord;
import factorization.api.ICoordFunction;
import factorization.fzds.interfaces.IDeltaChunk;
import net.minecraft.nbt.NBTTagCompound;

class MassCalculator implements ICoordFunction {
    static double calculateMass(IDeltaChunk idc) {
        NBTTagCompound tag = idc.getEntityData();
        if (tag.hasKey(massKey)) return tag.getDouble(massKey);
        double mass = new MassCalculator(idc).calculate();
        saveMass(idc, mass);
        return mass;
    }

    static void saveMass(IDeltaChunk idc, double mass) {
        idc.getEntityData().setDouble(massKey, mass);
    }

    static void dirty(IDeltaChunk idc) {
        final NBTTagCompound tag = idc.getEntityData();
        tag.removeTag(MassCalculator.massKey);
    }

    static final String massKey = "IdcMass";

    private double sum = 0;
    private final IDeltaChunk idc;

    private MassCalculator(IDeltaChunk idc) {
        this.idc = idc;
    }

    private double calculate() {
        Coord min = idc.getCorner();
        Coord max = idc.getFarCorner();
        Coord.iterateCube(min, max, this);
        return sum;
    }

    @Override
    public void handle(Coord here) {
        sum += MassHelper.getBlockMass(here);
    }
}
