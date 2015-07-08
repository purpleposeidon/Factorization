package factorization.beauty;

import cpw.mods.fml.common.Loader;
import factorization.api.Coord;
import factorization.api.IShaftPowerSource;
import factorization.util.SpaceUtil;
import ic2.api.energy.tile.IKineticSource;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

public class KineticProxy {
    public static IShaftPowerSource cast(TileEntity te) {
        if (te instanceof IShaftPowerSource) return (IShaftPowerSource) te;
        return castIc2(te);
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
            if (!(at.getBlock() instanceof BlockShaft)) {
                // NOTE: This is not the case on the first iteration
                IShaftPowerSource powerSource = cast(at.getTE());
                if (powerSource == null) return null;
                TileEntityShaftUpdater updater = new TileEntityShaftUpdater(powerSource, dir.getOpposite());
                start.setTE(updater);
                return updater;
            }
            ForgeDirection shaftDir = BlockShaft.meta2direction[at.getMd()];
            if (shaftDir != expectShaftDir) return null;
            at.adjust(dir);
            // There could be a TileEntityShaftUpdater underneath the Shaft. It is ignored, as it ought to only be at the end.
            // We aren't checking blockExists!
        }
    }

    static boolean ic2_kinetic_source = Loader.isModLoaded("IC2");
    private static IShaftPowerSource castIc2(TileEntity te) {
        if (!ic2_kinetic_source) return null;
        try {
            return castIc20(te);
        } catch (Throwable t) {
            t.printStackTrace();
            ic2_kinetic_source = false;
            return null;
        }
    }

    private static IShaftPowerSource castIc20(final TileEntity te) {
        if (!(te instanceof IKineticSource)) return null;
        Ic4Fz ret = new Ic4Fz((IKineticSource) te);
        ret.verify();
        return ret;
    }

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
        public double deplete(ForgeDirection direction, double maxPower) {
            return base.requestkineticenergy(direction, (int) (maxPower * IC2_FZ_RATIO)) / IC2_FZ_RATIO;
        }

        @Override
        public double getAngularSpeed(ForgeDirection direction) {
            return base.maxrequestkineticenergyTick(direction) * IC2_ANGULAR_VELOCITY_RATIO;
        }
    }
}
