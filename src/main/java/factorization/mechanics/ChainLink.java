package factorization.mechanics;

import factorization.shared.Core;
import factorization.util.NumUtil;
import factorization.util.SpaceUtil;
import net.minecraft.block.Block;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ChainLink {
    private Vec3 start, end, prevStart, prevEnd;

    int bagIndex = -1;

    public void update(Vec3 newStart, Vec3 newEnd) {
        if (prevStart == null) {
            prevStart = SpaceUtil.copy(start = SpaceUtil.copy(newStart));
            prevEnd = SpaceUtil.copy(end = SpaceUtil.copy(newEnd));
        } else {
            SpaceUtil.assignVecFrom(prevStart, start);
            SpaceUtil.assignVecFrom(start, newStart);
            SpaceUtil.assignVecFrom(prevEnd, end);
            SpaceUtil.assignVecFrom(end, newEnd);
        }
    }

    @SideOnly(Side.CLIENT)
    boolean cameraCheck(ICamera camera, float partial, AxisAlignedBB workBox, Vec3 workStart, Vec3 workEnd) {
        // This is far from efficient as it ought to be.
        // If the line is diagonal, then there will be huge amounts of
        // space where the line is drawn even tho it is far off-screen.
        if (start == null) return false;
        NumUtil.interp(prevStart, start, partial, workStart);
        NumUtil.interp(prevEnd, end, partial, workEnd);
        Vec3 min = SpaceUtil.copy(workStart);
        Vec3 max = SpaceUtil.copy(workEnd);
        SpaceUtil.sort(min, max);
        // The lines are drawn fat, so make the box a bit fatter as well
        SpaceUtil.setAABB(workBox, min, max);
        final double d = -0.125;
        SpaceUtil.incrContract(workBox, d, d, d);

        return camera.isBoundingBoxInFrustum(workBox);
    }

    @SideOnly(Side.CLIENT)
    void draw(WorldClient world, Tessellator tess, ICamera camera, float partial,
                     AxisAlignedBB workBox, Vec3 workStart, Vec3 workEnd) {
        Vec3 forward = SpaceUtil.subtract(workStart, workEnd);
        double length = forward.lengthVector();
        Vec3 side1 = forward.crossProduct(new Vec3(1, 0, 1));
        if (SpaceUtil.isZero(side1)) {
            side1 = forward.crossProduct(new Vec3(-1, 0, -1));
        }
        side1 = side1.normalize();
        Vec3 side2 = forward.crossProduct(side1).normalize();
        final double d = 0.25;
        final double iconLength = 2 * d;
        SpaceUtil.incrScale(side1, d);
        SpaceUtil.incrScale(side2, d);

        double linkCount = length / 2 / iconLength;
        Vec3 normForward = forward.normalize();

        double extraLinkage = length % iconLength;
        extraLinkage *= -1;
        extraLinkage += 0.5;
        SpaceUtil.incrAdd(workStart, SpaceUtil.scale(normForward, extraLinkage));
        linkCount += extraLinkage;


        double g = 9F/32F;
        double h = g + 0.5;
        drawPlane(world, tess, workStart, workEnd, side1, g - linkCount, 1 + g);
        drawPlane(world, tess, workStart, workEnd, side2, h - linkCount, 1 + h);
    }

    void setupLight(WorldClient world, Tessellator tess, Vec3 at) {
        int x = (int) at.xCoord;
        int y = (int) at.yCoord;
        int z = (int) at.zCoord;
        Block b = world.getBlock(pos);
        int brightness = b.getMixedBrightnessForBlock(world, pos);
        tess.setBrightness(brightness);
    }

    void drawPlane(WorldClient world, Tessellator tess, Vec3 workStart, Vec3 workEnd, Vec3 right, double uStart, double uEnd) {
        setupLight(world, tess, workStart);
        tess.addVertexWithUV(workStart.xCoord + right.xCoord,
                workStart.yCoord + right.yCoord,
                workStart.zCoord + right.zCoord,
                uStart, 1);
        tess.addVertexWithUV(workStart.xCoord - right.xCoord,
                workStart.yCoord - right.yCoord,
                workStart.zCoord - right.zCoord,
                uStart, 0);
        setupLight(world, tess, workEnd);
        tess.addVertexWithUV(workEnd.xCoord - right.xCoord,
                workEnd.yCoord - right.yCoord,
                workEnd.zCoord - right.zCoord,
                uEnd, 0);
        tess.addVertexWithUV(workEnd.xCoord + right.xCoord,
                workEnd.yCoord + right.yCoord,
                workEnd.zCoord + right.zCoord,
                uEnd, 1);
    }

    public void release() {
        if (bagIndex == -1) {
            Core.logWarning("Already released");
            Thread.dumpStack();
            return;
        }
        ChainRender.instance.release(this);
        bagIndex = -1;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (bagIndex != -1) {
            Core.logWarning("ChainLink was not released! Its location: " + start + " to " + end);
        }
    }
}
