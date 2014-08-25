package factorization.charge;

import java.io.DataInput;
import java.io.IOException;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;
import factorization.api.Charge;
import factorization.api.IChargeConductor;
import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.shared.BlockClass;
import factorization.shared.FzUtil;
import factorization.shared.TileEntityCommon;
import factorization.shared.NetworkFactorization.MessageType;

public class TileEntitySteamTurbine extends TileEntityCommon implements IFluidHandler, IChargeConductor {
    FluidTank steamTank = new FluidTank(/*this,*/ TileEntitySolarBoiler.steam_stack.copy(), 1000*16);
    Charge charge = new Charge(this);
    int fan_speed = 0;
    public int fan_rotation = 0;
    
    @Override
    public FactoryType getFactoryType() {
        return FactoryType.STEAMTURBINE;
    }
    
    @Override
    public IIcon getIcon(ForgeDirection dir) {
        switch (dir) {
        case UP: return BlockIcons.turbine_top;
        case DOWN: return BlockIcons.turbine_bottom;
        default: return BlockIcons.turbine_side;
        }
    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.Machine;
    }
    
    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        FzUtil.writeTank(tag, steamTank, "steam");
        charge.writeToNBT(tag);
        tag.setInteger("fan", fan_speed);
    }
    
    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        FzUtil.readTank(tag, steamTank, "steam");
        charge.readFromNBT(tag);
        fan_speed = tag.getInteger("fan");
    }

    @Override
    public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
        return steamTank.fill(resource, doFill);
    }
    
    @Override
    public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {
        return null;
    }
    
    @Override
    public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
        return steamTank.drain(maxDrain, doDrain);
    }
    
    @Override
    public boolean canFill(ForgeDirection from, Fluid fluid) {
        if (from == ForgeDirection.UP) {
            return false;
        }
        return fluid == null || fluid.getID() == TileEntitySolarBoiler.steam.getID();
    }
    
    @Override
    public boolean canDrain(ForgeDirection from, Fluid fluid) {
        return false;
    }
    
    @Override
    public FluidTankInfo[] getTankInfo(ForgeDirection from) {
        return new FluidTankInfo[] {steamTank.getInfo()};
    }

    @Override
    public Charge getCharge() {
        return charge; //Does java need multiple inheritance? Naaaah.
    }

    @Override
    public String getInfo() {
        float s = steamTank.getFluid().amount*16/(float)steamTank.getCapacity();
        return "Steam: " + String.format("%.1f", s)
                + "\nFan: " + fan_speed;
    }
    
    int last_speed = -9999;
    public void shareFanSpeed() {
        if (last_speed == fan_speed) return;
        boolean share = false;
        if (last_speed + fan_speed < 2) {
            share = true;
        } else if (fan_speed > 0) {
            share = Math.abs(last_speed/(double)fan_speed) > 0.05;
        } else if (fan_speed == 0 && last_speed != 0) {
            share = true;
        }
        if (share) {
            last_speed = fan_speed;
            broadcastMessage(null, MessageType.TurbineSpeed, fan_speed);
        }
    }

    @Override
    public void updateEntity() {
        fan_rotation += Math.min(35, fan_speed/5);
        if (worldObj.isRemote) {
            return;
        }
        shareFanSpeed();
        charge.update();
        if (worldObj.getBlockPowerInput(xCoord, yCoord, zCoord) > 0) {
            fan_speed = Math.max(0, fan_speed - 1);
            return;
        }
        FluidStack steam = steamTank.getFluid();
        if (steam == null) {
            steamTank.setFluid(steam = FluidRegistry.getFluidStack("steam", 0));
        }
        long seed = getCoord().seed() + worldObj.getTotalWorldTime();
        if (seed % 5 == 0) {
            if (fan_speed > steam.amount) {
                fan_speed = (int) Math.max(fan_speed * 0.8 - 1, 0);
                steam.amount = 0;
            } else {
                fan_speed += Math.log(steam.amount);
                steam.amount -= fan_speed;
            }
        }
        if (fan_speed <= 0) {
            fan_speed = 0;
            return;
        }
        if (3*charge.getValue() > fan_speed) {
            return;
        }
        charge.setValue(fan_speed);
    }
    
    @Override
    public boolean handleMessageFromServer(MessageType messageType, DataInput input) throws IOException {
        if (super.handleMessageFromServer(messageType, input)) {
            return true;
        }
        if (messageType == MessageType.TurbineSpeed) {
            fan_speed = input.readInt();
            return true;
        }
        return false;
    }
}
