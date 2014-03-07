package factorization.charge;

import java.io.DataInputStream;
import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;
import factorization.api.Charge;
import factorization.api.IChargeConductor;
import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.shared.BlockClass;
import factorization.shared.Core;
import factorization.shared.FzUtil;
import factorization.shared.TileEntityCommon;
import factorization.shared.NetworkFactorization.MessageType;

public class TileEntityBattery extends TileEntityCommon implements IChargeConductor {
    Charge charge = new Charge(this);
    int storage = 0;
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
    public IIcon getIcon(ForgeDirection dir) {
        switch (dir) {
        case UP: return BlockIcons.battery_top;
        case DOWN: return BlockIcons.battery_bottom;
        default: return BlockIcons.battery_side;
        }
    }

    @Override
    public String getInfo() {
        float f = storage * 100 / max_storage;
        if (Core.dev_environ) {
            return "Storage: " + ((int) f) + "%\n" + storage + "/" + max_storage;
        }
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
        storage = tag.getInteger("storage");
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        charge.writeToNBT(tag, "charge");
        tag.setInteger("storage", storage);
    }

    public static float getFullness(int value) {
        return value / ((float) max_storage);
    }

    public float getFullness() {
        return getFullness(storage);
    }

    @Override
    public void updateEntity() {
        if (worldObj.isRemote) {
            return;
        }
        charge.update();
        //if (getCoord().seed() + worldObj.getTotalWorldTime() % 10 != 0) {
        //	return;
        //}
        int val = getCharge().getValue();
        int store_delta = 0;
        int charge_delta = 0;
        if (val < 20) {
            //dump a bit out
            charge_delta = Math.min(20, storage);
            store_delta = -charge_delta;
        } else if (val > 30) {
            //pull it all in!
            charge_delta = -val;
            int free = max_storage - storage;
            store_delta = Math.min(val*2/3, free);
        }
        int tier = storage * 32 / max_storage;
        if (store_delta != 0) {
            charge.addValue(charge_delta);
            storage += store_delta;
        }
        if (tier != storage * 32 / max_storage) {
            markDirty();
            updateMeter();
        }
    }

    void updateMeter() {
        Core.network.broadcastMessage(null, getCoord(), MessageType.BatteryLevel, storage);
    }

    @Override
    public boolean handleMessageFromServer(MessageType messageType, DataInputStream input) throws IOException {
        if (super.handleMessageFromServer(messageType, input)) {
            return true;
        }
        if (messageType == MessageType.BatteryLevel) {
            storage = input.readInt();
            getCoord().redraw();
            return true;
        }
        return false;
    }

    @Override
    public void onPlacedBy(EntityPlayer player, ItemStack is, int side) {
        super.onPlacedBy(player, is, side);
        NBTTagCompound tag = FzUtil.getTag(is);
        if (tag.hasKey("storage")) {
            storage = tag.getInteger("storage");
        } else {
            storage = max_storage;
        }
    }

    @Override
    protected byte getExtraInfo() {
        float perc = storage / (float) max_storage;
        byte ret = (byte) (perc * 127);
        return ret;
    }

    @Override
    protected void useExtraInfo(byte b) {
        float perc = (b / 127F);
        int new_storage = (int) (max_storage * perc);
        storage = new_storage;
    }
    
    @Override
    public ItemStack getDroppedBlock() {
        ItemStack is = new ItemStack(Core.registry.battery);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("storage", storage);
        is.setTagCompound(tag);
        Core.registry.battery.normalizeDamage(is);
        return is;
    }
    
    @Override
    public int getComparatorValue(ForgeDirection side) {
        return (int) (getFullness()*0xF);
    }
}
