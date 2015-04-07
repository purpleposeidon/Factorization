package factorization.mechanisms;

import factorization.api.Coord;
import factorization.api.ICoordFunction;
import factorization.fzds.interfaces.IDeltaChunk;
import factorization.util.SpaceUtil;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Vec3;

class InertiaCalculator extends MassCalculator implements ICoordFunction {
    static double getInertia(IDeltaChunk idc, Vec3 axisOfRotation) {
        NBTTagCompound tag = idc.getEntityData();
        if (axisNotUpdated(tag, axisOfRotation) && tag.hasKey(inertiaKey)) return tag.getDouble(inertiaKey);
        final InertiaCalculator inertiaCalculator = new InertiaCalculator(idc, axisOfRotation);
        inertiaCalculator.calculate();
        return inertiaCalculator.inertiaSum;
    }

    static void dirty(IDeltaChunk idc) {
        final NBTTagCompound tag = idc.getEntityData();
        tag.removeTag(inertiaKey);
        MassCalculator.dirty(idc);
    }

    private static boolean axisNotUpdated(NBTTagCompound tag, Vec3 axis) {
        double lastX = tag.getDouble(axisKey + ".x");
        double lastY = tag.getDouble(axisKey + ".y");
        double lastZ = tag.getDouble(axisKey + ".z");
        if (lastX == axis.xCoord && lastY == axis.yCoord && lastZ == axis.zCoord) {
            return true;
        }
        tag.setDouble(axisKey + ".x", axis.xCoord);
        tag.setDouble(axisKey + ".y", axis.yCoord);
        tag.setDouble(axisKey + ".z", axis.zCoord);
        return false;
    }

    private static final String inertiaKey = "IdcInertia";
    private static final String axisKey = "IdcInertiaAxis";
    private final Vec3 origin, axisOfRotation;
    private double inertiaSum = 0;

    protected InertiaCalculator(IDeltaChunk idc, Vec3 axisOfRotation) {
        super(idc);
        Vec3 minVec = SpaceUtil.newVec();
        min.setAsVector(minVec);
        origin = SpaceUtil.incrAdd(minVec, idc.getRotationalCenterOffset());
        this.axisOfRotation = axisOfRotation;
    }

    private Vec3 here = SpaceUtil.newVec();

    @Override
    public void handle(Coord coordHere, double blockMass) {
        super.handle(coordHere, blockMass);
        coordHere.setAsVector(here);
        SpaceUtil.incrSubtract(here, origin);
        double d = SpaceUtil.lineDistance(axisOfRotation, here);
        inertiaSum += blockMass * d * d;
    }

    @Override
    protected void save(NBTTagCompound tag) {
        super.save(tag);
        tag.setDouble(inertiaKey, inertiaSum);
    }
}
