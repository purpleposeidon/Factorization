package factorization.client.gui;

import java.util.ArrayList;
import java.util.Iterator;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.src.AxisAlignedBB;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.FontRenderer;
import net.minecraft.src.ModelBase;
import net.minecraft.src.RenderLiving;
import net.minecraft.src.RenderManager;
import net.minecraft.src.StatCollector;
import net.minecraft.src.Tessellator;
import net.minecraft.src.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.ForgeSubscribe;

import factorization.api.Coord;
import factorization.api.DeltaCoord;
import factorization.common.Core;

public class FactorizationNotify {
    static class Message {
        Coord locus;
        String msg;
        long creationTime;
        
        Message set(Coord locus, String msg) {
            this.locus = locus;
            this.msg = msg;
            creationTime = System.currentTimeMillis();
            return this;
        }
    }
    
    static ArrayList<Message> messages = new ArrayList();
    
    public static void addMessage(Coord locus, String format, String ...args) {
        for (int i = 0; i < args.length; i++) {
            String translated = StatCollector.translateToLocal(args[i]);
            args[i] = translated;
        }
        String msg = String.format(format, (Object[]) args);
        if (Core.notify_in_chat) {
            EntityPlayer player = Minecraft.getMinecraft().thePlayer;
            if (player != null) {
                player.addChatMessage(msg);
            }
            return;
        }
        if (messages.size() > 2) {
            messages.remove(0);
        }
        for (Message m : messages) {
            if (m.locus.equals(locus)) {
                m.set(locus, msg);
                return;
            } else if (m.locus.distanceManhatten(locus) == 1) {
                m.creationTime = 0;
            }
        }
        messages.add(new Message().set(locus, msg));
    }

    @ForgeSubscribe
    public void renderMessages(RenderWorldLastEvent event) {
        doRenderMessages(event); //Forge events are too hard for eclipse to hot-swap?
    }
    
    void doRenderMessages(RenderWorldLastEvent event) {
        World w = Minecraft.getMinecraft().theWorld;
        if (w == null) {
            return;
        }
        if (messages.size() == 0) {
            return;
        }
        Core.profileStart("factorizationNotify");
        Iterator<Message> it = messages.iterator();
        long deathTime = System.currentTimeMillis() - 1000*6;
        EntityLiving camera = Minecraft.getMinecraft().renderViewEntity;
        double cx = camera.lastTickPosX + (camera.posX - camera.lastTickPosX) * (double)event.partialTicks;
        double cy = camera.lastTickPosY + (camera.posY - camera.lastTickPosY) * (double)event.partialTicks;
        double cz = camera.lastTickPosZ + (camera.posZ - camera.lastTickPosZ) * (double)event.partialTicks;
        GL11.glPushMatrix();
        GL11.glTranslated(-cx, -cy, -cz);
        GL11.glPushAttrib(GL11.GL_BLEND);
        
        
        while (it.hasNext()) {
            Message m = it.next();
            if (m.creationTime < deathTime || m.locus.w != w) {
                it.remove();
                continue;
            }
            renderMessage(m);
        }
        GL11.glPopMatrix();
        GL11.glPopAttrib();
        Core.profileEnd();
    }
    
    private void renderMessage(Message m) {
        int width = 0;
        int height = 0;
        String[] lines = m.msg.split("\n");
        FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
        for (String line : lines) {
            height += fr.FONT_HEIGHT + 2;
            width = Math.max(width, fr.getStringWidth(line));
        }
        width += 2;
        
        
        float scaling = 1.6F/60F;
        GL11.glPushMatrix();
        AxisAlignedBB bb = m.locus.getCollisionBoundingBoxFromPool();
        float y = m.locus.y;
        if (bb != null) {
            y += bb.maxY - bb.minY;
        } else {
            y += 0.5F;
        }
        GL11.glTranslatef(m.locus.x + 0.5F, y, m.locus.z + 0.5F);
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-RenderManager.instance.playerViewY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(RenderManager.instance.playerViewX, 1.0F, 0.0F, 0.0F);
        GL11.glScalef(-scaling, -scaling, scaling);
        GL11.glTranslatef(0, -10*lines.length, 0);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDepthMask(false);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        Tessellator tess = Tessellator.instance;
        int var16 = (lines.length - 1)*10;

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        tess.startDrawingQuads();
        int var17 = width / 2;
        tess.setColorRGBA_F(0.0F, 0.0F, 0.0F, 0.5F);
        tess.addVertex((double)(-var17 - 1), (double)(-1), 0.0D);
        tess.addVertex((double)(-var17 - 1), (double)(8 + var16), 0.0D);
        tess.addVertex((double)(var17 + 1), (double)(8 + var16), 0.0D);
        tess.addVertex((double)(var17 + 1), (double)(-1), 0.0D);
        tess.draw();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        int i = 0;
        for (String line : lines) {
            fr.drawString(line, -fr.getStringWidth(line) / 2, 10*i, -1);
            i++;
        }
        
        
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopMatrix();
        
    }
}
