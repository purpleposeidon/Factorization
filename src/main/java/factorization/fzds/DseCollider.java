package factorization.fzds;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import com.google.common.io.ByteArrayDataOutput;

import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import factorization.fzds.api.IDeltaChunk;
import factorization.fzds.api.IFzdsEntryControl;
import factorization.fzds.api.IFzdsShenanigans;

public class DseCollider extends Entity implements IFzdsEntryControl, IEntityAdditionalSpawnData, IFzdsShenanigans {
    DimensionSliceEntity parent;
    Vec3 offset;
    
    int parent_id = -1;
    
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
        if (worldObj.isRemote && parent == null) {
            Entity e = worldObj.getEntityByID(parent_id);
            if (e instanceof DimensionSliceEntity) {
                parent = (DimensionSliceEntity) e;
            } else {
                return;
            }
        } else if (parent == null) {
            setDead();
            return;
        }
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
        double d = 8;
        boundingBox.minX -= d;
        boundingBox.minY -= d;
        boundingBox.minZ -= d;
        boundingBox.maxX += d;
        boundingBox.maxY += d;
        boundingBox.maxZ += d;
    }
    
    @Override
    public AxisAlignedBB getBoundingBox() {
        if (parent == null) {
            return null;
        }
        return parent.metaAABB;
    }
    
    @Override
    public void setDead() {
        super.setDead();
    }
    
    @Override protected void entityInit() { }
    @Override protected void readEntityFromNBT(NBTTagCompound var1) { }
    @Override protected void writeEntityToNBT(NBTTagCompound var1) { }
    @Override public boolean canEnter(IDeltaChunk dse) { return false; }
    @Override public boolean canExit(IDeltaChunk dse) { return false; }
    @Override public void onEnter(IDeltaChunk dse) { }
    @Override public void onExit(IDeltaChunk dse) { }

    @Override
    public void writeSpawnData(ByteBuf data) {
        if (parent == null) {
            data.writeInt(-1);
            setDead();
            return;
        }
        data.writeInt(parent.getEntityId());
        data.writeDouble(offset.xCoord);
        data.writeDouble(offset.yCoord);
        data.writeDouble(offset.zCoord);
    }

    @Override
    public void readSpawnData(ByteBuf data) {
        parent_id = data.readInt();
        if (parent_id == -1) {
            setDead();
            return;
        }
        offset = Vec3.createVectorHelper(data.readDouble(), data.readDouble(), data.readDouble());
    }
    
    @Override
    public Entity[] getParts() {
        return parent == null ? null : parent.getParts();
    }
    
    @Override
    public boolean writeToNBTOptional(NBTTagCompound par1nbtTagCompound) {
        return false;
    }
}