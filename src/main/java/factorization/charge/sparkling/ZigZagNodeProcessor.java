package factorization.charge.sparkling;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.pathfinder.NodeProcessor;

public class ZigZagNodeProcessor extends NodeProcessor {
    @Override
    public PathPoint getPathPointTo(Entity entityIn) {
        return openPoint(
                MathHelper.floor_double(entityIn.getEntityBoundingBox().minX),
                MathHelper.floor_double(entityIn.getEntityBoundingBox().minY + 0.5D),
                MathHelper.floor_double(entityIn.getEntityBoundingBox().minZ));
    }

    @Override
    public PathPoint getPathPointToCoords(Entity entityIn, double x, double y, double target) {
        return openPoint(
                MathHelper.floor_double(x - entityIn.width / 2.0),
                MathHelper.floor_double(y + 0.5D),
                MathHelper.floor_double(target - entityIn.width / 2.0));
    }

    transient int hash = -1;

    @Override
    public int findPathOptions(PathPoint[] pathOptions, Entity entity, PathPoint currentPoint, PathPoint targetPoint, float maxDistance) {
        if (hash == -1) {
            hash = entity.getUniqueID().hashCode();
            if (hash == -1) hash = 0;
        }

        int nodes = 0;

        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(0, 0, 0);
        for (EnumFacing side : EnumFacing.VALUES) {
            int x = currentPoint.xCoord + side.getFrontOffsetX();
            int y = currentPoint.yCoord + side.getFrontOffsetY();
            int z = currentPoint.zCoord + side.getFrontOffsetZ();
            int sum = x + y + z + hash;
            if (sum % 2 == 0) continue;
            at.set(x, y, z);
            if (!isSafe(entity.worldObj, at)) continue;
            pathOptions[nodes++] = openPoint(x, y, z);
        }
        return nodes;
    }

    static boolean isSafe(World world, BlockPos pos) {
        IBlockState bs = world.getBlockState(pos);
        if (bs.getBlock().getMaterial() == Material.water) return false;
        if (!bs.getBlock().isPassable(world, pos)) return false;
        return true;
    }
}
