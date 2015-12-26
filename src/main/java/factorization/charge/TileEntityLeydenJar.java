package factorization.charge;

import factorization.api.Charge;
import factorization.api.Coord;
import factorization.api.IChargeConductor;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.common.FactoryType;
import factorization.shared.BlockClass;
import factorization.shared.Core;
import factorization.net.StandardMessageType;
import factorization.shared.TileEntityCommon;
import factorization.util.ItemUtil;
import factorization.util.NumUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;

import java.io.IOException;
import java.util.Random;

public class TileEntityLeydenJar extends TileEntityCommon implements IChargeConductor, ITickable {
    private Charge charge = new Charge(this);
    int storage = 0;
    
    static final double max_efficiency = 0.50, min_efficiency = 1.00;
    static final int max_charge_threshold = 70, min_charge_threshold = 20;
    static final int max_discharge_threshold = 40, min_discharge_threshold = 10;
    public static final int max_storage = 6400*200;
    static final int max_discharge_per_tick = 50;

    // NORELEASE: Add ChargeSparks to the leyden jar.
    
    char last_light = (char) -1;
    
    @Override
    public FactoryType getFactoryType() {
        return FactoryType.LEYDENJAR;
    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.MachineDynamicLightable;
    }
    
    public double getLevel() {
        return (((double)storage) / max_storage);
    }

    @Override
    public String getInfo() {
        String ret = "Storage: " + (int)(getLevel()*100) + "%";
        if (Core.dev_environ) {
            ret += "\n" + storage + "/" + max_storage;
            ret += "\nCharges at: " + getChargeThreshold();
            ret += "\nDischarge: " + getDischargeThreshold();
        }
        return ret;
    }

    @Override
    public Charge getCharge() {
        return charge;
    }
    
    public double getEfficiency() {
        double range = max_efficiency - min_efficiency;
        return min_efficiency + range*(1 - getLevel());
    }
    
    public int getChargeThreshold() {
        return (int) NumUtil.interp(min_charge_threshold, max_charge_threshold, (float) getLevel());
    }
    
    public int getDischargeThreshold() {
        return (int) NumUtil.interp(min_discharge_threshold, max_discharge_threshold, (float) getLevel());
    }
    
    private static Random rand = new Random();
    private static double randomizeDirection(int i) {
        if (i == 0) {
            final double turn = 2*Math.PI;
            double r = Math.cos(rand.nextDouble()*turn) - 1;
            r += r < -1 ? 2 : 0;
            return r*0.3 + 0.5;
        }
        return i*0.4 + 0.5;
    }
    
    @Override
    public void update() {
        charge.update();
        final Coord here = getCoord();
        if (worldObj.isRemote) {
            char now_light = (char) getDynamicLight();
            if (last_light == -1) {
                last_light = now_light;
            }
            if (Math.abs(last_light - now_light) > 1) {
                last_light = now_light;
                here.updateBlockLight();
                here.redraw();
            }
            return;
        }
        if (here.isPowered()) {
            return;
        }
        boolean change = false;
        int charge_value = charge.getValue();
        int charge_threshold = getChargeThreshold();
        int discharge_threshold = getDischargeThreshold();
        if (charge_value > charge_threshold) {
            int max_charge_per_tick = Math.max(20, charge_value / 5);
            double efficiency = getEfficiency();
            int free = max_storage - storage;
            int to_take = Math.min(charge_value - charge_threshold, max_charge_per_tick);
            to_take = Math.min(free, to_take);
            int gain = (int) (to_take*efficiency);
            if (gain > 0) {
                storage += charge.deplete(to_take)*efficiency;
                change = true;
            }
        } else if (charge_value < discharge_threshold) {
            int free = discharge_threshold - charge_value;
            int to_give = Math.min(Math.min(storage, free), max_discharge_per_tick);
            if (to_give > 0) {
                storage -= charge.addValue(to_give);
                change = true;
            }
        }
        if (change) {
            markDirty();
            updateClients();
        }
    }
    
    int last_storage = -1;
    
    void updateClients() {
        if (storage != last_storage) {
            if (NumUtil.significantChange(storage, last_storage, 0.05F)) {
                broadcastMessage(null, StandardMessageType.SetAmount, storage);
                last_storage = storage;
            }
        }
    }
    
    @Override
    public boolean handleMessageFromServer(StandardMessageType messageType, ByteBuf input) throws IOException {
        if (super.handleMessageFromServer(messageType, input)) {
            return true;
        }
        if (messageType == StandardMessageType.SetAmount) {
            storage = input.readInt();
            return true;
        }
        return false;
    }

    @Override
    public void putData(DataHelper data) throws IOException {
        storage = data.as(Share.VISIBLE, "store").putInt(storage);
    }

    @Override
    public void loadFromStack(ItemStack is) {
        super.loadFromStack(is);
        storage = ItemUtil.getTag(is).getInteger("storage");
    }
    
    @Override
    public int getDynamicLight() {
        return (int) (getLevel()*7);
    }
    
    @Override
    public ItemStack getDroppedBlock() {
        ItemStack is = new ItemStack(Core.registry.item_factorization, 1, getFactoryType().md);
        NBTTagCompound tag = ItemUtil.getTag(is);
        tag.setInteger("storage", storage);
        return is;
    }
    
    @Override
    public int getComparatorValue(EnumFacing side) {
        return (int) (getLevel()*0xF);
    }
}
