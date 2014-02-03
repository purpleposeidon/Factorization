package factorization.sockets.fanturpeller;

import java.io.IOException;

import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

public abstract class BufferedFanturpeller extends SocketFanturpeller implements IFluidHandler {

    protected static final int BUCKET = FluidContainerRegistry.BUCKET_VOLUME;
    protected FluidTank buffer = new FluidTank(BUCKET);
    private static FluidTankInfo[] no_info = new FluidTankInfo[0];

    @Override
    public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
        if (from != facing.getOpposite()) return 0;
        return buffer.fill(resource, doFill);
    }

    @Override
    public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {
        return null;
    }

    @Override
    public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
        return null;
    }

    @Override
    public boolean canFill(ForgeDirection from, Fluid fluid) {
        return from == facing.getOpposite();
    }

    @Override
    public boolean canDrain(ForgeDirection from, Fluid fluid) {
        return false;
    }

    private FluidTankInfo[] tank_info = new FluidTankInfo[] { new FluidTankInfo(buffer) };

    public BufferedFanturpeller() {
        super();
    }

    @Override
    public FluidTankInfo[] getTankInfo(ForgeDirection from) {
        if (from == facing.getOpposite()) return tank_info;
        return no_info;
    }
    
    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        super.serialize(prefix, data);
        if (data.isNBT()) { // Lame, but only just this once.
            if (data.isReader()) {
                buffer.readFromNBT(data.getTag());
            } else {
                buffer.writeToNBT(data.getTag());
            }
        }
        return this;
    }
}