package factorization.coremodhooks;

import net.minecraft.block.Block;
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
        // Can't call super: we *are* super!
        AxisAlignedBB underBox = ((Block) this).getCollisionBoundingBoxFromPool(world, x, y, z);

        if (underBox != null && queryBox.intersectsWith(underBox)) {
            boxList.add(underBox);
            return;
        }
        if (ent instanceof EntityPlayer) {
            final double h = 0.5;
            final double w = 0.25;
            final double s = 6.0 / 16.0;
            AxisAlignedBB box = null;
            BlockRailBase me = this;
            int i = me.getBasicRailMetadata(world, null, x, y, z);
            if (i == 0x2) { // Ascend East
                box = new AxisAlignedBB(x + w, y, z + s, x + 1, y + h, z + 1 - s);
            } else if (i == 0x3) { // Ascend West
                box = new AxisAlignedBB(x, y, z + s, x + w, y + h, z + 1 - s);
            } else if (i == 0x4) { // Ascend North
                box = new AxisAlignedBB(x + s, y, z, x + 1 - s, y + h, z + w);
            } else if (i == 0x5) { // Ascend South
                box = new AxisAlignedBB(x + s, y, z + w, x + 1 - s, y + h, z + 1);
            }
            if (box != null && queryBox.intersectsWith(box)) {
                boxList.add(box);
            }
        }
    }
}
