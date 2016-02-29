package factorization.flat;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.chunk.Chunk;

public class EntityHack extends Entity {
    public EntityHack(Chunk chunk) {
        super(chunk.getWorld());
        posX = chunk.xPosition;
        posY = 0;
        posZ = chunk.zPosition;
        setEntityBoundingBox(new AxisAlignedBB(0, 0, 0, 0xF, 0xFF, 0xF).addCoord(posX, posY, posZ));
    }

    @Override protected void entityInit() { }
    @Override protected void readEntityFromNBT(NBTTagCompound tagCompund) { }
    @Override protected void writeEntityToNBT(NBTTagCompound tagCompound) { }
}
