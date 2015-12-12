package factorization.mechanics;

import factorization.shared.Core;
import factorization.util.NumUtil;
import factorization.util.SpaceUtil;
import net.minecraft.block.Block;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
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
            prevStart = start;
            start = newStart;
            prevEnd = end;
            end = newEnd;
        }
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


    @SideOnly(Side.CLIENT)
    void visitChain(ICamera camera, float partial, ChainRender cr) {
        // This is far from efficient as it ought to be.
        // If the line is diagonal, then there will be huge amounts of
        // space where the line is drawn even tho it is far off-screen.
        if (start == null) return;
        Vec3 s = getStart(partial);
        Vec3 e = getEnd(partial);
        // The lines are drawn fat, so make the box a bit fatter as well
        final double d = 0.125;
        AxisAlignedBB box = SpaceUtil.newBoxSort(s, e).expand(d, d, d);

        if (camera.isBoundingBoxInFrustum(box)) {
            cr.drawChain(s, e, partial);
        }
    }


    public Vec3 getStart(float partial) {
        return NumUtil.interp(prevStart, start, partial);
    }

    public Vec3 getEnd(float partial) {
        return NumUtil.interp(prevEnd, end, partial);
    }
}
