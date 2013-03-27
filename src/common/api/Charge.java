package factorization.api;

import net.minecraft.nbt.NBTTagCompound;

public class Charge {
    ConductorSet conductorSet = null;
    IChargeConductor conductor = null;
    boolean isConductorSetLeader = false;
    boolean justCreated = true;
    
    public Charge(IChargeConductor conductor) {
        this.conductor = conductor;
    }
    
    public int getValue() {
        if (conductorSet == null || conductorSet.memberCount == 0) {
            return 0;
        }
        int chargeShare = conductorSet.totalCharge / conductorSet.memberCount;
        if (isConductorSetLeader) {
            chargeShare += conductorSet.totalCharge % conductorSet.memberCount;
        }
        return chargeShare;
    }

    public void setValue(int newCharge) {
        createOrJoinConductorSet();
        conductorSet.totalCharge += newCharge - getValue();
    }

    public int addValue(int chargeToAdd) {
        createOrJoinConductorSet();
        return conductorSet.totalCharge += chargeToAdd;
    }
    
    /**
     * Removes all the charge.
     * @return how much charge there was
     */
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
    
    public int tryTake(int toTake) {
        int c = getValue();
        if (c < toTake) {
            return 0;
        }
        setValue(c - toTake);
        return toTake;
    }

    public void writeToNBT(NBTTagCompound tag, String name) {
        tag.setInteger(name, getValue());
    }
    
    public void writeToNBT(NBTTagCompound tag) {
        writeToNBT(tag, "charge");
    }

    public void readFromNBT(NBTTagCompound tag, String name) {
        setValue(tag.getInteger(name));
    }
    
    public void readFromNBT(NBTTagCompound tag) {
        readFromNBT(tag, "charge");
    }
    
    void createOrJoinConductorSet() {
        if (conductorSet != null) {
            return;
        }
        Coord here = conductor.getCoord();
        if (here.w == null) {
            //BAH! BAH I say! Can't do this nicely because we don't have a world!
            new ConductorSet(conductor);
            return;
        }
        Iterable<IChargeConductor> neighbors = conductor.getCoord().getAdjacentTEs(IChargeConductor.class);
        for (IChargeConductor neighbor : neighbors) {
            Charge n = neighbor.getCharge();
            if (n.conductorSet == null) {
                continue;
            }
            if (n.conductorSet.addConductor(this.conductor)) {
                //we've got ourself added to a set. Inform the set of any adjacent sets.
                for (IChargeConductor otherNeighbor : neighbors) {
                    conductorSet.addNeighbor(otherNeighbor.getCharge().conductorSet);
                }
                return;
            }
        }
        new ConductorSet(conductor);
    }

    /*** 
     * Call this function every tick.
     */
    public void update() {
        Coord here = conductor.getCoord();
        if (here.w.isRemote) {
            return;
        }
        createOrJoinConductorSet();
        if (isConductorSetLeader) {
            conductorSet.update();
        }
        if (justCreated || (here.w.getWorldTime() + here.seed()) % 600 == 0) {
            justCreated = false;
            if (conductorSet.leader == null) {
                conductorSet.leader = conductor;
            }
            for (IChargeConductor neighbor : here.getAdjacentTEs(IChargeConductor.class)) {
                justCreated |= conductorSet.addNeighbor(neighbor.getCharge().conductorSet);
            }
        }
    }
    
    /**
     * Call when the IChargeConductor containing the charge is removed.
     */
    public void remove() {
        if (conductorSet == null) {
            return;
        }
        for (IChargeConductor hereConductor : conductorSet.getMembers(conductor)) {
            Charge hereCharge = hereConductor.getCharge();
            int saveCharge = hereCharge.getValue();
            new ConductorSet(hereCharge.conductor);
            hereCharge.setValue(saveCharge);
        }
    }
    
    public static class ChargeDensityReading {
        public int totalCharge, conductorCount;
    }
    /**
     * Gets the average charge in the nearby connected network
     * 
     * @param start
     *            where to measure from
     * @return
     */
    public static ChargeDensityReading getChargeDensity(IChargeConductor start) {
        ConductorSet cs = start.getCharge().conductorSet;
        if (cs == null) {
            return new ChargeDensityReading();
        }
        ChargeDensityReading ret = new ChargeDensityReading();
        ret.totalCharge = cs.totalCharge;
        ret.conductorCount = cs.memberCount;
        return ret;
    }

    /**
     * Call this function when the IChargeConductor is invalidated.
     */
    public void invalidate() {
        if (conductorSet == null) {
            return;
        }
        remove();
    }

}
