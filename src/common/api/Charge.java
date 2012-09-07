package factorization.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.World;
import net.minecraftforge.common.ForgeDirection;
import static net.minecraftforge.common.ForgeDirection.*;
import factorization.common.Core;
import factorization.common.TileEntityHeater;

public class Charge {
    private int charge = 0;
    private ForgeDirection motion = UNKNOWN;

    public int getValue() {
        return charge;
    }

    public void setValue(int newCharge) {
        this.charge = Math.max(0, newCharge);
    }

    public int addValue(int chargeToAdd) {
        setValue(charge + chargeToAdd);
        return charge;
    }
    
    public int deplete() {
        int ret = getValue();
        setValue(0);
        return ret;
    }
    
    public int deplete(int toTake) {
        int c = getValue();
        toTake = Math.min(toTake, c);
        setValue(c - toTake);
        return toTake;
    }

    public void writeToNBT(NBTTagCompound tag, String name) {
        tag.setInteger(name, charge);
        tag.setInteger(name + "_motion", motion.ordinal());
    }

    public void readFromNBT(NBTTagCompound tag, String name) {
        setValue(tag.getInteger(name));
        if (tag.hasKey(name + "_motion")) {
            motion = ForgeDirection.values()[tag.getInteger(name + "_motion")];
        } else {
            motion = UNKNOWN;
        }
    }

    public void swapWith(Charge other) {
        int a = this.charge, b = other.charge;
        this.setValue(b);
        other.setValue(a);
        ForgeDirection c = this.motion, d = other.motion;
        this.motion = d;
        other.motion = c;
    }

    //These are some functions for users to make good & healthy use of
    
    static ArrayList<ForgeDirection> realDirections = new ArrayList();
    static {
        for (ForgeDirection d : ForgeDirection.values()) {
            if (d != UNKNOWN) {
                realDirections.add(d);
            }
        }
    }
    /**
     * This function spreads charge around with a random conducting neighbor of src. Call it every tick.
     * 
     * @param te
     *            A conductive TileEntity
     */
    public static void update(IChargeConductor te) {
        Charge me = te.getCharge();
        //me.charge = 0;
        if (me.charge <= 0) {
            me.motion = UNKNOWN;
            return;
        }
        Coord here = te.getCoord();
        if (me.motion == UNKNOWN) {
            me.pickDirection(here, UNKNOWN);
            if (me.motion == UNKNOWN) {
                return;
            }
        }
        
        IChargeConductor moveTo = here.add(me.motion).getTE(IChargeConductor.class);
        if (moveTo == null) {
            me.pickDirection(here, me.motion.getOpposite());
            if (me.motion == UNKNOWN) {
                return;
            }
            moveTo = here.add(me.motion).getTE(IChargeConductor.class);
            if (moveTo == null) {
                return; //this'll never happen
            }
        }
        
        me.swapWith(moveTo.getCharge());
    }
    
    void pickDirection(Coord here, ForgeDirection avoid) {
        Collections.shuffle(realDirections);
        for (ForgeDirection direction : realDirections) {
            if (direction == avoid) {
                continue;
            }
            IChargeConductor conductor = here.add(direction).getTE(IChargeConductor.class);
            if (conductor != null) {
                motion = direction;
                return;
            }
        }
        if (avoid == UNKNOWN) {
            motion = UNKNOWN;
            return;
        }
        
        IChargeConductor conductor = here.add(avoid).getTE(IChargeConductor.class);
        if (conductor != null) {
            motion = avoid;
            return;
        }
    }
    
    private static ArrayList<IChargeConductor> frontier = new ArrayList(5 * 5 * 4);
    private static HashSet<IChargeConductor> visited = new HashSet(5 * 5 * 5);

    public static class ChargeDensityReading {
        public int totalCharge, conductorCount, maxCharge;
        public ForgeDirection motion;
    }
    /**
     * Gets the average charge in the nearby connected network
     * 
     * @param start
     *            where to measure from
     * @param maxDistance
     *            Only checks this range. Manhatten distance.
     * @return
     */
    public static ChargeDensityReading getChargeDensity(IChargeConductor start, int maxDistance) {
        int totalCharge = 0, maxCharge = 0;
        Coord startCoord = start.getCoord();
        frontier.clear();
        visited.clear();
        frontier.add(start);
        visited.add(start);
        while (frontier.size() > 0) {
            IChargeConductor here = frontier.remove(0);
            Coord hereCoord = here.getCoord();
            int hereCharge = here.getCharge().charge;
            totalCharge += hereCharge;
            maxCharge = Math.max(maxCharge, hereCharge);
            for (Coord neighborCoord : hereCoord.getNeighborsAdjacent()) {
                IChargeConductor neighbor = neighborCoord.getTE(IChargeConductor.class);
                if (neighbor == null) {
                    continue;
                }
                if (visited.contains(neighbor)) {
                    continue;
                }
                if (neighborCoord.distanceManhatten(startCoord) > maxDistance) {
                    continue;
                }
                frontier.add(neighbor);
                visited.add(neighbor);
            }
        }
        ChargeDensityReading ret = new ChargeDensityReading();
        ret.totalCharge = totalCharge;
        ret.conductorCount = visited.size();
        ret.maxCharge = maxCharge;
        ret.motion = start.getCharge().motion;
        return ret;
    }

}
