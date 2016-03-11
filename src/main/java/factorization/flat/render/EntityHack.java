package factorization.flat.render;

import factorization.api.Coord;
import factorization.util.NORELEASE;
import factorization.util.SpaceUtil;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.chunk.Chunk;

public class EntityHack extends Entity {
    final int slabY;
    public EntityHack(Chunk chunk, int slabY) {
        super(chunk.getWorld());
        Coord me = new Coord(chunk).add(0, slabY * 16, 0);
        SpaceUtil.setEntPos(this, me.toVector());
        AxisAlignedBB b = SpaceUtil.newBox(me, me.add(0x10, 0x10, 0x10));
        setEntityBoundingBox(b);
        this.slabY = slabY;
    }

    @Override protected void entityInit() { }
    @Override protected void readEntityFromNBT(NBTTagCompound tagCompund) { }
    @Override protected void writeEntityToNBT(NBTTagCompound tagCompound) { }

    @Override
    public void onUpdate() {
        NORELEASE.breakpoint();
        // We have a very large bounding box, which makes updating very slow.
        // This hack doesn't need to tick in any case.
        //super.onUpdate();
    }
}