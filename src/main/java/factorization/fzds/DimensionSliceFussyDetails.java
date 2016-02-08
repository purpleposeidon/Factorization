package factorization.fzds;

import factorization.fzds.interfaces.DimensionSliceEntityBase;
import factorization.fzds.interfaces.IDimensionSlice;
import factorization.fzds.interfaces.IFzdsEntryControl;
import factorization.util.NORELEASE;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

/**
 * This class contains boring noise.
 */
abstract class DimensionSliceFussyDetails extends DimensionSliceEntityBase implements IFzdsEntryControl {

    public DimensionSliceFussyDetails(World w) {
        super(w);
    }

    @Override
    @Deprecated
    public void setPosition(double x, double y, double z) {
        // Ignored!
        NORELEASE.fixme("Registration: don't ask for position/velocity updates!");
    }

    @Override
    public void setVelocity(double x, double y, double z) {
        // Ignored!
        NORELEASE.fixme("Registration: don't ask for position/velocity updates!");
    }

    @Override
    public boolean canEnter(IDimensionSlice dse) { return false; }

    @Override
    public boolean canExit(IDimensionSlice dse) { return true; }

    @Override
    public void onEnter(IDimensionSlice dse) { }

    @Override
    public void onExit(IDimensionSlice dse) { }

    @Override
    public void setPositionAndRotation2(double x, double y, double z, float yaw, float pitch, int posRotationIncrements, boolean p_180426_10_) {
        // This method is disabled because entity position packets call it with insufficiently precise variables.
    }

    @Override
    public void addVelocity(double ax, double ay, double az) {
        isAirBorne = false; //If this is true, we get packet spam
    }

    @Override
    public boolean attackEntityFrom(DamageSource damageSource, float damage) {
        return getController().onAttacked(this, damageSource, damage);
    }

    @Override
    public void setFire(int fireTicks) { }

    @Override
    public boolean doesEntityNotTriggerPressurePlate() {
        return true;
    }

    static final ItemStack[] blast_protection = new ItemStack[1];

    @Override
    public ItemStack[] getInventory() {
        return blast_protection;
    }

    static {
        ItemStack is = blast_protection[0] = new ItemStack(Items.diamond_chestplate, 0, 0 /* hopefully it doesn't get stolen */);
        is.addEnchantment(Enchantment.blastProtection, 88);
        // Prevents explosions from knocking the DSE around.
        // As of 1.8, requires a bug-fix to be asmed inserted (jeeze...)
    }

    public IDimensionSlice idc() {
        return this;
    }

    @Override
    public float getCollisionBorderSize() {
        return 0;
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBox() {
        return null; // universalCollider handles collisions.
    }

    @Override
    public void onCollideWithPlayer(EntityPlayer player) {
        //Maybe adjust our velocities?
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    public void killIDS() {
        setDead();
    }

    @Override
    public NBTTagCompound getTag() {
        return getEntityData();
    }
}
