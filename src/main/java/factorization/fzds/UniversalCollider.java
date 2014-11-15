package factorization.fzds;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import factorization.fzds.api.IDeltaChunk;
import factorization.fzds.api.IFzdsEntryControl;

class UniversalCollider extends Entity implements IFzdsEntryControl {
    private final DimensionSliceEntity dimensionSliceEntity;

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
    public AxisAlignedBB getBoundingBox() {
        return this.dimensionSliceEntity.metaAABB;
    }
    
    @Override
    public Entity[] getParts() {
        return this.dimensionSliceEntity.getRayParts();
    }

    @Override
    public boolean canEnter(IDeltaChunk dse) {
        return false;
    }

    @Override
    public boolean canExit(IDeltaChunk dse) {
        return false;
    }

    @Override
    public void onEnter(IDeltaChunk dse) { }

    @Override
    public void onExit(IDeltaChunk dse) { }
}