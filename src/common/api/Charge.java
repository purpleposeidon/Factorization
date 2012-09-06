package factorization.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;

import org.omg.CORBA.UNKNOWN;

import net.minecraft.client.Minecraft;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.World;
import net.minecraftforge.common.ForgeDirection;
import factorization.common.Core;

public class Charge {
    private int charge;
    private int beaconAge;
    private ForgeDirection beacon = ForgeDirection.UNKNOWN;
    static final int maxBeaconAge = 20*20; // 20 seconds

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
        if (getBeacon() != ForgeDirection.UNKNOWN) {
            tag.setInteger(name + "_beacon", beacon.ordinal());
            tag.setInteger(name + "_beaconAge", beaconAge);
        }
    }

    public void readFromNBT(NBTTagCompound tag, String name) {
        setValue(tag.getInteger(name));
        if (tag.hasKey(name + "_beacon")) {
            beacon = ForgeDirection.values()[tag.getInteger(name + "beacon")];
            beaconAge = tag.getInteger(name + "_beaconAge");
        } else {
            beacon = ForgeDirection.UNKNOWN;
        }
    }

    public void swapWith(Charge other) {
        int a = this.charge, b = other.charge;
        this.setValue(b);
        other.setValue(a);
    }

    //These are some functions for users to make good & healthy use of
    static List<Coord> toMark = Collections.synchronizedList(new ArrayList<Coord>());
    /**
     * This function spreads charge around with a random conducting neighbor of src. Call it every tick.
     * 
     * @param te
     *            A conductive TileEntity
     */
    public static void update(IChargeConductor te) {
        Coord here = te.getCoord();
        if (here.remote()) {
            //XXX TODO: oh my god, pull this out before release
            World w = te.getCoord().w;
            if (w.getWorldTime() % 40 != 0) {
                toMark.clear();
                return;
            }
            while (toMark.size() > 0) {
                Coord tm = toMark.remove(0);
                tm.setWorld(w);
                tm.mark();
            }
            return;
        }

        Charge me = te.getCharge();
        int thisIsZero = me.charge == 0 ? 1 : 0;
        if (me.charge > 0) {
            toMark.add(te.getCoord());
        }
        
        me.beaconAge++;
        ForgeDirection beacon = me.getBeacon();
        if (beacon != ForgeDirection.UNKNOWN) {
            if (me.beaconAge == 0) {
                me.shareBeacon(te);
            }
            IChargeConductor n = here.add(beacon).getTE(IChargeConductor.class);
            if (n != null) {
                Charge nc = n.getCharge();
                if (me.charge > nc.charge) {
                    //Excellent! We can move charge towards a sink.
                    me.swapWith(nc);
                    return;
                }
            }
        }
        
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
            Charge nCharge = n.getCharge();
            int neighborIsZero = nCharge.charge == 0 ? 1 : 0;
            if (thisIsZero + neighborIsZero == 1) {
                me.swapWith(n.getCharge());
                return;
            }
        }
    }
    
    private ForgeDirection getBeacon() {
        if (beaconAge > maxBeaconAge) {
            return ForgeDirection.UNKNOWN;
        }
        return beacon;
    }
    
    private void takeBeacon(ForgeDirection direction) {
        if (getBeacon() == direction) {
            //refresh
            beaconAge = -1;
        } else if (getBeacon() != ForgeDirection.UNKNOWN) {
            //conflict
            beacon = ForgeDirection.UNKNOWN;
        } else {
            beaconAge = -1;
            beacon = direction;
        }
    }
    
    public void shareBeacon(IChargeConductor me) {
        Coord here = me.getCoord();
        ForgeDirection myBeacon = getBeacon();
        for (ForgeDirection d : ForgeDirection.values()) {
            if (d == ForgeDirection.UNKNOWN || d == myBeacon) {
                continue;
            }
            IChargeConductor neighbor = here.add(d).getTE(IChargeConductor.class);
            if (neighbor == null) {
                continue;
            }
            neighbor.getCharge().takeBeacon(d.getOpposite());
        }
    }

    private static ArrayList<IChargeConductor> frontier = new ArrayList(5 * 5 * 4);
    private static HashSet<IChargeConductor> visited = new HashSet(5 * 5 * 5);

    public static class ChargeDensityReading {
        public int totalCharge, conductorCount, maxCharge;
        public ForgeDirection beacon;
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
        ret.beacon = start.getCharge().getBeacon();
        return ret;
    }

}
