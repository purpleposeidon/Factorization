package factorization.notify;

import java.util.ArrayList;
import java.util.Iterator;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class RenderMessages extends RenderMessagesProxy {
    static ArrayList<ClientMessage> messages = new ArrayList();
    
    {
        NotifyImplementation.loadBus(this);
        ClientCommandHandler.instance.registerCommand(new PointCommand());
    }
    
    @Override
    public void addMessage(Object locus, ItemStack item, String format, String... args) {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player == null || player.worldObj == null) {
            return;
        }
        ClientMessage msg = new ClientMessage(player.worldObj, locus, item, format, args);
        if (msg.style.contains(Style.CLEAR)) {
            messages.clear();
            return;
        }
        if (msg.style.contains(Style.UPDATE) || msg.style.contains(Style.UPDATE_SAME_ITEM)) {
            updateMessage(msg);
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
        for (ClientMessage m : messages) {
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
    
    void updateMessage(ClientMessage update) {
        for (ClientMessage msg : messages) {
            if (!msg.locus.equals(update.locus)) {
                continue;
            }
            if (!update.style.contains(Style.UPDATE_SAME_ITEM)) {
                msg.item = update.item;
            }
            msg.msg = update.msg;
            return;
        }
        // Otherwise it's an UPDATE to a non-existing message.
        // Presumably it's to a message that's died already.
    }

    @SubscribeEvent
    public void renderMessages(RenderWorldLastEvent event) {
        doRenderMessages(event); // Forge events are too hard for eclipse to hot-swap?
    }
    
    void doRenderMessages(RenderWorldLastEvent event) {
        World w = Minecraft.getMinecraft().theWorld;
        if (w == null) {
            return;
        }
        /*if (messages.size() == 0) {
            return;
        }*/ // NORELEASE
        Iterator<ClientMessage> it = messages.iterator();
        long approximateNow = System.currentTimeMillis();
        EntityLivingBase camera = Minecraft.getMinecraft().renderViewEntity;
        double cx = camera.lastTickPosX + (camera.posX - camera.lastTickPosX) * (double) event.partialTicks;
        double cy = camera.lastTickPosY + (camera.posY - camera.lastTickPosY) * (double) event.partialTicks;
        double cz = camera.lastTickPosZ + (camera.posZ - camera.lastTickPosZ) * (double) event.partialTicks;
        GL11.glPushMatrix();
        GL11.glTranslated(-cx, -cy, -cz);
        GL11.glPushAttrib(GL11.GL_BLEND);
        
        GL11.glDepthMask(false);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1, 1, 1, 1);
        
        while (it.hasNext()) {
            ClientMessage m = it.next();
            long timeExisted = approximateNow - m.creationTime;
            if (timeExisted > m.lifeTime || m.world != w || !m.stillValid()) {
                it.remove();
                continue;
            }
            if (!m.style.contains(Style.DRAWFAR)) {
                Vec3 pos = m.getPosition(event.partialTicks);
                double dist = camera.getDistance(pos.xCoord, pos.yCoord, pos.zCoord);
                if (dist > 8) {
                    continue;
                }
            }
            GL11.glDisable(GL11.GL_LIGHTING);
            float lifeLeft = (m.lifeTime - timeExisted)/1000F;
            float opacity = 1F;
            if (lifeLeft < 1) {
                opacity = lifeLeft / 1F;
            }
            opacity = (float) Math.sin(opacity);
            if (opacity > 0.12) {
                renderMessage(m, event.partialTicks, opacity);
            }
        }
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glColor4f(1, 1, 1, 1);

        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    RenderItem renderItem = new RenderItem();

    private void renderMessage(ClientMessage m, float partial, float opacity) {
        int width = 0;
        String[] lines = m.msg.split("\n");
        FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
        for (String line : lines) {
            width = Math.max(width, fr.getStringWidth(line));
        }
        width += 2;
        int halfWidth = width / 2;

        float scaling = 1.6F / 60F;
        scaling *= 2F / 3F;
        GL11.glPushMatrix();
        
        int lineCount = lines.length;
        float centeringOffset = 0;
        if (m.show_item) {
            if (lineCount == 1) {
                centeringOffset = 5F;
            }
            lineCount = Math.max(2, lineCount);
        }

        Vec3 vec = m.getPosition(partial);
        float x = (float) vec.xCoord;
        float y = (float) vec.yCoord;
        float z = (float) vec.zCoord;
        ISaneCoord co = m.asCoordMaybe();
        if (co != null && !m.position_important) {
            Block b = co.w().getBlock(co.x(), co.y(), co.z());
            AxisAlignedBB bb = b.getCollisionBoundingBoxFromPool(co.w(), co.x(), co.y(), co.z());
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
        
        {
            Tessellator tess = Tessellator.instance;
            int var16 = (lineCount - 1) * 10;

            GL11.glDisable(GL11.GL_TEXTURE_2D);
            tess.startDrawingQuads();
            double item_add = 0;
            if (m.show_item) {
                item_add += 24;
            }
            float c = 0.0F;
            tess.setColorRGBA_F(c, c, c, Math.min(opacity, 0.2F));
            tess.addVertex((double) (-halfWidth - 1), (double) (-1), 0.0D);
            tess.addVertex((double) (-halfWidth - 1), (double) (8 + var16), 0.0D);
            tess.addVertex((double) (halfWidth + 1 + item_add), (double) (8 + var16), 0.0D);
            tess.addVertex((double) (halfWidth + 1 + item_add), (double) (-1), 0.0D);
            tess.draw();
            GL11.glEnable(GL11.GL_TEXTURE_2D);
        }

        {
            int i = 0;
            int B = (int) (0xFF * Math.min(1, 0.5F + opacity));
            int color = (B << 16) + (B << 8) + B + ((int) (0xFF*opacity) << 24);
            GL11.glTranslatef(0, centeringOffset, 0);
            for (String line : lines) {
                fr.drawString(line, -fr.getStringWidth(line) / 2, 10 * i, color);
                i++;
            }
        }
        {
            if (m.show_item) {
                //GL11.glColor4f(opacity, opacity, opacity, opacity);
                // :| Friggin' resets the transparency don't it...
                GL11.glTranslatef(0, -centeringOffset, 0);
                TextureManager re = mc.renderEngine;
                
                GL11.glTranslatef((float) (halfWidth + 4), -lineCount/2, 0);
                renderItem.zLevel -= 50;
                renderItem.renderItemAndEffectIntoGUI(fr, re, m.item, 0, 0);
                renderItem.zLevel += 50;
            }
        }
        GL11.glPopMatrix();

    }
    
    @Override
    public void onscreen(String message, String[] formatArgs) {
        Minecraft mc = Minecraft.getMinecraft();
        String msg = I18n.format(message, (Object[]) formatArgs);
        mc.ingameGUI.func_110326_a(msg, false);
    }
}
