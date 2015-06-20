package factorization.coremodhooks;

import net.minecraft.block.BlockRailBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import java.util.List;

public class MixinRailStairs extends BlockRailBase {
    private MixinRailStairs(boolean ignored) {
        super(ignored);
    }

    @Override
    public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB queryBox, List boxList, Entity ent) {
        if (ent instanceof EntityPlayer) {
            final double h = 0.5;
            final double w = 0.5;
            AxisAlignedBB box = null;
            BlockRailBase me = this;
            int i = me.getBasicRailMetadata(world, null, x, y, z);
            if (i == 0x2) { // Ascend East
                box = AxisAlignedBB.getBoundingBox(x + w, y, z, x + 1, y + h, z + 1);
            } else if (i == 0x3) { // Ascend West
                box = AxisAlignedBB.getBoundingBox(x, y, z, x + w, y + h, z + 1);
            } else if (i == 0x4) { // Ascend North
                box = AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + h, z + w);
            } else if (i == 0x5) { // Ascend South
                box = AxisAlignedBB.getBoundingBox(x, y, z + w, x + 1, y + h, z + 1);
            }
            if (box != null && queryBox.intersectsWith(box)) {
                boxList.add(box);
            }
        }
        // Don't call super. We *are* the super.
    }
}
