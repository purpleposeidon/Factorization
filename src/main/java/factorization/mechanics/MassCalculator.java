package factorization.mechanics;

import factorization.api.Coord;
import factorization.api.ICoordFunction;
import factorization.fzds.interfaces.IDimensionSlice;
import factorization.util.NumUtil;
import factorization.util.SpaceUtil;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;

class MassCalculator implements ICoordFunction {
    private static final boolean ROUND_RESULTS = true;

    static double calculateMass(IDimensionSlice idc) {
        NBTTagCompound tag = idc.getTag();
        if (tag.hasKey(massKey)) return tag.getDouble(massKey);
        MassCalculator mc = new MassCalculator(idc);
        mc.calculate();
        return mc.massTotal;
    }

    static Vec3 getCenterOfMass(IDimensionSlice idc) {
        NBTTagCompound tag = idc.getTag();
        if (tag.hasKey(comKey + ".x")) {
            double x = tag.getDouble(comKey + ".x");
            double y = tag.getDouble(comKey + ".y");
            double z = tag.getDouble(comKey + ".z");
            return new Vec3(x, y, z);
        }
        MassCalculator mc = new MassCalculator(idc);
        return mc.com;
    }

    static Coord getComCoord(IDimensionSlice idc) {
        Vec3 com = SpaceUtil.floor(MassCalculator.getCenterOfMass(idc));
        Coord min = idc.getMinCorner();
        return new Coord(min.w, com.subtract(min.x, min.y, min.z));
    }

    static void dirty(IDimensionSlice idc) {
        final NBTTagCompound tag = idc.getTag();
        tag.removeTag(massKey);
        tag.removeTag(comKey + ".x");
        tag.removeTag(comKey + ".y");
        tag.removeTag(comKey + ".z");
    }

    protected static double round(double v) {
        if (!ROUND_RESULTS) return v;
        int exp = Math.getExponent(v);
        if (exp < 0) exp = 1;
        return Math.pow(2, exp);
    }

    private static final String massKey = "IdcMass";
    private static final String comKey = "IdcCOM";

    private double massTotal = 0;
    private Vec3 com = SpaceUtil.newVec();
    private final IDimensionSlice idc;
    protected final Coord min, max;

    protected MassCalculator(IDimensionSlice idc) {
        this.idc = idc;
        min = idc.getMinCorner();
        max = idc.getMaxCorner();
    }

    protected final void calculate() {
        Coord.iterateCube(min, max, this);
        save(idc.getTag());
    }

    protected void save(final NBTTagCompound tag) {
        tag.setDouble(massKey, round(massTotal));
        tag.setDouble(comKey + ".x", com.xCoord);
        tag.setDouble(comKey + ".y", com.yCoord);
        tag.setDouble(comKey + ".z", com.zCoord);
    }

    protected void handle(Coord here, double mass) {
        final AxisAlignedBB box = here.getCollisionBoundingBox();
        if (box != null) {
            // Unlikely, my good sir!
            Vec3 boxMid = SpaceUtil.getMiddle(box);
            double posRatio = mass / (mass + massTotal); // First iteration this will be 1, setting com = boxMid
            com = NumUtil.interp(com, boxMid, (float) posRatio);
        }
        massTotal += mass;
    }

    @Override
    public final void handle(Coord here) {
        final double hereMass = MassHelper.getBlockMass(here);
        if (hereMass <= 0) return;
        handle(here, hereMass);
    }
}
