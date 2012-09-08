package factorization.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.World;
import net.minecraftforge.common.ForgeDirection;
import static net.minecraftforge.common.ForgeDirection.*;
import factorization.common.Core;
import factorization.common.TileEntityHeater;

public class Charge {
    private int charge = 0;
    private ForgeDirection last_motion = DOWN;
    private static Random rand = new Random();

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
        tag.setInteger(name + "_dir", last_motion.ordinal());
    }

    public void readFromNBT(NBTTagCompound tag, String name) {
        setValue(tag.getInteger(name));
        if (tag.hasKey(name + "_dir")) {
            last_motion = ForgeDirection.values()[tag.getInteger(name + "_dir")];
        } else {
            last_motion = DOWN;
        }
    }

    public void swapWith(Charge other) {
        int a = this.charge, b = other.charge;
        this.setValue(b);
        other.setValue(a);
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
            return;
        }
        Coord here = te.getCoord();
        if (rand.nextInt(20) == 5) {
            //jostle randomly
            for (Coord neighbor : here.getRandomNeighborsAdjacent()) {
                IChargeConductor cond = neighbor.getTE(IChargeConductor.class);
                if (cond == null) {
                    continue;
                }
                me.swapWith(cond.getCharge());
                return;
            }
            return;
        }
        ForgeDirection opposite = me.last_motion.getOpposite();
        for (int i = me.last_motion.ordinal() + 1; i < UNKNOWN.ordinal(); i++) {
            ForgeDirection dir = ForgeDirection.values()[i];
            if (dir == opposite) {
                continue;
            }
            if (me.tryPush(here, dir)) {
                me.last_motion = dir;
                return;
            }
        }
        for (int i = 0; i <= me.last_motion.ordinal(); i++) {
            ForgeDirection dir = ForgeDirection.values()[i];
            if (dir == opposite) {
                continue;
            }
            if (me.tryPush(here, dir)) {
                me.last_motion = dir;
                return;
            }
        }
        if (me.tryPush(here, opposite)) {
            me.last_motion = opposite;
        }
    }
    
    boolean tryPush(Coord here, ForgeDirection d) {
        IChargeConductor con = here.add(d).getTE(IChargeConductor.class);
        if (con == null) {
            return false;
        }
        Charge neighborCharge = con.getCharge();
        if (neighborCharge.charge != 0) {
            return false;
        }
        neighborCharge.charge = charge;
        neighborCharge.last_motion = d;
        charge = 0;
        return true;
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
        ret.motion = start.getCharge().last_motion;
        return ret;
    }

}
