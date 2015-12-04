package factorization.coremodhooks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

import java.util.List;

public abstract class MixinRailStairs extends BlockRailBase {
    private MixinRailStairs(boolean ignored) {
        super(ignored);
    }

    @Override
    public void addCollisionBoxesToList(World world, BlockPos pos, IBlockState state, AxisAlignedBB mask, List<AxisAlignedBB> list, Entity collidingEntity) {
        // Can't call super: we *are* super!
        AxisAlignedBB underBox = ((Block) this).getCollisionBoundingBox(world, pos, state);

        if (underBox != null && mask.intersectsWith(underBox)) {
            list.add(underBox);
            return;
        }
        if (collidingEntity instanceof EntityPlayer) {
            final double h = 0.5;
            final double w = 0.25;
            final double s = 6.0 / 16.0;
            AxisAlignedBB box = null;
            BlockRailBase me = this;
            double x = pos.getX();
            double y = pos.getY();
            double z = pos.getZ();
            EnumRailDirection i = state.getValue(getShapeProperty());
            if (i == EnumRailDirection.ASCENDING_EAST) {
                box = new AxisAlignedBB(x + w, y, z + s, x + 1, y + h, z + 1 - s);
            } else if (i == EnumRailDirection.ASCENDING_WEST) {
                box = new AxisAlignedBB(x, y, z + s, x + w, y + h, z + 1 - s);
            } else if (i == EnumRailDirection.ASCENDING_NORTH) {
                box = new AxisAlignedBB(x + s, y, z, x + 1 - s, y + h, z + w);
            } else if (i == EnumRailDirection.ASCENDING_SOUTH) {
                box = new AxisAlignedBB(x + s, y, z + w, x + 1 - s, y + h, z + 1);
            }
            if (box != null && mask.intersectsWith(box)) {
                list.add(box);
            }
        }
    }
}
