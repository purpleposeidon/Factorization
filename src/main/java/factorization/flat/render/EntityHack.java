package factorization.flat.render;

import factorization.api.Coord;
import factorization.util.NORELEASE;
import factorization.util.SpaceUtil;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.chunk.Chunk;

public class EntityHack extends Entity {
    public EntityHack(Chunk chunk) {
        super(chunk.getWorld());
        Coord me = new Coord(chunk);
        SpaceUtil.setEntPos(this, me.toVector());
        AxisAlignedBB b = SpaceUtil.newBox(me, me.add(0xF, 0xFF, 0xF));
        setEntityBoundingBox(b);
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
