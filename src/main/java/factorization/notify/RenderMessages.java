package factorization.notify;

import factorization.api.ISaneCoord;
import factorization.util.RenderUtil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class RenderMessages extends RenderMessagesProxy {
    static List<ClientMessage> messages = Collections.synchronizedList(new ArrayList<ClientMessage>());
    
    {
        NotifyImplementation.loadBus(this);
        ClientCommandHandler.instance.registerCommand(new PointCommand());
    }

    @Override
    public void addMessage(Object locus, ItemStack item, String format, String... args) {
        addMessage0(locus, item, format, args);
    }

    private void addMessage0(Object locus, ItemStack item, String format, String... args) {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player == null || player.worldObj == null) {
            return;
        }
        ClientMessage msg = new ClientMessage(player.worldObj, locus, item, format, args);
        if (msg.style.contains(Style.CLEAR)) {
            messages.clear();
            if (msg.msg == null || msg.msg.equals("")) return;
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
    
    private void updateMessage(ClientMessage update) {
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
        synchronized (messages) {
            doRenderMessages(event);
        }
    }
    
    void doRenderMessages(RenderWorldLastEvent event) {
        World w = Minecraft.getMinecraft().theWorld;
        if (w == null) {
            return;
        }
        if (messages.size() == 0) {
            return;
        }
        RenderUtil.checkGLError("A mod has a rendering error");
        Iterator<ClientMessage> it = messages.iterator();
        long approximateNow = System.currentTimeMillis();
        Entity camera = Minecraft.getMinecraft().getRenderViewEntity();
        double cx = camera.lastTickPosX + (camera.posX - camera.lastTickPosX) * (double) event.partialTicks;
        double cy = camera.lastTickPosY + (camera.posY - camera.lastTickPosY) * (double) event.partialTicks;
        double cz = camera.lastTickPosZ + (camera.posZ - camera.lastTickPosZ) * (double) event.partialTicks;
        GlStateManager.pushMatrix();
        GL11.glPushAttrib(GL11.GL_BLEND);
        GlStateManager.translate(-cx, -cy, -cz);

        GlStateManager.depthMask(false);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1, 1, 1, 1);

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
            GlStateManager.disableLighting();
            float lifeLeft = (m.lifeTime - timeExisted)/1000F;
            float opacity = 1F;
            if (lifeLeft < 1) {
                opacity = lifeLeft / 1F;
            }
            opacity = (float) Math.sin(opacity);
            if (opacity > 0.12) {
                renderMessage(m, event.partialTicks, opacity, cx, cy, cz);
            }
        }
        GlStateManager.enableLighting();
        GlStateManager.color(1, 1, 1, 1);

        GlStateManager.popMatrix();
        GlStateManager.popAttrib();
        RenderUtil.checkGLError("Notification render error!");
    }

    RenderItem renderItem = Minecraft.getMinecraft().getRenderItem();

    private void renderMessage(ClientMessage m, float partial, float opacity, double cx, double cy, double cz) {
        int width = 0;
        String[] lines = m.msg.split("\n");
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        for (String line : lines) {
            width = Math.max(width, fr.getStringWidth(line));
        }
        width += 2;
        int halfWidth = width / 2;

        float scaling = 1.6F / 60F;
        scaling *= 2F / 3F;
        GlStateManager.pushMatrix();

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
        if (m.style.contains(Style.SCALE_SIZE)) {
            double dx = x - cx;
            double dy = y - cy;
            double dz = z - cz;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            scaling *= Math.sqrt(dist);
        }
        
        ISaneCoord co = m.asCoordMaybe();
        if (co != null && !m.position_important) {
            BlockPos pos = co.toBlockPos();
            IBlockState bs = co.w().getBlockState(pos);
            AxisAlignedBB bb = bs.getBlock().getCollisionBoundingBox(co.w(), pos, bs);
            if (bb != null) {
                y += bb.maxY - bb.minY;
            } else {
                y += 0.5F;
            }
        }
        GlStateManager.translate(x + 0.5F, y, z + 0.5F);
        Minecraft mc = Minecraft.getMinecraft();
        float pvx = mc.getRenderManager().playerViewX;
        float pvy = -mc.getRenderManager().playerViewY;
        if (mc.gameSettings.thirdPersonView == 2) {
            pvx = -pvx;
        }
        GlStateManager.rotate(pvy, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(pvx, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-scaling, -scaling, scaling);
        GlStateManager.translate(0, -10 * lineCount, 0);
        
        {
            int lineHeight = (lineCount - 1) * 10;

            double item_add = 0;
            if (m.show_item) {
                item_add += 24;
            }
            float c = 0.0F;
            GlStateManager.disableTexture2D();
            GlStateManager.color(c, c, c, Math.min(opacity, 0.2F));
            double Z = 0.001D;
            // TODO: Why didn't the tessellator work?
            // TODO: Use 2 tessellator + 2 draw calls to do all notice rendering
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex3d(-halfWidth - 1, -1, Z);
            GL11.glVertex3d(-halfWidth - 1, 8 + lineHeight, Z);
            GL11.glVertex3d(halfWidth + 1 + item_add, 8 + lineHeight, Z);
            GL11.glVertex3d(halfWidth + 1 + item_add, -1, Z);
            GL11.glEnd();
            GlStateManager.enableTexture2D();
        }

        {
            int i = 0;
            int B = (int) (0xFF * Math.min(1, 0.5F + opacity));
            int color = (B << 16) + (B << 8) + B + ((int) (0xFF*opacity) << 24);
            GlStateManager.translate(0, centeringOffset, 0);
            for (String line : lines) {
                fr.drawString(line, -fr.getStringWidth(line) / 2, 10 * i, color);
                i++;
            }
        }
        {
            if (m.show_item) {
                //GL11.glColor4f(opacity, opacity, opacity, opacity);
                // :| Friggin' resets the transparency don't it...
                GlStateManager.translate(0, -centeringOffset, 0);

                GlStateManager.translate((float) (halfWidth + 4), -lineCount/2, 0);
                renderItem.zLevel -= 50;
                GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
                renderItem.renderItemAndEffectIntoGUI(m.item, 0, 0);
                GL11.glPopAttrib();
                renderItem.zLevel += 50;
            }
        }
        GlStateManager.popMatrix();

    }
    
    @Override
    public void onscreen(String message, String[] formatArgs) {
        Minecraft mc = Minecraft.getMinecraft();
        Object targs[] = new Object[formatArgs.length];
        for (int i = 0; i < formatArgs.length; i++) {
            targs[i] = StatCollector.translateToLocal(formatArgs[i]);
        }
        String msg = I18n.format(message, targs);
        mc.ingameGUI.setRecordPlaying(msg, false);
    }
    
    @Override
    public void replaceable(IChatComponent msg, int msgKey) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(msg, msgKey);
    }
}
