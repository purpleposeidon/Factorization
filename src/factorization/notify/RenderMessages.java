package factorization.notify;

import java.util.ArrayList;
import java.util.Iterator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeSubscribe;

import org.lwjgl.opengl.GL11;

import factorization.api.Coord;
import factorization.notify.Notify.Style;

public class RenderMessages extends RenderMessagesProxy {
    static ArrayList<Message> messages = new ArrayList();
    
    {
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    @Override
    public void addMessage(Object locus, ItemStack item, String format, String... args) {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player == null || player.worldObj == null) {
            return;
        }
        Message msg = new Message(player.worldObj, locus, item, format, args);
        if (msg.style.contains(Style.CLEAR)) {
            messages.clear();
            return;
        }
        
        boolean force_position = msg.style.contains(Style.FORCE);
        
        if (messages.size() > 4 && !force_position) {
            messages.remove(0);
        }
        Vec3 testPos = msg.getPosition(0);
        if (testPos == null) {
            return;
        }
        for (Message m : messages) {
            if (m.getPosition(0).distanceTo(testPos) < 1.05 && !force_position) {
                m.creationTime = 0;
            }
        }
        if (msg.msg == null || msg.msg.trim().length() == 0) {
            if (!(msg.show_item && msg.item != null)) {
                return;
            }
        }
        messages.add(msg);
    }

    @ForgeSubscribe
    public void renderMessages(RenderWorldLastEvent event) {
        doRenderMessages(event); // Forge events are too hard for eclipse to hot-swap?
    }

    void doRenderMessages(RenderWorldLastEvent event) {
        World w = Minecraft.getMinecraft().theWorld;
        if (w == null) {
            return;
        }
        if (messages.size() == 0) {
            return;
        }
        Iterator<Message> it = messages.iterator();
        long deathTime = System.currentTimeMillis() - 1000 * 6;
        EntityLivingBase camera = Minecraft.getMinecraft().renderViewEntity;
        double cx = camera.lastTickPosX + (camera.posX - camera.lastTickPosX) * (double) event.partialTicks;
        double cy = camera.lastTickPosY + (camera.posY - camera.lastTickPosY) * (double) event.partialTicks;
        double cz = camera.lastTickPosZ + (camera.posZ - camera.lastTickPosZ) * (double) event.partialTicks;
        GL11.glPushMatrix();
        GL11.glTranslated(-cx, -cy, -cz);
        GL11.glPushAttrib(GL11.GL_BLEND);

        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDepthMask(false);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glColor4f(1, 1, 1, 1);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        
        while (it.hasNext()) {
            Message m = it.next();
            if (m.creationTime < deathTime || m.world != w || !m.stillValid()) {
                it.remove();
                continue;
            }
            renderMessage(m, event.partialTicks);
        }
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    RenderItem renderItem = new RenderItem();

    private void renderMessage(Message m, float partial) {
        int width = 0;
        int height = 0;
        String[] lines = m.msg.split("\n");
        FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
        for (String line : lines) {
            height += fr.FONT_HEIGHT + 2;
            width = Math.max(width, fr.getStringWidth(line));
        }
        width += 2;

        float scaling = 1.6F / 60F;
        scaling *= 2F / 3F;
        GL11.glPushMatrix();
        
        int lineCount = lines.length;
        if (m.show_item) {
            lineCount = Math.max(2, lineCount);
        }

        Vec3 vec = m.getPosition(partial);
        float x = (float) vec.xCoord;
        float y = (float) vec.yCoord;
        float z = (float) vec.zCoord;
        Coord co = m.asCoordMaybe();
        if (co != null && !m.position_important) {
            AxisAlignedBB bb = co.getCollisionBoundingBoxFromPool();
            if (bb != null) {
                y += bb.maxY - bb.minY;
            } else {
                y += 0.5F;
            }
        }
        GL11.glTranslatef(x + 0.5F, y, z + 0.5F);
        float pvx = RenderManager.instance.playerViewX;
        float pvy = -RenderManager.instance.playerViewY;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings.thirdPersonView == 2) {
            pvx = -pvx;
        }
        GL11.glRotatef(pvy, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(pvx, 1.0F, 0.0F, 0.0F);
        GL11.glScalef(-scaling, -scaling, scaling);
        GL11.glTranslatef(0, -10 * lineCount, 0);

        Tessellator tess = Tessellator.instance;
        int var16 = (lineCount - 1) * 10;

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        tess.startDrawingQuads();
        int halfWidth = width / 2;
        double item_add = 0;
        if (m.show_item) {
            item_add += 24;
        }
        tess.setColorRGBA_F(0.0F, 0.0F, 0.0F, 0.125F);
        tess.addVertex((double) (-halfWidth - 1), (double) (-1), 0.0D);
        tess.addVertex((double) (-halfWidth - 1), (double) (8 + var16), 0.0D);
        tess.addVertex((double) (halfWidth + 1 + item_add), (double) (8 + var16), 0.0D);
        tess.addVertex((double) (halfWidth + 1 + item_add), (double) (-1), 0.0D);
        tess.draw();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        int i = 0;
        for (String line : lines) {
            fr.drawString(line, -fr.getStringWidth(line) / 2, 10 * i, -1);
            i++;
        }

        if (m.show_item) {
            TextureManager re = mc.renderEngine;
            RenderBlocks rb = mc.renderGlobal.globalRenderBlocks;
            
            GL11.glTranslatef((float) (halfWidth + 4), lineCount, 0);
            /*
            EntityItem entityitem = new EntityItem(mc.theWorld, 0.0D, 0.0D, 0.0D, m.item);
            entityitem.getEntityItem().stackSize = 1;
            entityitem.hoverStart = 0.0F;
            RenderItem.renderInFrame = true;
            float s = 40;
            GL11.glScalef(s, s, s);
            RenderManager.instance.renderEntityWithPosYaw(entityitem, 0.0D, 0.0D, 0.0D, 0.0F, 0.0F);
            RenderItem.renderInFrame = false;
            */
            renderItem.renderItemAndEffectIntoGUI(fr, re, m.item, 0, 0);
        }

        GL11.glPopMatrix();

    }
}
