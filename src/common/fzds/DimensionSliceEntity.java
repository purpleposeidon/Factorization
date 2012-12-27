package factorization.fzds;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import factorization.api.Coord;

public class DimensionSliceEntity extends Entity {
    int cell;
    
    public Coord hammerCell;
    Object renderInfo = null;
    
    public DimensionSliceEntity(World world) {
        super(world);
        ignoreFrustumCheck = true; //kinda lame; should give ourselves a proper bounding box
    }
    
    public DimensionSliceEntity(World world, int cell) {
        this(world);
        this.cell = cell;
        this.hammerCell = Hammer.getCellCorner(cell);
    }
    
    public Vec3 real2shadow(Vec3 realCoords) {
        //NOTE: This ignores transformations! Need to fix!
        double diffX = realCoords.xCoord - posX;
        double diffY = realCoords.yCoord - posY;
        double diffZ = realCoords.zCoord - posZ;
        return Vec3.createVectorHelper(hammerCell.x + diffX, hammerCell.y + diffY, hammerCell.z + diffZ);
    }
    
    public Vec3 shadow2real(Vec3 shadowCoords) {
        //NOTE: This ignores transformations! Need to fix!
        double diffX = shadowCoords.xCoord - hammerCell.x;
        double diffY = shadowCoords.yCoord - hammerCell.y;
        double diffZ = shadowCoords.zCoord - hammerCell.z;
        return Vec3.createVectorHelper(posX + diffX, posY + diffY, posZ + diffZ);
    }
    
    @Override
    protected void entityInit() {
        // TODO Auto-generated method stub

    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        cell = tag.getInteger("cell");
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        this.ticksExisted = 1;
        super.writeToNBT(tag);
        tag.setInteger("cell", cell);
    }
    
    @Override
    public void onEntityUpdate() {
        if (this.ticksExisted < 2) {
            Hammer.slices.add(this);
        }
        super.onEntityUpdate();
        if (this.isDead) {
            Hammer.slices.remove(this);
        }
    }

}
