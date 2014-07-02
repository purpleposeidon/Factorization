package factorization.fzds;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;

public class DseRayTarget extends Entity {
    //This is used on the client side to give the player something to smack
    DimensionSliceEntity parent;

    public DseRayTarget(DimensionSliceEntity parent) {
        super(parent.worldObj);
        this.parent = parent;
    }

    @Override
    protected void entityInit() { }

    @Override
    protected void readEntityFromNBT(NBTTagCompound var1) { }

    @Override
    protected void writeEntityToNBT(NBTTagCompound var1) { }
    
    @Override
    public boolean canBeCollidedWith() {
        return true;
    }
}
