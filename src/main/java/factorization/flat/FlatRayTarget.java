package factorization.flat;

import factorization.api.Coord;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

public class FlatRayTarget extends Entity {
    public FlatRayTarget(World world) {
        super(world);
    }

    @Override protected void readEntityFromNBT(NBTTagCompound tagCompund) { }
    @Override protected void writeEntityToNBT(NBTTagCompound tagCompound) { }
    @Override protected void entityInit() { }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public float getCollisionBorderSize() {
        return 0;
    }

    public Coord at;
    public EnumFacing side;
    public AxisAlignedBB box;
}
