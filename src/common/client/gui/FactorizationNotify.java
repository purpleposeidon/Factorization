package factorization.client.gui;

import java.util.ArrayList;
import java.util.Iterator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.ForgeSubscribe;

import org.lwjgl.opengl.GL11;

import factorization.api.Coord;
import factorization.common.Core;

public class FactorizationNotify {
    static class Message {
        Coord locus;
        String msg;
        long creationTime;
        boolean position_important;
        TileEntity orig_under;
        
        Message set(Coord locus, String msg, boolean long_lasting, boolean position_important) {
            creationTime = System.currentTimeMillis();
            if (long_lasting) {
                creationTime += 1000*5;
            } 
            this.locus = locus;
            this.msg = msg;
            this.position_important = position_important;
            orig_under = locus.getTE();
            return this;
        }
    }
    
    static ArrayList<Message> messages = new ArrayList();
    
    public static void addMessage(Coord locus, String format, String ...args) {
        if (format.equals("!clear")) {
            messages.clear();
            return;
        }
        for (int i = 0; i < args.length; i++) {
            String translated = StatCollector.translateToLocal(args[i]);
            args[i] = translated;
        }
        String msg = String.format(format, (Object[]) args);
        
        boolean force_position = msg.startsWith("\b");
        if (force_position) {
            msg = msg.substring(1);
        }
        boolean long_lasting = msg.startsWith("\t");
        if (long_lasting) {
            msg = msg.substring(1);
        }
        if (msg.length() == 0) {
            return;
        }
        if (Core.notify_in_chat) {
            EntityPlayer player = Minecraft.getMinecraft().thePlayer;
            if (player != null) {
                player.addChatMessage(msg);
            }
            return;
        }
        if (messages.size() > 2 && !force_position) {
            messages.remove(0);
        }
        for (Message m : messages) {
            if (m.locus.equals(locus)) {
                m.set(locus, msg, long_lasting, force_position);
                return;
            } else if (m.locus.distanceManhatten(locus) == 1 && !force_position) {
                m.creationTime = 0;
            }
        }
        messages.add(new Message().set(locus, msg, long_lasting, force_position));
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
            if (m.orig_under != m.locus.getTE()) {
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
        scaling *= 2F/3F;
        GL11.glPushMatrix();
        
        float y = m.locus.y;
        AxisAlignedBB bb = m.locus.getCollisionBoundingBoxFromPool();
        if (bb != null && !m.position_important) {
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
