package factorization.common;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.liquids.ILiquidTank;
import net.minecraftforge.liquids.ITankContainer;
import net.minecraftforge.liquids.LiquidDictionary;
import net.minecraftforge.liquids.LiquidStack;
import net.minecraftforge.liquids.LiquidTank;
import factorization.api.Coord;
import factorization.api.IMeterInfo;
import factorization.api.IReflectionTarget;

public class TileEntitySolarBoiler extends TileEntityCommon implements IReflectionTarget, ITankContainer, IMeterInfo {
    public static LiquidStack water_stack = null;
    public static LiquidStack steam_stack = null;
    
    public static void setupSteam() {
        if (water_stack == null) {
            water_stack = LiquidDictionary.getOrCreateLiquid("Water", new LiquidStack(Block.waterStill, 0));
            steam_stack = LiquidDictionary.getOrCreateLiquid("Steam", new LiquidStack(Core.registry.fz_steam, 0));
        }
    }
    
    LiquidTank waterTank = new LiquidTank(water_stack.copy(), 1000*8, this);
    LiquidTank steamTank = new LiquidTank(steam_stack.copy(), 1000*8, this);
    int reflector_count = 0;
    
    public TileEntitySolarBoiler() {
        waterTank.setTankPressure(0);
        steamTank.setTankPressure(1);
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.SOLARBOILER;
    }
    
    @Override
    public Icon getIcon(ForgeDirection dir) {
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
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        FactorizationUtil.writeTank(tag, waterTank, "water");
        FactorizationUtil.writeTank(tag, steamTank, "steam");
    }
    
    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        FactorizationUtil.readTank(tag, waterTank, "water");
        FactorizationUtil.readTank(tag, steamTank, "steam");
        sanitize();
    }
    
    private LiquidTank getTank(ForgeDirection from) {
        if (from == ForgeDirection.UP) {
            return steamTank;
        }
        return waterTank;
    }

    @Override
    public int fill(ForgeDirection from, LiquidStack resource, boolean doFill) {
        return getTank(from, resource).fill(resource, doFill);
    }

    @Override
    public int fill(int tankIndex, LiquidStack resource, boolean doFill) {
        return waterTank.fill(resource, doFill);
    }

    @Override
    public LiquidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
        return getTank(from).drain(maxDrain, doDrain);
    }

    @Override
    public LiquidStack drain(int tankIndex, int maxDrain, boolean doDrain) {
        return steamTank.drain(maxDrain, doDrain);
    }

    @Override
    public ILiquidTank[] getTanks(ForgeDirection direction) {
        return new ILiquidTank[] { getTank(direction) };
    }

    @Override
    public ILiquidTank getTank(ForgeDirection direction, LiquidStack type) {
        if (direction == ForgeDirection.UP) {
            return steamTank;
        }
        if (water_stack.isLiquidEqual(type)) {
            return waterTank;
        }
        if (steam_stack.isLiquidEqual(type)) {
            return steamTank;
        }
        return getTank(direction);
    }

    //MAIN LOGIC
    @Override
    public void addReflector(int strength) {
        reflector_count = Math.max(0, reflector_count + strength);
    }
    
    int getWater() {
        return waterTank.getLiquid().amount;
    }
    
    int getSteam() {
        return steamTank.getLiquid().amount;
    }
    
    int getHeat() {
        return reflector_count;
    }
    
    void sanitize() {
        LiquidStack water = waterTank.getLiquid();
        LiquidStack steam = steamTank.getLiquid();
        if (water == null) {
            water = LiquidDictionary.getLiquid("Water", 0);
            waterTank.setLiquid(water);
        }
        if (steam == null) {
            steam = LiquidDictionary.getLiquid("Steam", 0);
            steamTank.setLiquid(steam);
        }
        if (water == null || steam == null) {
            throw new RuntimeException("Steam/Water not defined");
        }
    }
    
    @Override
    public void updateEntity() {
        if (worldObj.isRemote) {
            return;
        }
        sanitize();
        LiquidStack water = waterTank.getLiquid();
        LiquidStack steam = steamTank.getLiquid();
        Coord here = getCoord();
        long seed = here.seed() + worldObj.getWorldTime();
        if (steam.amount > 0 && seed % 5 == 0) {
            //Send steam upwards
            Coord above = here.add(0, 1, 0);
            ITankContainer tc = above.getTE(ITankContainer.class);
            if (tc != null) {
                LiquidStack sending_steam = steam.copy();
                sending_steam.amount = Math.min(sending_steam.amount, Math.max(1, getHeat()/10));
                steam.amount -= tc.fill(ForgeDirection.DOWN, steam.copy(), true);
                steam.amount = Math.max(0, steam.amount);
            }
        }
        boolean random = seed % 40 == 0;
        if (water.amount <= 0 || (random && water.amount < waterTank.getCapacity())) {
            //pull water from below
            Coord below = here.add(0, -1, 0);
            ITankContainer tc = below.getTE(ITankContainer.class);
            boolean water_below = (below.is(Block.waterMoving) || below.is(Block.waterStill));
            water_below &= !here.isPowered();
            if (water_below && Core.boilers_suck_water) {
                if (below.getMd() == 0) {
                    below.setId(0);
                    water.amount += 1000;
                }
            } else if (tc != null) {
                ForgeDirection dir = ForgeDirection.UP;
                if (below.getTE(TileEntitySolarBoiler.class) != null) {
                    dir = ForgeDirection.DOWN;
                }
                int free = Math.max(0, waterTank.getCapacity() - water.amount);
                free = Math.min(1000/10, free);
                LiquidStack avail = tc.drain(dir, free, false);
                if (avail != null && avail.isLiquidEqual(water_stack)) {
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
        if (steam.amount >= steamTank.getCapacity()) {
            return; //no room for more steam
        }
        int toBoil = Math.min(getHeat()*time_scale, water.amount);
        toBoil = Math.min(steamTank.getCapacity() - steam.amount, toBoil);
        int water_to_steam = 160; /* CovertJaguar gives 1:160 as the water:steam ratio */;
        water.amount -= Math.max(toBoil/water_to_steam, 1);
        steam.amount += (int)(toBoil*Core.steam_output_adjust);
    }
    
    @Override
    void onRemove() {
        super.onRemove();
        Coord here = getCoord();
        FactorizationUtil.spill(here, waterTank.getLiquid());
        FactorizationUtil.spill(here, steamTank.getLiquid());
    }
    
    @Override
    public String getInfo() {
        sanitize();
        float w = waterTank.getLiquid().amount*16/(float)waterTank.getCapacity();
        float s = steamTank.getLiquid().amount*16/(float)steamTank.getCapacity();
        return "Power: " + reflector_count
                + "\nSteam: " + String.format("%.1f", s)
                + "\nWater: " + String.format("%.1f", w);
    }
}
