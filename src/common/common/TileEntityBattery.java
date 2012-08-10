package factorization.common;

import net.minecraft.src.NBTTagCompound;
import factorization.api.Charge;
import factorization.api.IChargeConductor;

public class TileEntityBattery extends TileEntityCommon implements IChargeConductor {
    Charge charge = new Charge(), storage = new Charge();
    private final int max_storage = 600;

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

    float getFullness() {
        return storage.getValue() / ((float) max_storage);
    }

    private int getTargetCharge() {
        return (int) Math.min(getFullness() * 6, 0);
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        charge.update(this);
        int val = getCharge().getValue();
        int delta = 0;
        if (val < getTargetCharge()) {
            delta = Math.min(getTargetCharge() - val, storage.getValue());
        } else if (val > getTargetCharge()) {
            int free = max_storage - storage.getValue();
            if (free <= 0) {
                return;
            }
            delta = -Math.min(free, val - getTargetCharge());
        } else {
            return;
        }
        if (delta != 0) {
            charge.addValue(delta);
            storage.addValue(-delta);
        }
    }
}
