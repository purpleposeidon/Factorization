package factorization.common;

import java.io.DataInput;
import java.io.IOException;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.liquids.ILiquidTank;
import net.minecraftforge.liquids.ITankContainer;
import net.minecraftforge.liquids.LiquidDictionary;
import net.minecraftforge.liquids.LiquidStack;
import net.minecraftforge.liquids.LiquidTank;
import net.minecraftforge.liquids.LiquidDictionary.LiquidRegisterEvent;
import factorization.api.Charge;
import factorization.api.IChargeConductor;
import factorization.common.NetworkFactorization.MessageType;

public class TileEntitySteamTurbine extends TileEntityCommon implements ITankContainer, IChargeConductor {
    LiquidTank steamTank = new LiquidTank(TileEntitySolarBoiler.steam_stack.copy(), 1000*16, this);
    Charge charge = new Charge(this);
    int fan_speed = 0;
    public int fan_rotation = 0;
    
    @Override
    public FactoryType getFactoryType() {
        return FactoryType.STEAMTURBINE;
    }
    
    @SideOnly(Side.CLIENT)
    public static FzIcon turbine_top = tex("charge/turbinee_top"), turbine_side = tex("charge/turbine_side"), turbine_bottom = tex("charge/turbine_bottom");
    
    @Override
    Icon getIcon(ForgeDirection dir) {
        switch (dir) {
        case UP: return turbine_top;
        case DOWN: return turbine_bottom;
        default: return turbine_side;
        }
    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.Machine;
    }
    
    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        FactorizationUtil.writeTank(tag, steamTank, "steam");
        charge.writeToNBT(tag);
        tag.setInteger("fan", fan_speed);
    }
    
    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        FactorizationUtil.readTank(tag, steamTank, "steam");
        charge.readFromNBT(tag);
        fan_speed = tag.getInteger("fan");
    }

    @Override
    public int fill(ForgeDirection from, LiquidStack resource, boolean doFill) {
        return steamTank.fill(resource, doFill);
    }

    @Override
    public int fill(int tankIndex, LiquidStack resource, boolean doFill) {
        return steamTank.fill(resource, doFill);
    }

    @Override
    public LiquidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
        return steamTank.drain(maxDrain, doDrain);
    }

    @Override
    public LiquidStack drain(int tankIndex, int maxDrain, boolean doDrain) {
        return steamTank.drain(maxDrain, doDrain);
    }

    @Override
    public ILiquidTank[] getTanks(ForgeDirection direction) {
        if (direction == ForgeDirection.UP) {
            return new ILiquidTank[] {};
        }
        return new ILiquidTank[] {steamTank};
    }

    @Override
    public ILiquidTank getTank(ForgeDirection direction, LiquidStack type) {
        if (direction == ForgeDirection.UP) {
            return null;
        }
        return steamTank;
    }

    @Override
    public Charge getCharge() {
        return charge; //Does java need multiple inheritance? Naaaah.
    }

    @Override
    public String getInfo() {
        float s = steamTank.getLiquid().amount*16/(float)steamTank.getCapacity();
        return "Steam: " + String.format("%.1f", s)
                + "\nFan: " + fan_speed;
    }
    
    int last_speed = -9999;
    public void shareFanSpeed() {
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
        fan_rotation += Math.min(35, fan_speed);
        if (worldObj.isRemote) {
            return;
        }
        shareFanSpeed();
        charge.update();
        if (worldObj.getBlockPower(xCoord, yCoord, zCoord) > 0) {
            fan_speed = Math.max(0, fan_speed - 1);
            return;
        }
        LiquidStack steam = steamTank.getLiquid();
        if (steam == null) {
            steamTank.setLiquid(steam = LiquidDictionary.getLiquid("Steam", 0));
        }
        long seed = getCoord().seed() + worldObj.getWorldTime();
        if (seed % 5 == 0) {
            if (fan_speed > steam.amount) {
                fan_speed -= 10;
                steam.amount = 0;
            } else {
                if (steam.amount == fan_speed) {
                    steam.amount = 0;
                } else if (steam.amount > fan_speed) {
                    steam.amount -= fan_speed + 1;
                    fan_speed++;
                }
            }
        }
        if (fan_speed < 0) {
            fan_speed = 0;
            return;
        }
        if (3*charge.getValue() > fan_speed) {
            return;
        }
        charge.addValue(fan_speed);
    }
    
    @Override
    public boolean handleMessageFromServer(int messageType, DataInput input) throws IOException {
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
