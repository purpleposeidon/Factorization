package factorization.client.gui;

import java.util.ArrayList;
import java.util.Iterator;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.src.AxisAlignedBB;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.ModelBase;
import net.minecraft.src.RenderLiving;
import net.minecraft.src.RenderManager;
import net.minecraft.src.StatCollector;
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
            args[i] = StatCollector.translateToLocal(args[i]);
        }
        String msg = String.format(format, (Object[]) args);
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
    
    static class NameEntity extends EntityLiving {

        public NameEntity(World par1World) {
            super(par1World);
        }

        @Override
        public int getMaxHealth() {
            return 0;
        }
        
    }
    
    static class NameEntityRenderer extends RenderLiving {

        public NameEntityRenderer(ModelBase par1ModelBase, float par2) {
            super(par1ModelBase, par2);
        }
        
        void renderLabel(NameEntity ent, String label) {
            renderManager = RenderManager.instance;
            this.renderLivingLabel(ent, label, ent.posX, ent.posY, ent.posZ, 27);
        }
        
    }
    
    static NameEntityRenderer ner = new NameEntityRenderer(null, 0);
    
    @ForgeSubscribe
    public void renderMessages(RenderWorldLastEvent event) {
        doRender(event); //Forge events are too hard for eclipse to hot-swap?
    }
    
    void doRender(RenderWorldLastEvent event) {
        World w = Minecraft.getMinecraft().theWorld;
        if (w == null) {
            return;
        }
        if (messages.size() == 0) {
            return;
        }
        Core.profileStart("factorizationNotify");
        NameEntity namer = new NameEntity(w);
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
            
            namer.posX = m.locus.x + 0.5;
            namer.posY = m.locus.y - 2;
            
            
            double boundAdd = 0;
            AxisAlignedBB bb = m.locus.getCollisionBoundingBoxFromPool();
            if (bb != null) {
                boundAdd = bb.maxY - bb.minY;
            }
            for (DeltaCoord dc : DeltaCoord.directNeighbors) {
                if (dc.y != 0) {
                    continue;
                }
                Coord here = m.locus.add(dc);
                bb = here.getCollisionBoundingBoxFromPool();
                if (bb != null) {
                    boundAdd = Math.max(boundAdd, bb.maxY - bb.minY);
                }
            }
            
            namer.posY += boundAdd;
            namer.posZ = m.locus.z + 0.5;
            String[] lines = m.msg.split("\n");
            namer.posY += (lines.length - 1)*0.25;
            for (String line : lines) {
                ner.renderLabel(namer, line);
                namer.posY -= 0.25F;
            }
        }
        GL11.glPopMatrix();
        GL11.glPopAttrib();
        Core.profileEnd();
    }
}
