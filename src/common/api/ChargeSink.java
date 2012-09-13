package factorization.api;

import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.World;

public class ChargeSink {
    // Charge doesn't seem to propagate well over long distances. To compensate for this, we'll consume whole charges at a time.
    // And to make *that* easier, we'll use this simple class.
    
    private Charge buffer = new Charge(), charge = new Charge();
    
    public void update(IChargeConductor te) {
        charge.update(te);
    }
    
    public Charge getCharge() {
        return charge;
    }
    
    public int takeCharge(int max) {
        return takeCharge(max, -1);
    }
    
    public int takeCharge(int max, int min) {
        if (max > buffer.getValue()) {
            buffer.addValue(charge.deplete());
        }
        if (min != -1 && buffer.getValue() < min) {
            return 0;
        }
        return buffer.deplete(max);
    }
    
    public void writeToNBT(NBTTagCompound tag) {
        charge.writeToNBT(tag, "charge");
        buffer.writeToNBT(tag, "buffer");
    }
    
    public void readFromNBT(NBTTagCompound tag) {
        charge.readFromNBT(tag, "charge");
        buffer.readFromNBT(tag, "buffer");
    }
    
    public int getBufferValue() {
        return buffer.getValue();
    }
    
    public int getChargeValue() {
        return charge.getValue();
    }
}
