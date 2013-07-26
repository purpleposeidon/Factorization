package factorization.client.gui;

import java.util.ArrayList;
import java.util.Iterator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.StatCollector;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.ForgeSubscribe;

import org.lwjgl.opengl.GL11;

import factorization.api.Coord;
import factorization.common.Core;
import factorization.common.FzConfig;

public class FactorizationNotify {
    static class Message {
        Object locus;
        World world;
        String msg;
        long creationTime;
        boolean position_important;
        ItemStack item;
        boolean show_item = false;

        Message set(Object locus, World world, String msg, boolean long_lasting, boolean position_important, ItemStack item) {
            creationTime = System.currentTimeMillis();
            this.world = world;
            if (long_lasting) {
                creationTime += 1000 * 5;
            }
            this.locus = locus;
            this.msg = msg;
            this.position_important = position_important;
            this.item = item;
            show_item = false;
            return this;
        }

        Vec3 getPosition() {
            if (locus instanceof Vec3) {
                return (Vec3) locus;
            }
            if (locus instanceof Entity) {
                Entity e = ((Entity) locus);
                return Vec3.createVectorHelper(e.posX, e.posY, e.posZ);
            }
            if (locus instanceof TileEntity) {
                TileEntity te = ((TileEntity) locus);
                return Vec3.createVectorHelper(te.xCoord, te.yCoord, te.zCoord);
            }
            if (locus instanceof Coord) {
                return ((Coord) locus).createVector();
            }
            return null;
        }

        boolean stillValid() {
            if (locus instanceof Entity) {
                Entity e = ((Entity) locus);
                return !e.isDead;
            }
            if (locus instanceof TileEntity) {
                TileEntity te = ((TileEntity) locus);
                return !te.isInvalid();
            }
            return true;
        }

        Coord asCoordMaybe() {
            if (locus instanceof Coord) {
                return (Coord) locus;
            }
            if (locus instanceof TileEntity) {
                return new Coord((TileEntity) locus);
            }
            return null;
        }
    }

    static ArrayList<Message> messages = new ArrayList();

    public static void addMessage(Object locus, ItemStack item, String format, String... args) {
        if (format.equals("!clear")) {
            messages.clear();
            return;
        }

        // Translate some things
        format = StatCollector.translateToLocal(format);
        for (int i = 0; i < args.length; i++) {
            String translated = StatCollector.translateToLocal(args[i]);
            args[i] = translated;
        }

        String item_name = "null", item_info = "", item_info_newline = "";
        if (item != null) {
            item_name = item.getDisplayName();
            EntityPlayer player = Minecraft.getMinecraft().thePlayer;
            ArrayList<String> bits = new ArrayList();
            item.getItem().addInformation(item, player, bits, false);
            boolean tail = false;
            for (String s : bits) {
                if (tail) {
                    item_info += "\n";
                }
                tail = true;
                item_info += s;
            }
            item_info_newline = "\n" + item_info;
        }
        String itemString = item != null ? item.getDisplayName() : "null";

        String[] cp = new String[args.length + 3];
        for (int i = 0; i < args.length; i++) {
            cp[i] = args[i];
        }
        cp[args.length] = item_name;
        cp[args.length + 1] = item_info;
        cp[args.length + 2] = item_info_newline;
        // format = format.replace("{ITEM_NAME}", "%" + (args.length + 0) +
        // "$s");
        format = format.replace("{ITEM_NAME}", "%" + (args.length + 1) + "$s");
        format = format.replace("{ITEM_INFOS}", "%" + (args.length + 2) + "$s");
        format = format.replace("{ITEM_INFOS_NEWLINE}", "%" + (args.length + 3) + "$s");

        String msg;
        try {
            msg = String.format(format, (Object[]) cp);
        } catch (Exception e) {
            e.printStackTrace();
            msg = "FORMAT ERROR\n" + format;
        }

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
        if (FzConfig.notify_in_chat) {
            EntityPlayer player = Minecraft.getMinecraft().thePlayer;
            if (player != null) {
                player.addChatMessage(msg);
            }
            return;
        }
        if (messages.size() > 2 && !force_position) {
            messages.remove(0);
        }
        Minecraft mc = Minecraft.getMinecraft();
        World w = mc.theWorld;
        Message testMessage = new Message().set(locus, w, msg, long_lasting, force_position, item);
        Vec3 testPos = testMessage.getPosition();
        if (testPos == null) {
            return;
        }
        for (Message m : messages) {
            if (m.locus.equals(locus)) {
                m.set(locus, w, msg, long_lasting, force_position, item);
                return;
            } else if (m.getPosition().distanceTo(testPos) < 1.05 && !force_position) {
                m.creationTime = 0;
            }
        }
        messages.add(testMessage);
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
        Core.profileStart("factorizationNotify");
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
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        while (it.hasNext()) {
            Message m = it.next();
            if (m.creationTime < deathTime || m.world != w || !m.stillValid()) {
                it.remove();
                continue;
            }
            renderMessage(m);
        }
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        GL11.glPopMatrix();
        GL11.glPopAttrib();
        Core.profileEnd();
    }

    RenderItem renderItem = new RenderItem();

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

        float scaling = 1.6F / 60F;
        scaling *= 2F / 3F;
        GL11.glPushMatrix();
        
        int lineCount = lines.length;
        if (m.show_item) {
            lineCount = Math.max(2, lineCount);
        }

        Vec3 vec = m.getPosition();
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
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-RenderManager.instance.playerViewY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(RenderManager.instance.playerViewX, 1.0F, 0.0F, 0.0F);
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
        tess.setColorRGBA_F(0.0F, 0.0F, 0.0F, 0.5F);
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
            Minecraft mc = Minecraft.getMinecraft();
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
