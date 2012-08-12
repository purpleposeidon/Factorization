package factorization.common;

import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ItemStack;
import net.minecraft.src.NBTTagCompound;
import factorization.api.Charge;
import factorization.api.IChargeConductor;

public class TileEntityBattery extends TileEntityCommon implements IChargeConductor {
    Charge charge = new Charge(), storage = new Charge();
    private static final int max_storage = 6400;

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.BATTERY;
    }

    @Override
    public Charge getCharge() {
        return charge;
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
        super.updateEntity();
        charge.update(this);
        int val = getCharge().getValue();
        int delta = 0;
        final int min_charge = 10, max_charge = 30;
        if (val < min_charge) {
            delta = Math.min(min_charge - val, storage.getValue());
        } else if (val > max_charge) {
            int free = max_storage - storage.getValue();
            if (free <= 0) {
                return;
            }
            delta = -Math.min(free, val - max_charge);
        } else {
            return;
        }
        int tier = storage.getValue() * 32 / max_storage;
        if (delta != 0) {
            charge.addValue(delta);
            storage.addValue(-delta);
        }
        if (tier != storage.getValue() * 32 / max_storage) {
            getCoord().dirty();
        }
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
}
