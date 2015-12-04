package factorization.compat.ic2;

import factorization.api.IRotationalEnergySource;
import factorization.api.adapter.Adapter;
import ic2.api.energy.tile.IKineticSource;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

public class RotationalEnergySourceAdapter implements Adapter<TileEntity, IRotationalEnergySource> {
    @Override
    public IRotationalEnergySource adapt(TileEntity val) {
        return new Kinetic2Rotational(val, (IKineticSource) val);
    }

    @Override
    public boolean canAdapt(Class<?> valClass) {
        return IKineticSource.class.isAssignableFrom(valClass);
    }

    @Override
    public int priority() {
        return 0;
    }

    private static class Kinetic2Rotational implements IRotationalEnergySource {
        public static double IC2_FZ_RATIO = 1500;
        public static double IC2_ANGULAR_VELOCITY_RATIO = 1.0 / 1000.0;

        private final TileEntity baseTe;
        private final IKineticSource base;
        private int last_velocity = 0;

        private Kinetic2Rotational(TileEntity baseTe, IKineticSource base) {
            this.baseTe = baseTe;
            this.base = base;
        }

        @Override
        public boolean canConnect(EnumFacing direction) {
            return true;
        }

        @Override
        public double availableEnergy(EnumFacing direction) {
            return base.maxrequestkineticenergyTick(direction) / IC2_FZ_RATIO;
        }

        @Override
        public double takeEnergy(EnumFacing direction, double maxPower) {
            final int avail = base.requestkineticenergy(direction, (int) (maxPower * IC2_FZ_RATIO));
            last_velocity = avail;
            return avail / IC2_FZ_RATIO;
        }

        @Override
        public double getVelocity(EnumFacing direction) {
            double v = last_velocity * IC2_ANGULAR_VELOCITY_RATIO;
            if (v > MAX_SPEED) v = MAX_SPEED;
            if (v < -MAX_SPEED) v = -MAX_SPEED;
            return v;
        }

        @Override
        public boolean isTileEntityInvalid() {
            return baseTe.isInvalid();
        }
    }
}
