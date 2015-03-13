package factorization.mechanisms;

import factorization.api.Coord;
import factorization.api.ICoordFunction;
import factorization.fzds.interfaces.IDeltaChunk;
import factorization.util.SpaceUtil;
import net.minecraft.block.material.Material;
import net.minecraft.util.Vec3;

public class InertiaCalculator implements ICoordFunction {
    private final Coord min, max;
    private final Vec3 origin, axisOfRotation;
    private double sum = 0;

    public InertiaCalculator(IDeltaChunk idc, Vec3 axisOfRotation) {
        min = idc.getCorner();
        max = idc.getFarCorner();
        Vec3 minVec = SpaceUtil.newVec();
        min.setAsVector(minVec);
        origin = SpaceUtil.incrAdd(minVec, idc.getRotationalCenterOffset());
        this.axisOfRotation = axisOfRotation;
    }

    public double calculate() {
        Coord.iterateCube(min, max, this);
        return sum;
    }

    private Vec3 here = SpaceUtil.newVec();
    @Override
    public void handle(Coord coordHere) {
        coordHere.setAsVector(here);
        SpaceUtil.incrSubtract(here, origin);
        double d = SpaceUtil.lineDistance(axisOfRotation, here);
        sum += getBlockMass(coordHere) * d * d;
    }

    public double getBlockMass(Coord at) {
        Material mat = at.getBlock().getMaterial();
        if (mat == Material.air) {
            return 0.0;
        }
        if (mat == Material.cactus || mat == Material.leaves || mat == Material.plants || mat == Material.vine) {
            return 0.25;
        }
        if (mat == Material.wood) {
            return 0.5;
        }
        if (mat == Material.iron || mat == Material.anvil) {
            return 7;
        }
        if (mat == Material.cloth || mat == Material.carpet || mat == Material.web) {
            return 0.1;
        }
        if (mat == Material.water || mat == Material.gourd) {
            return 1.0;
        }
        if (mat == Material.snow || mat == Material.ice) {
            return 0.8;
        }
        return 2.0;
    }
}
