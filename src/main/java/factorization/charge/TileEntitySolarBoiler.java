package factorization.charge;

import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.shared.*;
import factorization.util.DataUtil;
import factorization.util.FluidUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;
import factorization.api.Coord;
import factorization.api.IMeterInfo;
import factorization.api.IReflectionTarget;
import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.common.FzConfig;

import java.io.IOException;

public class TileEntitySolarBoiler extends TileEntityCommon implements IReflectionTarget, IFluidHandler, IMeterInfo {
    public static Fluid steam;
    public static FluidStack water_stack = null;
    public static FluidStack steam_stack = null;
    
    public static void setupSteam() {
        if (water_stack == null) {
            water_stack = new FluidStack(FluidRegistry.WATER, 0);
            steam_stack = FluidRegistry.getFluidStack("steam", 0);
            steam = steam_stack.getFluid();
        }
    }
    
    FluidTank waterTank = new FluidTank(/*this, */ water_stack.copy(), 1000 * 2);
    FluidTank steamTank = new FluidTank(/*this, */ steam_stack.copy(), 1000 * 1);
    int reflector_count = 0;
    public transient short given_heat = 0, last_synced_heat = 0;
    
    public TileEntitySolarBoiler() {
        waterTank.getFluid().amount = 0;
        steamTank.getFluid().amount = 0;
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.SOLARBOILER;
    }
    
    @Override
    public IIcon getIcon(ForgeDirection dir) {
        switch (dir) {
        case UP: return BlockIcons.boiler_top;
        default: return BlockIcons.boiler_side;
        }
    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.Machine;
    }

    @Override
    public void putData(DataHelper data) throws IOException {
        waterTank = data.as(Share.PRIVATE, "water").putTank(waterTank);
        steamTank = data.as(Share.PRIVATE, "steam").putTank(steamTank);
        if (data.isReader()) {
            sanitize();
        }
        given_heat = data.as(Share.VISIBLE_TRANSIENT, "givenHeat").putShort(given_heat);
    }
    
    private FluidTank getTank(ForgeDirection from) {
        if (from == ForgeDirection.UP) {
            return steamTank;
        }
        return waterTank;
    }
    
    @Override
    public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
        if (resource.isFluidEqual(water_stack)) {
            return waterTank.fill(resource, doFill);
        } else if (resource.isFluidEqual(steam_stack)) {
            return steamTank.fill(resource, doFill);
        } else {
            return 0;
        }
    }

    @Override
    public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
        return getTank(from).drain(maxDrain, doDrain);
    }
    
    @Override
    public boolean canDrain(ForgeDirection from, Fluid fluid) {
        return false;
    }
    
    @Override
    public boolean canFill(ForgeDirection from, Fluid fluid) {
        if (from == ForgeDirection.UP) {
            return false;
        }
        return fluid == null || fluid == water_stack.getFluid();
    }
    
    @Override
    public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {
        if (from != ForgeDirection.UP) return null; 
        FluidTank tank = getTank(from);
        if (resource == null || tank.getFluid() != resource) {
            return null;
        }
        return tank.drain(tank.getCapacity(), doDrain);
    }
    
    @Override
    public FluidTankInfo[] getTankInfo(ForgeDirection from) {
        return new FluidTankInfo[] {getTank(from).getInfo()};
    }

    //MAIN LOGIC
    @Override
    public void addReflector(int strength) {
        reflector_count = Math.max(0, reflector_count + strength);
    }
    
    int getWater() {
        return waterTank.getFluid().amount;
    }
    
    int getSteam() {
        return steamTank.getFluid().amount;
    }
    
    int getHeat() {
        return Math.max(reflector_count - 3, 0);
    }
    
    void sanitize() {
        if (waterTank.getFluid() == null) {
            waterTank.setFluid(water_stack.copy());
        }
        if (steamTank.getFluid() == null) {
            steamTank.setFluid(steam_stack.copy());
        }
    }

    IFluidHandler above = this;

    IFluidHandler getAbove() {
        if (above == this) {
            above = getCoord().add(0, 1, 0).getTE(IFluidHandler.class);
        }
        return above;
    }

    @Override
    public void onNeighborTileChanged(int tilex, int tiley, int tilez) {
        above = this;
    }

    @Override
    public void updateEntity() {
        if (worldObj.isRemote) {
            return;
        }
        sanitize();
        FluidStack water = waterTank.getFluid();
        FluidStack steam = steamTank.getFluid();
        Coord here = getCoord();
        long seed = here.seed() + worldObj.getTotalWorldTime();
        short measure_time = 5;
        if (seed % measure_time == 0) {
            short m = (short) (given_heat / measure_time);
            given_heat = 0;
            if (m != last_synced_heat) {
                last_synced_heat = m;
                broadcastMessage(null, NetworkFactorization.MessageType.BoilerHeat, last_synced_heat);
            }
        }
        IFluidHandler aboveTank = getAbove();
        if (aboveTank != null) {
            FluidStack sending_steam = steam.copy();
            sending_steam.amount = Math.min(sending_steam.amount, 1000);
            steam.amount -= aboveTank.fill(ForgeDirection.DOWN, steam.copy(), true);
            steam.amount = Math.max(0, steam.amount);
        }
        if (water.amount < 1000) {
            //pull water from below
            Coord below = here.add(0, -1, 0);
            IFluidHandler tc = below.getTE(IFluidHandler.class);
            boolean water_below = (below.is(Blocks.flowing_water) || below.is(Blocks.water));
            water_below &= !here.isPowered();
            if (water_below && FzConfig.boilers_suck_water) {
                if (below.getMd() == 0) {
                    below.setAir();
                    water.amount += 1000;
                    water.amount = Math.min(water.amount, waterTank.getCapacity());
                }
            } else if (tc != null) {
                ForgeDirection dir = ForgeDirection.UP;
                if (below.getTE(TileEntitySolarBoiler.class) != null) {
                    dir = ForgeDirection.DOWN;
                }
                int free = Math.max(0, waterTank.getCapacity() - water.amount);
                free = Math.min(1000/10, free);
                FluidStack avail = tc.drain(dir, free, false);
                if (avail != null && avail.isFluidEqual(water_stack)) {
                    water.amount += tc.drain(dir, free, true).amount;
                }
            }
            return;
        }
        
        //try boiling
        int time_scale = 1;
//		if (seed % time_scale != 0) {
//			return;
//		}
        if (getHeat() <= 0) {
            return; //nothing to heat
        }
        applyHeat(getHeat()*time_scale);
    }
    
    public void applyHeat(int heat) {
        heat *= 5;
        given_heat += heat;
        sanitize();
        FluidStack water = waterTank.getFluid();
        FluidStack steam = steamTank.getFluid();
        if (steam.amount >= steamTank.getCapacity()) {
            return; //no room for more steam
        }
        int toBoil = Math.min(heat, water.amount);
        toBoil = Math.min(steamTank.getCapacity() - steam.amount, toBoil);
        int water_to_steam = 160; /* CovertJaguar gives 1:160 as the water:steam ratio */;
        int water_to_remove = Math.max(toBoil/water_to_steam, 1);
        if (water_to_remove > water.amount) {
            return;
        }
        water.amount -= water_to_remove;
        steam.amount += (int)(toBoil*FzConfig.steam_output_adjust);
    }
    
    @Override
    protected void onRemove() {
        super.onRemove();
        Coord here = getCoord();
        FluidUtil.spill(here, waterTank.getFluid());
        FluidUtil.spill(here, steamTank.getFluid());
    }
    
    @Override
    public String getInfo() {
        sanitize();
        float w = waterTank.getFluid().amount / 1000F;
        float s = steamTank.getFluid().amount / 1000F;
        return "Power: " + reflector_count
                + "\nSteam: " + String.format("%.1f", s)
                + "\nWater: " + String.format("%.1f", w);
    }

    @Override
    public boolean handleMessageFromServer(NetworkFactorization.MessageType messageType, ByteBuf input) throws IOException {
        if (messageType == NetworkFactorization.MessageType.BoilerHeat) {
            given_heat = last_synced_heat = input.readShort();
            return true;
        }
        return super.handleMessageFromServer(messageType, input);
    }
}
