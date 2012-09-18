package factorization.common;

import java.io.DataInput;
import java.io.IOException;

import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ItemStack;
import net.minecraft.src.NBTTagCompound;
import factorization.api.Charge;
import factorization.api.IChargeConductor;
import factorization.common.NetworkFactorization.MessageType;

public class TileEntityBattery extends TileEntityCommon implements IChargeConductor {
    Charge charge = new Charge(), storage = new Charge();
    static final int max_storage = 6400;

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.BATTERY;
    }

    @Override
    public Charge getCharge() {
        return charge;
    }

    @Override
    public String getInfo() {
        float f = storage.getValue() * 100 / max_storage;
        return "Storage: " + ((int) f) + "%";
    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.Machine;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        charge.readFromNBT(tag, "charge");
        storage.readFromNBT(tag, "storage");
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        charge.writeToNBT(tag, "charge");
        storage.writeToNBT(tag, "storage");
    }

    public static float getFullness(int value) {
        return value / ((float) max_storage);
    }

    public float getFullness() {
        return getFullness(storage.getValue());
    }

    @Override
    public void updateEntity() {
        if (worldObj.isRemote) {
            return;
        }
        charge.update(this);
        int val = getCharge().getValue();
        int store_delta = 0;
        int charge_delta = 0;
        if (val == 0) {
            //dump a bit out
            charge_delta = Math.min(20, storage.getValue());
            store_delta = -charge_delta;
        } else if (val > 30) {
            //pull it all in!
            charge_delta = -val;
            int free = max_storage - storage.getValue();
            store_delta = Math.min(val*2/3, free);
        }
        int tier = storage.getValue() * 32 / max_storage;
        if (store_delta != 0) {
            charge.addValue(charge_delta);
            storage.addValue(store_delta);
        }
        if (tier != storage.getValue() * 32 / max_storage) {
            updateMeter();
        }
    }

    void updateMeter() {
        Core.network.broadcastMessage(null, getCoord(), MessageType.BatteryLevel, storage.getValue());
    }

    @Override
    public boolean handleMessageFromServer(int messageType, DataInput input) throws IOException {
        if (super.handleMessageFromServer(messageType, input)) {
            return true;
        }
        if (messageType == MessageType.BatteryLevel) {
            storage.setValue(input.readInt());
            getCoord().dirty();
            return true;
        }
        return false;
    }

    @Override
    void onPlacedBy(EntityPlayer player, ItemStack is, int side) {
        super.onPlacedBy(player, is, side);
        NBTTagCompound tag = FactorizationUtil.getTag(is);
        if (tag.hasKey("storage")) {
            storage.setValue(tag.getInteger("storage"));
        } else {
            storage.setValue(max_storage);
        }
    }

    @Override
    byte getExtraInfo() {
        float perc = storage.getValue() / (float) max_storage;
        byte ret = (byte) (perc * 127);
        return ret;
    }

    @Override
    void useExtraInfo(byte b) {
        float perc = (b / 127F);
        int new_storage = (int) (max_storage * perc);
        storage.setValue(new_storage);
    }
}
