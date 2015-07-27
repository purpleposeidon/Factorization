package factorization.beauty;

import cpw.mods.fml.common.SidedProxy;
import factorization.api.Coord;
import factorization.api.IRotationalEnergySource;
import factorization.util.SpaceUtil;
import ic2.api.energy.tile.IKineticSource;
import net.minecraft.block.Block;
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

    @SidedProxy(clientSide = "factorization.beauty.KineticProxy.Ic2ConverterImpl", serverSide = "factorization.beauty.KineticProxy.Ic2ConverterImpl", modId = "IC2")
    static Ic2ConverterProxy ic2Proxy = new Ic2ConverterProxy(); //Loader.isModLoaded("IC2") ?

    public static double IC2_FZ_RATIO = 1.0;
    public static double IC2_ANGULAR_VELOCITY_RATIO = 1.0 / 20.0;

    private static class Ic4Fz implements IRotationalEnergySource {
        private final IKineticSource base;
        private final TileEntity baseTe;
        private static boolean verified = false;

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
            return base.requestkineticenergy(direction, (int) (maxPower * IC2_FZ_RATIO)) / IC2_FZ_RATIO;
        }

        @Override
        public double getVelocity(ForgeDirection direction) {
            return base.maxrequestkineticenergyTick(direction) * IC2_ANGULAR_VELOCITY_RATIO;
        }

        @Override
        public boolean isTileEntityInvalid() {
            return baseTe.isInvalid();
        }
    }
}
