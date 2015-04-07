package factorization.mechanics;

import factorization.api.Coord;
import factorization.api.ICoordFunction;
import factorization.fzds.interfaces.IDeltaChunk;
import factorization.util.NumUtil;
import factorization.util.SpaceUtil;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Vec3;

class MassCalculator implements ICoordFunction {
    static double calculateMass(IDeltaChunk idc) {
        NBTTagCompound tag = idc.getEntityData();
        if (tag.hasKey(massKey)) return tag.getDouble(massKey);
        MassCalculator mc = new MassCalculator(idc);
        mc.calculate();
        return mc.massTotal;
    }

    static Vec3 getCenterOfMass(IDeltaChunk idc) {
        NBTTagCompound tag = idc.getEntityData();
        if (tag.hasKey(comKey + ".x")) {
            double x = tag.getDouble(comKey + ".x");
            double y = tag.getDouble(comKey + ".y");
            double z = tag.getDouble(comKey + ".z");
            return Vec3.createVectorHelper(x, y, z);
        }
        MassCalculator mc = new MassCalculator(idc);
        return mc.com;
    }

    static Coord getComCoord(IDeltaChunk idc) {
        Vec3 com = SpaceUtil.floor(MassCalculator.getCenterOfMass(idc));
        Coord min = idc.getCorner();
        com.xCoord -= min.x;
        com.yCoord -= min.y;
        com.zCoord -= min.z;
        return new Coord(idc.getCorner().w, com);
    }

    static void dirty(IDeltaChunk idc) {
        final NBTTagCompound tag = idc.getEntityData();
        tag.removeTag(massKey);
        tag.removeTag(comKey + ".x");
        tag.removeTag(comKey + ".y");
        tag.removeTag(comKey + ".z");
    }

    private static final String massKey = "IdcMass";
    private static final String comKey = "IdcCOM";

    private double massTotal = 0;
    private Vec3 com = SpaceUtil.newVec();
    private final IDeltaChunk idc;
    protected final Coord min, max;

    protected MassCalculator(IDeltaChunk idc) {
        this.idc = idc;
        min = idc.getCorner();
        max = idc.getFarCorner();
    }

    protected final void calculate() {
        Coord.iterateCube(min, max, this);
        save(idc.getEntityData());
    }

    protected void save(final NBTTagCompound tag) {
        tag.setDouble(massKey, massTotal);
        tag.setDouble(comKey + ".x", com.xCoord);
        tag.setDouble(comKey + ".y", com.yCoord);
        tag.setDouble(comKey + ".z", com.zCoord);
    }

    private Vec3 boxMid = SpaceUtil.newVec();
    protected void handle(Coord here, double mass) {
        SpaceUtil.setMiddle(here.getCollisionBoundingBoxFromPool(), boxMid);
        double posRatio = mass / (mass + massTotal); // First iteration this will be 1, setting com = boxMid
        NumUtil.interp(com, boxMid, (float) posRatio, com);
        massTotal += mass;
    }

    @Override
    public final void handle(Coord here) {
        final double hereMass = MassHelper.getBlockMass(here);
        if (hereMass <= 0) return;
        handle(here, hereMass);
    }
}
