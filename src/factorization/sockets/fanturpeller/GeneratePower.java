package factorization.sockets.fanturpeller;

import java.io.IOException;

import net.minecraft.block.Block;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.fluids.BlockFluidFinite;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.common.FactoryType;
import factorization.sockets.ISocketHolder;

public class GeneratePower extends BufferedFanturpeller {
    @Override
    public FactoryType getFactoryType() {
        return FactoryType.SOCKET_POWERGEN;
    }
    
    FluidStack handled;
    
    float getPowerMultiplier(FluidStack fs) {
        return 0.3F;
    }
    
    @Override
    protected void fanturpellerUpdate(ISocketHolder socket, Coord coord, boolean powered, boolean neighbor_changed) {
        if (worldObj.isRemote) return;
        if ((coord.seed() + worldObj.getTotalWorldTime()) % 20 != 0) {
            return;
        }
        if (!unclog(coord)) {
            fanω *= 0.8F;
            return;
        }
        if (3*charge.getValue() <= fanω) {
            charge.addValue((int)fanω);
        }
        int n = 10;
        FluidStack in = buffer.getFluid();
        fanω = (buffer.getFluidAmount()*getPowerMultiplier(in) + fanω*n)/(n+1);
        if (in == null) return;
        if (in.isFluidEqual(handled)) {
            handled.amount += in.amount;
        } else {
            handled = in.copy();
        }
        buffer.setFluid(null);
    }
    
    boolean isClogged() {
        if (handled == null) return false;
        final Fluid fluid = handled.getFluid();
        return handled.amount > BUCKET*10 && fluid.canBePlacedInWorld();
    }
    
    boolean unclog(Coord coord) {
        if (!isClogged()) return true;
        final Fluid fluid = handled.getFluid();
        Coord at = coord.add(facing);
        final int fid = fluid.getBlockID();
        if (!at.isReplacable()) {
            return true;
        }
        Fluid otherFluid = at.getFluid();
        if (otherFluid != null && otherFluid != fluid) {
            return true;
        }
        final Block block = Block.blocksList[fid];
        if (otherFluid == fluid) {
            if (block instanceof BlockFluidFinite) {
                if (at.getMd() == 0xF) return true;
            } else if (at.getMd() == 0) return true;
        }
        at.setIdMd(fid, block instanceof BlockFluidFinite ? 0xF : 0, true);
        handled = null;
        return false;
    }
    
    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        super.serialize(prefix, data);
        if (data.isNBT()) {
            if (data.isReader()) {
                handled = FluidStack.loadFluidStackFromNBT(data.getTag());
            } else if (handled != null) {
                handled.writeToNBT(data.getTag());
            }
        }
        return this;
    }
    
    @Override
    protected boolean shouldFeedJuice() {
        return false;
    }
    
    @Override
    public boolean canFill(ForgeDirection from, Fluid fluid) {
        if (super.canFill(from, fluid)) {
            if (facing != ForgeDirection.DOWN && !fluid.isGaseous()) {
                return false;
            }
            return fluid.getTemperature() > 333 && fluid.getDensity() < 500;
        }
        return false;
    }
    
    @Override
    protected float scaleRotation(float rotation) {
        return rotation*0.5F;
    }
    
    @Override
    public String getInfo() {
        String text = "Fan Speed: " + (int)fanω + "\nBuffer: " + buffer.getFluidAmount() + "mB";
        if (isClogged()) {
            text += "\nClogged";
        }
        return text;
    }
}
