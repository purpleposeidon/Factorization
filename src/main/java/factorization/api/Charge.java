package factorization.api;

import java.io.IOException;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;

public class Charge implements IDataSerializable {
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

    public void raiseValue(int toAdd) {
        if (toAdd < getValue()) return;
        setValue(toAdd);
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
        int val = tag.getInteger(name);
        setValue(val < 0 ? 0 : val);
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
            ConductorSet assignedConductorSet = new ConductorSet(conductor);
            return;
        }
        Iterable<Coord> neighbors = here.getNeighborsAdjacent();
        for (Coord n : neighbors) {
            IChargeConductor neighbor = n.getTE(IChargeConductor.class);
            if (neighbor == null) {
                continue;
            }
            Charge neighbor_charge = neighbor.getCharge();
            if (neighbor_charge.conductorSet == null) {
                continue;
            }
            if (neighbor_charge.conductorSet.addConductor(this.conductor)) {
                //we've got ourself added to a set. Inform the set of any adjacent sets.
                for (Coord coord_otherNeighbor : neighbors) {
                    IChargeConductor otherNeighbor = coord_otherNeighbor.getTE(IChargeConductor.class);
                    if (otherNeighbor != null) {
                        conductorSet.addNeighbor(otherNeighbor.getCharge().conductorSet);
                    }
                }
                return;
            }
        }
        ConductorSet assignedConductorSet = new ConductorSet(conductor);
    }

    /*** 
     * Call this function every tick.
     */
    public void update() {
        TileEntity te = (TileEntity) conductor;
        World w = te.getWorld();
        if (w.isRemote) {
            return;
        }
        createOrJoinConductorSet();
        if (isConductorSetLeader) {
            conductorSet.update();
        }
        int seed = ((te.xCoord << 4 + te.zCoord) << 8) + te.yCoord;
        //Does the time part really need to be here?
        if (justCreated || (w.getTotalWorldTime() + seed) % 600 == 0) {
            justCreated = false;
            if (conductorSet.leader == null) {
                conductorSet.leader = conductor;
            }
            Coord here = conductor.getCoord();
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
        final int memberCount = conductorSet.memberCount;
        if (memberCount <= 1) {
            return;
        }
        //setValue(0);
        
        for (IChargeConductor hereConductor : conductorSet.getMembers(conductor)) {
            if (hereConductor == conductor) {
                continue;
            }
            Charge hereCharge = hereConductor.getCharge();
            final int val = hereCharge.getValue();
            new ConductorSet(hereConductor).totalCharge = val;
            conductorSet.memberCount--;
            conductorSet.totalCharge -= val;
        }
        conductorSet = null;
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
    
    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        int new_val = data.as(Share.PRIVATE, prefix + "charge").putInt(getValue());
        if (data.isReader()) {
            setValue(new_val);
        }
        return this;
    }

}
