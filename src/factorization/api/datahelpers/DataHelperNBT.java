package factorization.api.datahelpers;

import net.minecraft.nbt.NBTTagCompound;

public abstract class DataHelperNBT extends DataHelper {
    protected NBTTagCompound tag;
    
    @Override
    public NBTTagCompound getTag() {
        return tag;
    }
    
    @Override
    public boolean isNBT() {
        return true;
    }
}
