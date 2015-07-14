package factorization.beauty;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.SidedProxy;
import factorization.api.Coord;
import factorization.api.IShaftPowerSource;
import factorization.util.SpaceUtil;
import ic2.api.energy.tile.IKineticSource;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

public class KineticProxy {
    public static IShaftPowerSource cast(TileEntity te) {
        if (te instanceof IShaftPowerSource) return (IShaftPowerSource) te;
        return ic2Proxy.convert(te);
    }

    /**
     * This method can be a little expensive; only call it after block updates.
     */
    public static IShaftPowerSource connect(Coord at, ForgeDirection dir) {
        at.adjust(dir);
        if (!(at.getBlock() instanceof BlockShaft)) return cast(at.getTE());
        IShaftPowerSource end = at.getTE(IShaftPowerSource.class);
        if (end != null) return end;
        ForgeDirection expectShaftDir = SpaceUtil.sign(dir) == -1 ? dir.getOpposite() : dir;
        Coord start = at.copy();
        while (true) {
            Block atBlock = at.getBlock();
            if (!(atBlock instanceof BlockShaft)) {
                // NOTE: This is not the case on the first iteration
                IShaftPowerSource powerSource = cast(at.getTE());
                if (powerSource == null) return null;
                TileEntityShaftUpdater updater = new TileEntityShaftUpdater(powerSource, dir.getOpposite());
                start.setTE(updater);
                return updater;
            }
            ForgeDirection shaftDir = ((BlockShaft) atBlock).axis;
            if (shaftDir != expectShaftDir) return null;
            at.adjust(dir);
            // There could be a TileEntityShaftUpdater underneath the Shaft. It is ignored, as it ought to only be at the end.
            // We aren't checking blockExists!
        }
    }

    private static class Ic2ConverterProxy {
        IShaftPowerSource convert(TileEntity te) {
            return null;
        }
    }

    private static class Ic2ConverterImpl {
        IShaftPowerSource convert(TileEntity te) {
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

    private static class Ic4Fz implements IShaftPowerSource {
        private final IKineticSource base;
        private static boolean verified = false;

        private Ic4Fz(IKineticSource base) {
            this.base = base;
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
        public double availablePower(ForgeDirection direction) {
            return base.maxrequestkineticenergyTick(direction) / IC2_FZ_RATIO;
        }

        @Override
        public double powerConsumed(ForgeDirection direction, double maxPower) {
            return base.requestkineticenergy(direction, (int) (maxPower * IC2_FZ_RATIO)) / IC2_FZ_RATIO;
        }

        @Override
        public double getAngularVelocity(ForgeDirection direction) {
            return base.maxrequestkineticenergyTick(direction) * IC2_ANGULAR_VELOCITY_RATIO;
        }
    }
}
