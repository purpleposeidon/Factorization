package factorization.fzds;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Vec3;

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

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        return super.attackEntityFrom(source, amount);
    }

    @Override
    public boolean interactFirst(EntityPlayer playerIn) {
        return super.interactFirst(playerIn);
    }

    @Override
    public boolean interactAt(EntityPlayer player, Vec3 targetVec3) {
        return super.interactAt(player, targetVec3);
    }

    @Override
    public void onEntityUpdate() {
        super.onEntityUpdate();
    }

    @Override
    public void setDead() {
        super.setDead();
    }
}
