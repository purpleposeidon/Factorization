package factorization.charge.sparkling;

import factorization.util.SpaceUtil;
import net.minecraft.entity.EntityLiving;
import net.minecraft.pathfinding.PathFinder;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class PathNavigateZigZag extends PathNavigateGround {
    public PathNavigateZigZag(EntityLiving ent, World world) {
        super(ent, world);
    }

    @Override
    protected PathFinder getPathFinder() {
        return new PathFinder(new ZigZagNodeProcessor());
    }

    @Override
    protected Vec3 getEntityPosition() {
        return SpaceUtil.fromEntPos(theEntity);
    }

    @Override
    protected boolean canNavigate() {
        return !theEntity.isRiding();
    }

    @Override
    protected boolean isDirectPathBetweenPoints(Vec3 start, Vec3 end, int sizeX, int sizeY, int sizeZ) {
        Vec3 travel = end.subtract(start);
        double len = travel.lengthVector();
        if (len < 0.1) return false;
        Vec3 delta = SpaceUtil.scale(travel, 1 / len);
        double x = start.xCoord;
        double y = start.yCoord;
        double z = start.zCoord;
        int iter = (int) len + 1;
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(0, 0, 0);
        while (iter-- > 0) {
            x += delta.xCoord;
            y += delta.yCoord;
            z += delta.zCoord;
            at.set(MathHelper.floor_double(x), MathHelper.floor_double(y), MathHelper.floor_double(z));
            if (!ZigZagNodeProcessor.isSafe(worldObj, at)) return false;
        }
        return true;
    }
}
