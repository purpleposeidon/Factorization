package factorization.beauty;

import cpw.mods.fml.common.Loader;
import factorization.api.IRotationalEnergySource;
import factorization.common.FzConfig;
import ic2.api.energy.tile.IKineticSource;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

public class KineticProxy {
    public static IRotationalEnergySource cast(TileEntity te) {
        if (te == null || te.isInvalid()) return null;
        if (te instanceof IRotationalEnergySource) return (IRotationalEnergySource) te;
        return ic2Proxy.convert(te);
    }

    private static class Ic2ConverterProxy {
        IRotationalEnergySource convert(TileEntity te) {
            return null;
        }
    }

    private static class Ic2ConverterImpl extends Ic2ConverterProxy {
        IRotationalEnergySource convert(TileEntity te) {
            if (!(te instanceof IKineticSource)) return null;
            Ic4Fz ret = new Ic4Fz((IKineticSource) te);
            ret.verify();
            return ret;
        }
    }

    public static boolean ic2compatEnabled = FzConfig.ic2_kinetic_compat && Loader.isModLoaded("IC2");
    static Ic2ConverterProxy ic2Proxy = ic2compatEnabled ? new Ic2ConverterImpl() : new Ic2ConverterProxy();

    public static double IC2_FZ_RATIO = 5000;
    public static double IC2_ANGULAR_VELOCITY_RATIO = 1.0 / 1000.0;

    private static class Ic4Fz implements IRotationalEnergySource {
        private final IKineticSource base;
        private final TileEntity baseTe;
        private static boolean verified = false;

        private int last_velocity = 0;

        private Ic4Fz(IKineticSource base) {
            this.base = base;
            this.baseTe = (TileEntity) base;
        }

        private void verify() {
            if (verified) return;
            base.maxrequestkineticenergyTick(ForgeDirection.UP);
            base.requestkineticenergy(ForgeDirection.UP, 0);
            verified = true;
        }

        @Override
        public boolean canConnect(ForgeDirection direction) {
            return true;
        }

        @Override
        public double availableEnergy(ForgeDirection direction) {
            return base.maxrequestkineticenergyTick(direction) / IC2_FZ_RATIO;
        }

        @Override
        public double takeEnergy(ForgeDirection direction, double maxPower) {
            final int avail = base.requestkineticenergy(direction, (int) (maxPower * IC2_FZ_RATIO));
            last_velocity = avail;
            return avail / IC2_FZ_RATIO;
        }

        @Override
        public double getVelocity(ForgeDirection direction) {
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
