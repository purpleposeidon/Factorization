package factorization.api;

import java.util.ArrayList;
import java.util.HashSet;

import net.minecraft.src.NBTTagCompound;
import factorization.common.Core;

public class Charge {
    private int charge;

    public Charge() {
        this.charge = 0;
    }

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

    public void writeToNBT(NBTTagCompound tag, String name) {
        tag.setInteger(name, charge);
    }

    public void readFromNBT(NBTTagCompound tag, String name) {
        setValue(tag.getInteger(name));
    }

    public void swapWith(Charge other) {
        int a = this.charge, b = other.charge;
        this.setValue(b);
        other.setValue(a);
    }

    //These are some functions for users to make good & healthy use of
    /**
     * This function spreads charge around with a random conducting neighbor of src. Call it every tick.
     * 
     * @param te
     *            A conductive TileEntity
     */
    public static void update(IChargeConductor te) {
        Coord here = te.getCoord();
        if (!Core.instance.isCannonical(here.w)) {
            return;
        }
        Charge me = te.getCharge();

        if (here.parity()) {
            //In short lines, it's possible to swap and then swap back
            //Anyways, a neighbor'll swap with us.
            return;
        }

        for (Coord neighbor : here.getRandomNeighborsAdjacent()) {
            IChargeConductor n = neighbor.getTE(IChargeConductor.class);
            if (n == null) {
                continue;
            }
            //This is a fine place for an error if me == null
            me.swapWith(n.getCharge());
            return;
        }
    }

    private static ArrayList<IChargeConductor> frontier = new ArrayList(5 * 5 * 4);
    private static HashSet<IChargeConductor> visited = new HashSet(5 * 5 * 5);

    public static class ChargeDensityReading {
        public int totalCharge, conductorCount, maxCharge;
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
                if (neighborCoord.distanceManhatten(hereCoord) > maxDistance) {
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
        return ret;
    }

}
