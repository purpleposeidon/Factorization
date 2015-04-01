package factorization.mechanisms;

import factorization.api.Coord;
import factorization.api.ICoordFunction;
import factorization.fzds.interfaces.IDeltaChunk;
import factorization.util.SpaceUtil;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Vec3;

class InertiaCalculator implements ICoordFunction {
    static double getInertia(IDeltaChunk idc, Vec3 axisOfRotation) {
        NBTTagCompound tag = idc.getEntityData();
        if (checkAxis(tag, axisOfRotation) && tag.hasKey(inertiaKey)) return tag.getDouble(inertiaKey);
        final InertiaCalculator inertiaCalculator = new InertiaCalculator(idc, axisOfRotation);
        double inertia = inertiaCalculator.calculate();
        double mass = inertiaCalculator.massSum;
        tag.setDouble(inertiaKey, inertia);
        MassCalculator.saveMass(idc, mass);
        return inertia;
    }

    static void dirty(IDeltaChunk idc) {
        final NBTTagCompound tag = idc.getEntityData();
        tag.removeTag(inertiaKey);
        tag.removeTag(MassCalculator.massKey);
    }

    private static boolean checkAxis(NBTTagCompound tag, Vec3 axis) {
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
    private final Coord min, max;
    private final Vec3 origin, axisOfRotation;
    private double sum = 0;
    private double massSum = 0;

    private InertiaCalculator(IDeltaChunk idc, Vec3 axisOfRotation) {
        min = idc.getCorner();
        max = idc.getFarCorner();
        Vec3 minVec = SpaceUtil.newVec();
        min.setAsVector(minVec);
        origin = SpaceUtil.incrAdd(minVec, idc.getRotationalCenterOffset());
        this.axisOfRotation = axisOfRotation;
    }

    private double calculate() {
        Coord.iterateCube(min, max, this);
        return sum;
    }

    private Vec3 here = SpaceUtil.newVec();
    @Override
    public void handle(Coord coordHere) {
        coordHere.setAsVector(here);
        SpaceUtil.incrSubtract(here, origin);
        double d = SpaceUtil.lineDistance(axisOfRotation, here);
        final double blockMass = MassHelper.getBlockMass(coordHere);
        sum += blockMass * d * d;
        massSum += blockMass;
    }


}
