package factorization.fzds;

import factorization.fzds.interfaces.IFzdsShenanigans;
import factorization.shared.Core;
import factorization.util.NORELEASE;
import factorization.util.NumUtil;
import factorization.util.SpaceUtil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import java.util.ArrayList;
import java.util.List;

public class MetaAxisAlignedBB extends AxisAlignedBB implements IFzdsShenanigans {
    /*
     * So, there's 2 operations: calculate<Axis>Offset and intersectsWith.
     * For each operation, we need to work a list of relevant AABBs in hammer space and apply the operation to each of them.
     * 1. Convert the argument AABB to an AABB in shadow space:
     * 		calculate the length of the diagonal
     * 		Calculate the center, as a vector. Transform the vector.
     * 		Build a new AABB using the transformed center; it will be a cube with the min/max just center ± diagonal/2
     * 		Faster for simpler cases, tho slower if the unnecessarily larger area
     * 
     * 		alternative method:
     * 			Convert input AABB to 8 points, convert them all to shadow space, make AABB out of the maximum
     * 			Too expensive!
     *
     * 2. We now have a list of AABBs in shadow space, and the passed in AABB in real world space.
     * 3. Iterate over the AABBs, converting each one to real space & applying the operation
     */
    private final DimensionSliceEntity idc;
    private final World shadowWorld;
    
    public MetaAxisAlignedBB(DimensionSliceEntity idc, World shadowWorld, AxisAlignedBB orig) {
        super(orig.minX, orig.minY, orig.minZ,
                orig.maxX, orig.maxY, orig.maxZ);
        this.idc = idc;
        this.shadowWorld = shadowWorld;
        NORELEASE.fixme("Slopes. Probably easy? :D");
    }

    public MetaAxisAlignedBB setUnderlyingNew(AxisAlignedBB bb) {
        return new MetaAxisAlignedBB(idc, shadowWorld, bb);
    }

    private static final List<AxisAlignedBB> EMPTY = new ArrayList<AxisAlignedBB>();

    List<AxisAlignedBB> getShadowBoxesWithinShadowBox(final AxisAlignedBB box) {
        final double averageEdgeLength = box.getAverageEdgeLength(); // I've measured the average averageEdgeLength to be about 24.
        if (averageEdgeLength > 1024) {
            Core.logSevere("Giant MetaAABB!? {}", this);
            Thread.dumpStack();
            return EMPTY;
        }

        // Far too slow: return shadowWorld.getCollidingBoundingBoxes(aabbHolder, aabb);
        // We could, say, use that if the averageEdgeLength was small, but then there'd be inconsistent behavior w/ ents

        final ArrayList<AxisAlignedBB> ret = new ArrayList<AxisAlignedBB>();
        final int R = 30000000;

        // Vanilla adds 1 to the max instead of using <=
        final int boxMinX = NumUtil.clip(MathHelper.floor_double(box.minX + 0), -R, +R);
        final int boxMaxX = NumUtil.clip(MathHelper.floor_double(box.maxX + 1), -R, +R);
        final int boxMinY = NumUtil.clip(MathHelper.floor_double(box.minY + 0), 0, 0xFF);
        final int boxMaxY = NumUtil.clip(MathHelper.floor_double(box.maxY + 1), 0, 0xFF);
        final int boxMinZ = NumUtil.clip(MathHelper.floor_double(box.minZ + 0), -R, +R);
        final int boxMaxZ = NumUtil.clip(MathHelper.floor_double(box.maxZ + 1), -R, +R);

        final int chunkMinX = boxMinX >> 4;
        final int chunkMaxX = (boxMaxX >> 4) + 1;
        final int chunkMinZ = boxMinZ >> 4;
        final int chunkMaxZ = (boxMaxZ >> 4) + 1;

        // (Manually inline Coord.iterateChunk)
        for (int chunkX = chunkMinX; chunkX < chunkMaxX; chunkX++) {
            for (int chunkZ = chunkMinZ; chunkZ < chunkMaxZ; chunkZ++) {
                // We could do a shadowWorld.blockExists() check here. Let's not:
                // { small DSE, large multi-chunk DSE} × { DSE moving, DSE stopped } × { adjacent loaded, adjacent unloaded, adjacent not generated }
                // If the DSE is stopped, then things may load, but minor.
                // If the adjacent is loaded, then no issue.
                // { small DSE, large DSE} × { DSE moving } × { adjacent unloaded, adjacent not generated }
                // If the DSE is small & moving, then it'll stop ticking once it gets near the edge of ticking area.
                // If a large moving DSE is exiting loaded area, then ***there may indeed be trouble***
                // But checking if a chunk is loaded is a bit expensive, yes...
                // If there was a 'get chunk w/o trying to load', we'd very much want that.

                final Chunk chunk = shadowWorld.getChunkFromChunkCoords(chunkX, chunkZ);
                final int cornerX = chunk.xPosition << 4, cornerZ = chunk.zPosition << 4;
                final int lx = NumUtil.clip(boxMinX, cornerX, cornerX + 16);
                final int hx = NumUtil.clip(boxMaxX + 1, cornerX, cornerX + 16);
                final int lz = NumUtil.clip(boxMinZ, cornerZ, cornerZ + 16);
                final int hz = NumUtil.clip(boxMaxZ + 1, cornerZ, cornerZ + 16);
                // Iterate nesting "YZX" to go with the grain of how data is stored in NibbleArrays.
                // It is well that Y is on the outside, so that the ExtendedBlockStorages get visited one-at-a-time
                // instead of jumping around constantly.
                BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
                for (int y = boxMinY; y < boxMaxY; y++) {
                    for (int z = lz; z < hz; z++) {
                        for (int x = lx; x < hx; x++) {
                            if (!(NumUtil.intersect(x, x + 1, box.minX, box.maxX)
                                    && NumUtil.intersect(y, y + 1 /* Or +2 for fences. Nobody loves you, fences. */, box.minY, box.maxY)
                                    && NumUtil.intersect(z, z + 1, box.minZ, box.maxZ))) {
                                // A simple sampling run while fighting a colossus found:
                                //   20% of blocks visited collided
                                //   31% of blocks visited could be excluded by the simple cube check
                                //   79% of blocks visited did not collide via addCollisionBoxesToList
                                // Seems worthwhile.
                                continue;
                            }

                            IBlockState bs = chunk.getBlockState(pos.set(x, y, z));
                            bs.getBlock().addCollisionBoxesToList(shadowWorld, pos, bs, box, ret, idc);
                        }
                    }
                }

                // We could totally do entities here. I choose not to!
            }
        }
        return ret;
    }

    List<AxisAlignedBB> getShadowBoxesInRealBox(AxisAlignedBB realBox) {
        NORELEASE.fixme("Convert to iterator style");
        double expansion = 0.3660254037844387;
        // It is important that expansion be the right value.
        // If it is too small, then collisions will be incorrect when rotated.
        // If it is too big, then there will be significant lag with e.g. splash potions.
        // Currently using width = 1.
        // (Which is cheating. It should be 2 due to fences, but I think this is a good tradeoff)
        
        // Here's how this number is derived 
        // Take our max-sized cube.
        // Calculate the distance between the center of the cube and a corner.
        // If we move that cube to the origin and rotate it so that the corner pokes out as far as it can,
        // this is equivalent to laying the corner_radius down flat, and the amount that it pokes over the cube's
        // area is what our expansion should be.
        // corner_radius = sqrt(3 * (width/2)**2)
        // expansion = corner_radius + 1 - 0.5
        
        // Optimization: make the expansion depend on the rotation; so the expansion would
        // range from 0, at no rotation, to <whatever the maximum should be> at the most extreme angles.
        // Could probably be done as a simpleish function depending on rotationQuaternion.w
        // Or could do it a bit slower & cache it. (And maybe doing it live wouldn't be bad, since this isn't the slow part yet)
        AxisAlignedBB shadowBox = convertRealBoxToShadowBox(realBox);
        if (!idc.getTransform().getRot().isZero()) {
            shadowBox = shadowBox.expand(expansion, expansion, expansion);
        }
        return getShadowBoxesWithinShadowBox(shadowBox);
    }

    AxisAlignedBB convertRealBoxToShadowBox(AxisAlignedBB realBox) {
        // This function returns a box is likely larger than what it should really be.
        // A more accurate algo would be to translate each corner and make a box that contains them.
        // That'd be slower tho. (And, anyways, if we really wanted accuracy, we'd imlpement non-AA collisions...)
        // NORELEASE: Let's try for non-AA collisions!
        double d = SpaceUtil.getDiagonalLength(realBox);
        Vec3 realMiddle = SpaceUtil.getMiddle(realBox);
        Vec3 shadowMiddle = convertRealVecToShadowVec(realMiddle);
        return new AxisAlignedBB(
                shadowMiddle.xCoord - d, shadowMiddle.yCoord - d, shadowMiddle.zCoord - d,
                shadowMiddle.xCoord + d, shadowMiddle.yCoord + d, shadowMiddle.zCoord + d);
    }
    
    AxisAlignedBB convertShadowBoxToRealBox(AxisAlignedBB shadowBox) {
        // We're gonna try a different approach here.
        // Will work well so long as everything is a cube.
        // NORELEASE: Why?
        Vec3 shadowMidle = SpaceUtil.getMiddle(shadowBox);
        Vec3 minMinusMiddle = SpaceUtil.getMin(shadowBox).subtract(shadowMidle);
        Vec3 maxMinusMiddle = SpaceUtil.getMax(shadowBox).subtract(shadowMidle);
        Vec3 realMiddle = convertShadowVecToRealVec(shadowMidle);
        return SpaceUtil.newBox(minMinusMiddle.add(realMiddle), maxMinusMiddle.add(realMiddle));
    }
    
    Vec3 convertRealVecToShadowVec(Vec3 real) {
        return idc.real2shadow(real);
    }
    
    Vec3 convertShadowVecToRealVec(Vec3 shadow) {
        return idc.shadow2real(shadow);
    }
    
    /*
     * The three functions are decomposed here:
     *  - Vec3 offset = rotateOffset(XcurrentOffset, YcurrentOffset, ZcurrentOffset)
     *  - bbs.get(i).calculate_AXIS_Offset
     * 
     * *NOTE* currentOffset is the length of a vector aligned to the relevant axis in **real world space**.
     * So, we just need to rotate the vector and use that to adjust the peek-area.
     */
    @Override
    public double calculateXOffset(AxisAlignedBB collider, double currentOffset) {
        List<AxisAlignedBB> shadowBoxes = getShadowBoxesInRealBox(collider.expand(currentOffset, 0, 0));
        for (AxisAlignedBB shadowBox : shadowBoxes) {
            AxisAlignedBB realShadow = convertShadowBoxToRealBox(shadowBox);
            currentOffset = realShadow.calculateXOffset(collider, currentOffset);
        }
        return currentOffset;
    }
    
    @Override
    public double calculateYOffset(AxisAlignedBB collider, double currentOffset) {
        List<AxisAlignedBB> shadowBoxes = getShadowBoxesInRealBox(collider.expand(0, currentOffset, 0));
        for (AxisAlignedBB shadowBox : shadowBoxes) {
            AxisAlignedBB realShadow = convertShadowBoxToRealBox(shadowBox);
            currentOffset = realShadow.calculateYOffset(collider, currentOffset);
        }
        return currentOffset;
    }
    
    @Override
    public double calculateZOffset(AxisAlignedBB collider, double currentOffset) {
        List<AxisAlignedBB> shadowBoxes = getShadowBoxesInRealBox(collider.expand(0, 0, currentOffset));
        for (AxisAlignedBB shadowBox : shadowBoxes) {
            AxisAlignedBB realShadow = convertShadowBoxToRealBox(shadowBox);
            currentOffset = realShadow.calculateZOffset(collider, currentOffset);
        }
        return currentOffset;
    }

    public AxisAlignedBB intersectsWithGet(AxisAlignedBB collider) {
        if (!idc.realArea.intersectsWith(collider)) return null;
        List<AxisAlignedBB> shadowBoxes = getShadowBoxesInRealBox(collider);
        for (AxisAlignedBB shadowBox : shadowBoxes) {
            AxisAlignedBB realShadow = convertShadowBoxToRealBox(shadowBox);
            if (realShadow.intersectsWith(collider)) {
                return shadowBox;
            }
        }
        return null;
    }

    @Override
    public boolean intersectsWith(AxisAlignedBB collider) {
        return intersectsWithGet(collider) != null;
    }
    
    @Override
    public String toString() {
        return "META" + super.toString();
    }

}
