package factorization.mechanisms;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.shared.Core;
import factorization.shared.NORELEASE;
import factorization.util.NumUtil;
import factorization.util.SpaceUtil;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.util.Vec3;

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
    void draw(Tessellator tess, ICamera camera, float partial, IIcon icon,
                     AxisAlignedBB workBox, Vec3 workStart, Vec3 workEnd) {
        Vec3 forward = SpaceUtil.subtract(workStart, workEnd);
        Vec3 side1 = forward.crossProduct(Vec3.createVectorHelper(1, 0, 1));
        if (SpaceUtil.isZero(side1)) {
            side1 = forward.crossProduct(Vec3.createVectorHelper(-1, 0, -1));
        }
        side1 = side1.normalize();
        Vec3 side2 = forward.crossProduct(side1).normalize();
        double d = 0.125;
        SpaceUtil.incrScale(side1, d);
        SpaceUtil.incrScale(side2, d);
        drawPlane(tess, workStart, workEnd, side1, icon);
        drawPlane(tess, workStart, workEnd, side2, icon);
    }

    void drawPlane(Tessellator tess, Vec3 workStart, Vec3 workEnd, Vec3 right, IIcon icon) {
        tess.addVertexWithUV(workStart.xCoord + right.xCoord,
                workStart.yCoord + right.yCoord,
                workStart.zCoord + right.zCoord,
                icon.getMinU(), icon.getMaxV());
        tess.addVertexWithUV(workStart.xCoord - right.xCoord,
                workStart.yCoord - right.yCoord,
                workStart.zCoord - right.zCoord,
                icon.getMinU(), icon.getMinV());
        tess.addVertexWithUV(workEnd.xCoord - right.xCoord,
                workEnd.yCoord - right.yCoord,
                workEnd.zCoord - right.zCoord,
                icon.getMaxU(), icon.getMinV());
        tess.addVertexWithUV(workEnd.xCoord + right.xCoord,
                workEnd.yCoord + right.yCoord,
                workEnd.zCoord + right.zCoord,
                icon.getMaxU(), icon.getMaxV());
    }

    public void release() {
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
