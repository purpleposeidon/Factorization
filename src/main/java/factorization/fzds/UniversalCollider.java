package factorization.fzds;

import factorization.fzds.interfaces.IDimensionSlice;
import factorization.fzds.interfaces.IFzdsEntryControl;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

class UniversalCollider extends Entity implements IFzdsEntryControl {
    public final DimensionSliceEntity dimensionSliceEntity;

    public UniversalCollider(DimensionSliceEntity dimensionSliceEntity, World world) {
        super(world);
        this.dimensionSliceEntity = dimensionSliceEntity;
    }

    @Override
    protected void entityInit() { }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) { }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) { }

    @Override
    public AxisAlignedBB getEntityBoundingBox() {
        return this.dimensionSliceEntity.metaAABB;
    }

    @Override
    public Entity[] getParts() {
        return this.dimensionSliceEntity.getRayParts();
    }

    @Override
    public boolean canEnter(IDimensionSlice dse) {
        return false;
    }

    @Override
    public boolean canExit(IDimensionSlice dse) {
        return false;
    }

    @Override
    public void onEnter(IDimensionSlice dse) { }

    @Override
    public void onExit(IDimensionSlice dse) { }

    @Override
    public boolean doesEntityNotTriggerPressurePlate() {
        return true;
    }
}