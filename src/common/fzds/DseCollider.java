package factorization.fzds;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;

import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraft.client.particle.EntityDiggingFX;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import factorization.common.Core;
import factorization.fzds.api.IFzdsEntryControl;

public class DseCollider extends Entity implements IFzdsEntryControl, IEntityAdditionalSpawnData {
    DimensionSliceEntity parent;
    Vec3 offset;
    
    public DseCollider(World world) {
        super(world);
    }
    
    public DseCollider(DimensionSliceEntity parent, Vec3 offsets) {
        super(parent.worldObj);
        this.parent = parent;
        this.offset = offsets;
        this.parent.children.add(this);
    }

    @Override
    public void onEntityUpdate() {
        //TODO: Take transformations into account
        if (parent.isDead && !worldObj.isRemote) {
            setDead();
            return;
        }
        if (parent.realArea == null) {
            return;
        }
        posX = parent.posX + offset.xCoord;
        posY = parent.posY + offset.yCoord;
        posZ = parent.posZ + offset.zCoord;
        AxisAlignedBB toSet = parent.realArea;
        boundingBox.setBB(toSet);
    }
    
    @Override
    public AxisAlignedBB getBoundingBox() {
        return parent.metaAABB;
        //return boundingBox;
    }
    
    @Override
    public boolean addEntityID(NBTTagCompound par1nbtTagCompound) {
        //Don't save this entity
        return false;
    }
    
    @Override
    public void setDead() {
        super.setDead();
    }
    
    @Override protected void entityInit() { }
    @Override protected void readEntityFromNBT(NBTTagCompound var1) { }
    @Override protected void writeEntityToNBT(NBTTagCompound var1) { }
    @Override public boolean canEnter(DimensionSliceEntity dse) { return false; }
    @Override public boolean canExit(DimensionSliceEntity dse) { return false; }
    @Override public void onEnter(DimensionSliceEntity dse) { }
    @Override public void onExit(DimensionSliceEntity dse) { }

    @Override
    public void writeSpawnData(ByteArrayDataOutput data) {
        data.writeInt(parent.entityId);
        data.writeDouble(offset.xCoord);
        data.writeDouble(offset.yCoord);
        data.writeDouble(offset.zCoord);
    }

    @Override
    public void readSpawnData(ByteArrayDataInput data) {
        int parent_id = data.readInt();
        offset = Vec3.createVectorHelper(data.readDouble(), data.readDouble(), data.readDouble());
        Entity e = worldObj.getEntityByID(parent_id);
        if (e instanceof DimensionSliceEntity) {
            parent = (DimensionSliceEntity) e;
        } else {
            Core.logFine("DseCollider's parent didn't exist. (Could be out of range)");
            setDead();
        }
    }
}