package factorization.aabbdebug;

import java.util.ArrayList;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import factorization.shared.Core;

public enum AabbDebugger {
    INSTANCE;
    
    private AabbDebugger() {
        Core.loadBus(this);
    }
    
    static ArrayList<AxisAlignedBB> boxes = new ArrayList();
    
    public static void addBox(AxisAlignedBB box) {
        boxes.add(box.copy());
    }
    
    @SubscribeEvent
    public void clearBox(ClientTickEvent event) {
        if (event.phase == Phase.START) {
            boxes.clear();
        }
    }
    
    @SubscribeEvent
    public void drawBoxes(RenderWorldLastEvent event) {
        if (boxes.isEmpty()) return;
        World w = Minecraft.getMinecraft().theWorld;
        if (w == null) return;
        EntityLivingBase camera = Minecraft.getMinecraft().renderViewEntity;
        double cx = camera.lastTickPosX + (camera.posX - camera.lastTickPosX) * (double) event.partialTicks;
        double cy = camera.lastTickPosY + (camera.posY - camera.lastTickPosY) * (double) event.partialTicks;
        double cz = camera.lastTickPosZ + (camera.posZ - camera.lastTickPosZ) * (double) event.partialTicks;
        
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT);
        GL11.glPushMatrix();
        
        GL11.glTranslated(-cx, -cy, -cz);
        GL11.glDepthMask(false);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1, 1, 1, 0.5F);
        
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glLineWidth(1);
        for (AxisAlignedBB box : boxes) {
            RenderGlobal.drawOutlinedBoundingBox(box, 0x800000);
        }
        GL11.glDepthMask(true);
        
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }
}
