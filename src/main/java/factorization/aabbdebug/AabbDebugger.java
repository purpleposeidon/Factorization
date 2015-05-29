package factorization.aabbdebug;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import factorization.api.Coord;
import factorization.shared.Core;
import factorization.util.SpaceUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public enum AabbDebugger {
    INSTANCE;
    
    private AabbDebugger() {
        Core.loadBus(this);
        ClientCommandHandler.instance.registerCommand(new ICommand() {
            public int compareTo(ICommand other) {
                return this.getCommandName().compareTo(other.getCommandName());
            }

            @Override
            public int compareTo(Object obj) {
                return this.compareTo((ICommand) obj);
            }

            @Override
            public String getCommandName() {
                return "boxdbg";
            }

            @Override
            public String getCommandUsage(ICommandSender p_71518_1_) {
                return "/bxdbg freeze|thaw|clean";
            }

            @Override
            public List getCommandAliases() {
                return null;
            }

            @Override
            public void processCommand(ICommandSender player, String[] args) {
                String arg0 = args.length > 0 ? args[0] : "help";
                if (arg0.equals("freeze")) {
                    freeze = true;
                } else if (arg0.equals("thaw")) {
                    freeze = false;
                } else if (arg0.equals("clean")) {
                    frozen.clear();
                    frozen_lines.clear();
                } else {
                    player.addChatMessage(new ChatComponentText(getCommandUsage(player)));
                }
            }

            @Override public boolean canCommandSenderUseCommand(ICommandSender p_71519_1_) { return true; }
            @Override public List addTabCompletionOptions(ICommandSender p_71516_1_, String[] p_71516_2_) { return null; }
            @Override public boolean isUsernameIndex(String[] p_82358_1_, int p_82358_2_) { return false; }
            
        });
    }
    
    private static class Line {
        Vec3 start, end;
    }

    static <T> List<T> list() {
        return Collections.synchronizedList(new ArrayList<T>());
    }
    
    static final List<AxisAlignedBB> boxes = list(), frozen = list();
    static final List<Line> lines = list(), frozen_lines = list();
    static boolean freeze = false;
    
    public static void addBox(AxisAlignedBB box) {
        if (box == null) return;
        boxes.add(box.copy());
    }

    public static void addBox(Coord c) {
        if (c == null) return;
        addBox(SpaceUtil.createAABB(c, c.add(1, 1, 1)));
    }

    public static void addLine(Vec3 start, Vec3 end) {
        Line line = new Line();
        line.start = SpaceUtil.copy(start);
        line.end = SpaceUtil.copy(end);
        lines.add(line);
    }
    
    @SubscribeEvent
    public void clearBox(ClientTickEvent event) {
        if (event.phase == Phase.START) {
            if (freeze) {
                if (!boxes.isEmpty() || !lines.isEmpty()) {
                    frozen.clear();
                    frozen_lines.clear();
                    frozen.addAll(boxes);
                    frozen_lines.addAll(lines);
                }
            }
            boxes.clear();
            lines.clear();
        }
    }
    
    boolean hasBoxes() {
        return !frozen.isEmpty() || !boxes.isEmpty() || !lines.isEmpty() || !frozen_lines.isEmpty();
    }
    
    @SubscribeEvent
    public void drawBoxes(RenderWorldLastEvent event) {
        if (!hasBoxes()) return;
        World w = Minecraft.getMinecraft().theWorld;
        if (w == null) return;
        EntityLivingBase eyePos = Minecraft.getMinecraft().renderViewEntity;
        double cx = eyePos.lastTickPosX + (eyePos.posX - eyePos.lastTickPosX) * (double) event.partialTicks;
        double cy = eyePos.lastTickPosY + (eyePos.posY - eyePos.lastTickPosY) * (double) event.partialTicks;
        double cz = eyePos.lastTickPosZ + (eyePos.posZ - eyePos.lastTickPosZ) * (double) event.partialTicks;
        
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT);
        GL11.glPushMatrix();
        
        GL11.glTranslated(-cx, -cy, -cz);
        GL11.glDepthMask(false);
        //GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glColor4f(1, 1, 1, 0.5F);
        
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glLineWidth(4);
        synchronized (boxes) {
            for (AxisAlignedBB box : boxes) {
                RenderGlobal.drawOutlinedBoundingBox(box, 0x800000);
            }
        }
        synchronized (frozen) {
            for (AxisAlignedBB box : frozen) {
                RenderGlobal.drawOutlinedBoundingBox(box, 0x4040B0);
            }
        }
        GL11.glLineWidth(2);
        GL11.glColor4f(1, 1, 0, 1);
        GL11.glBegin(GL11.GL_LINES);
        synchronized (lines) {
            for (Line line : lines) {
                GL11.glVertex3d(line.start.xCoord, line.start.yCoord, line.start.zCoord);
                GL11.glVertex3d(line.end.xCoord, line.end.yCoord, line.end.zCoord);
            }
        }
        GL11.glEnd();
        GL11.glColor4f(0, 1, 1, 1);
        GL11.glBegin(GL11.GL_LINES);
        synchronized (frozen_lines) {
            for (Line line : frozen_lines) {
                GL11.glVertex3d(line.start.xCoord, line.start.yCoord, line.start.zCoord);
                GL11.glVertex3d(line.end.xCoord, line.end.yCoord, line.end.zCoord);
            }
        }
        GL11.glEnd();
        GL11.glDepthMask(true);
        
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }
}
