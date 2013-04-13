package factorization.common;

import java.io.DataInput;
import java.io.IOException;

import net.minecraft.util.Vec3;

import factorization.api.Charge;
import factorization.api.IChargeConductor;
import factorization.client.render.ChargeSparks;
import factorization.common.NetworkFactorization.MessageType;

public class TileEntityLeydenJar extends TileEntityCommon implements IChargeConductor {
    private Charge charge = new Charge(this);
    private int storage = 0;
    private final double efficiency = 0.02;
    private final int charge_threshold = 500;
    private final int discharge_threshold = 100;
    private final int max_storage = 6400*100;
    private final int max_charge_per_tick = 50;
    private final int max_discharge_per_tick = 80;

    public ChargeSparks sparks = null;
    
    @Override
    public FactoryType getFactoryType() {
        return FactoryType.LEYDENJAR;
    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.Machine;
    }
    
    public byte getLevel() {
        return (byte) (((double)storage) / max_storage);
    }

    @Override
    public String getInfo() {
        return "Storage: " + (int)(getLevel())*100 + "%%";
    }

    @Override
    public Charge getCharge() {
        return charge;
    }
    
    @Override
    public void updateEntity() {
        charge.update();
        if (worldObj.isRemote) {
            if (sparks == null) {
                sparks = new ChargeSparks(10);
            }
            if (getLevel() > Math.random()) {
                Vec3 src = Vec3.createVectorHelper(xCoord + 0.5, yCoord, zCoord + 0.5);
                Vec3 dest = Vec3.createVectorHelper(xCoord + 0.5, yCoord + 1, zCoord + 0.5); //TODO
                sparks.spark(src, dest, 50, 10, 5, 1, 0xFF80FF);
            }
            sparks.update();
            return;
        }
        boolean change = false;
        if (charge.getValue() > charge_threshold) {
            int free = max_storage - storage;
            int to_take = Math.min(charge.getValue() - charge_threshold, max_charge_per_tick);
            to_take = Math.min(free, to_take);
            if (to_take > 0) {
                storage += charge.deplete(to_take)*efficiency;
                change = true;
            }
        } else if (charge.getValue() < discharge_threshold) {
            int free = discharge_threshold - charge.getValue();
            int to_give = Math.min(Math.min(storage, free), max_discharge_per_tick);
            if (to_give > 0) {
                storage -= charge.addValue(to_give);
                change = true;
            }
        }
        if (change) {
            updateClients();
        }
    }
    
    byte last_level = -1;
    
    void updateClients() {
        byte level = getLevel();
        if (level == 0 && storage != 0) {
            level = 1;
        }
        if (level != last_level) {
            broadcastMessage(null, MessageType.LeydenjarLevel, level);
        }
    }
    
    @Override
    public boolean handleMessageFromServer(int messageType, DataInput input) throws IOException {
        if (super.handleMessageFromServer(messageType, input)) {
            return true;
        }
        if (messageType == MessageType.LeydenjarLevel) {
            double perc = 100.0/input.readByte();
            storage = (int) (perc*max_storage);
            return true;
        }
        return false;
    }
    
}
