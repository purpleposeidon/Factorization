package factorization.mechanisms;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import factorization.shared.Core;
import factorization.shared.FastBag;
import factorization.util.SpaceUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.culling.Frustrum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import org.lwjgl.opengl.GL11;

public class ChainRender {
    public static final ChainRender instance = new ChainRender();

    private ChainRender() {
        Core.loadBus(this);
    }

    private FastBag<ChainLink> chains = new FastBag<ChainLink>();

    public ChainLink add() {
        ChainLink ret = new ChainLink();
        ret.bagIndex = chains.size();
        chains.add(ret);
        return ret;
    }

    void release(ChainLink link) {
        final int index = link.bagIndex;
        chains.remove(index);
        if (index >= chains.size()) return;
        chains.get(index).bagIndex = index;
    }

    @SubscribeEvent
    public void drawChains(RenderWorldLastEvent event) {
        if (chains.isEmpty()) return;
        final float partial = event.partialTicks;
        final ICamera camera = getFrustum(partial);
        final Tessellator tess = Tessellator.instance;
        final AxisAlignedBB workBox = SpaceUtil.newBox();
        final Vec3 workStart = SpaceUtil.newVec(), workEnd = SpaceUtil.newVec();
        final IIcon icon = Blocks.redstone_lamp.getIcon(0, 0);
        boolean setup = false;
        for (ChainLink chain : chains) {
            if (!chain.cameraCheck(camera, partial, workBox, workStart, workEnd)) continue;
            if (!setup) {
                setup = true;
                tess.startDrawingQuads();

                EntityLivingBase eyePos = Minecraft.getMinecraft().renderViewEntity;
                double cx = eyePos.lastTickPosX + (eyePos.posX - eyePos.lastTickPosX) * (double) event.partialTicks;
                double cy = eyePos.lastTickPosY + (eyePos.posY - eyePos.lastTickPosY) * (double) event.partialTicks;
                double cz = eyePos.lastTickPosZ + (eyePos.posZ - eyePos.lastTickPosZ) * (double) event.partialTicks;
                tess.setTranslation(-cx, -cy, -cz);
            }
            GL11.glBegin(GL11.GL_LINES);
            GL11.glVertex3d(workStart.xCoord, workStart.yCoord, workStart.zCoord);
            GL11.glVertex3d(workEnd.xCoord, workEnd.yCoord + 1, workEnd.zCoord);
            GL11.glEnd();
            chain.draw(tess, camera, partial, icon, workBox, workStart, workEnd);
        }
        if (!setup) return;

        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        tess.draw();
        tess.setTranslation(0, 0, 0);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glPopMatrix();
    }

    @SubscribeEvent
    public void reset(WorldEvent.Unload unload) {
        if (!unload.world.isRemote) return;
        for (ChainLink chain : chains) {
            chain.bagIndex = -1;
        }
        chains.clear();
    }

    ICamera getFrustum(float partial) {
        // Unfortunately we have to make our own Frustum.
        final Minecraft mc = Minecraft.getMinecraft();
        final EntityLivingBase eye = mc.renderViewEntity;
        double eyeX = eye.lastTickPosX + (eye.posX - eye.lastTickPosX) * (double)partial;
        double eyeY = eye.lastTickPosY + (eye.posY - eye.lastTickPosY) * (double)partial;
        double eyeZ = eye.lastTickPosZ + (eye.posZ - eye.lastTickPosZ) * (double)partial;

        Frustrum frustrum = new Frustrum(); // Notch can't spell
        frustrum.setPosition(eyeX, eyeY, eyeZ);
        return frustrum;
    }


}
