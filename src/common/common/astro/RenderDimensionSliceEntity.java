package factorization.common.astro;

import static org.lwjgl.opengl.GL11.glCallList;
import static org.lwjgl.opengl.GL11.glColor3f;
import static org.lwjgl.opengl.GL11.glGetError;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;
import static org.lwjgl.opengl.GL11.glRotatef;
import static org.lwjgl.opengl.GL11.glScalef;
import static org.lwjgl.opengl.GL11.glTranslatef;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.src.Chunk;
import net.minecraft.src.ChunkCoordinates;
import net.minecraft.src.Entity;
import net.minecraft.src.GLAllocation;
import net.minecraft.src.Render;
import net.minecraft.src.RenderHelper;
import net.minecraft.src.RenderManager;
import net.minecraft.src.TileEntity;
import net.minecraft.src.TileEntityRenderer;
import net.minecraft.src.World;
import net.minecraft.src.WorldRenderer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.WorldEvent;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import cpw.mods.fml.common.IScheduledTickHandler;
import cpw.mods.fml.common.TickType;
import factorization.common.Core;


public class RenderDimensionSliceEntity extends Render implements IScheduledTickHandler {
    void checkGLError(String op) {
        int var2 = glGetError();

        if (var2 != 0)
        {
            String var3 = GLU.gluErrorString(var2);
            System.out.println("########## GL ERROR ##########");
            System.out.println("@ " + op);
            System.out.println(var2 + ": " + var3);
        }
    }
    
    Set<DSRenderInfo> renderInfoTracker = new HashSet();
    long megatickCount = 0;
    
    class DSRenderInfo {
        int renderCounts = 0;
        long lastRenderInMegaticks = megatickCount;
        boolean dirty = false;
        private int renderList = -1;
        WorldRenderer worldRenderer = null;
        
        int getRenderList() {
            if (renderList == -1) {
                renderList = GLAllocation.generateDisplayLists(3);
                renderInfoTracker.add(this);
            }
            return renderList;
        }
        
        void discardRenderList() {
            if (renderList != -1) {
                GLAllocation.deleteDisplayLists(renderList);
                renderList = -1;
            }
            worldRenderer = null;
        }
    }
    
    int nest = 0;
    @Override
    public void doRender(Entity ent, double x, double y, double z, float yaw, float partialTicks) {
        //XXX TODO: Don't render if we're far away! (This should maybe be done in some other function?)
        if (ent.isDead) {
            return;
        }
        DimensionSliceEntity we = (DimensionSliceEntity) ent;
        if (we.renderInfo == null) {
            we.renderInfo = new DSRenderInfo();
        }
        DSRenderInfo renderInfo = (DSRenderInfo) we.renderInfo;
        if (nest == 0) {
            Core.profileStart("fzds");
            renderInfo.lastRenderInMegaticks = megatickCount;
        } else if (nest == 1) {
            Core.profileStart("recursion");
        }
        nest++;
        glPushMatrix();
        try {
            //World subWorld = DimensionManager.getWorld(Core.dimension_slice_dimid); // we.wew;
            //subWorld = DimensionManager.getWorld(0);
            World subWorld = DimensionManager.getWorld(0); // we.wew;
            subWorld = HammerManager.getClientWorld();
            if (subWorld == null) {
                //Huh. Lame.
                subWorld = we.worldObj;
            }
            //subWorld = we.worldObj;
            WorldRenderer wr = renderInfo.worldRenderer;
            checkGLError("FZDS before render");
            if (wr == null) {
                wr = new WorldRenderer(subWorld, subWorld.loadedTileEntityList, 0, 0, 0, renderInfo.getRenderList());
                renderInfo.worldRenderer = wr;
                checkGLError("FZDS render list");
            }
            wr.needsUpdate = renderInfo.renderCounts == 0;
            if (nest == 1) {
                Core.profileStart("build");
                wr.updateRenderer();
                Core.profileEnd();
            }
            float s = 1F/16F;
            glTranslatef((float)x, (float)y, (float)z);
            //glRotatef(45, 1, 1, 0);
            glScalef(s, s, s);
            wr.isInFrustum = true;
            RenderHelper.disableStandardItemLighting();
            if (Minecraft.getMinecraft().isAmbientOcclusionEnabled() && Core.dimension_slice_allow_smooth) {
                GL11.glShadeModel(GL11.GL_SMOOTH);
            }
            for (int pass = 0; pass < 2; pass++) {
                int displayList = wr.getGLCallListForPass(pass);
                if (displayList >= 0) {
                    loadTexture("/terrain.png");
                    glCallList(displayList);
                }
            }
            RenderHelper.enableStandardItemLighting();
            
            //glRotatef(-45, 1, 0, 0);
            glTranslatef((float)-x, (float)-y, (float)-z);
            glTranslatef((float)we.posX, (float)we.posY, (float)we.posZ);
            //Maybe we should use RenderGlobal.renderEntities ???
            Chunk here = subWorld.getChunkFromBlockCoords(0, 0);
            for (int i1 = 0; i1 < here.entityLists.length; i1++) {
                List<Entity> ents = here.entityLists[i1];
                for (int i2 = 0; i2 < ents.size(); i2++) {
                    Entity e = ents.get(i2);
                    if (e instanceof DimensionSliceEntity && nest >= 3) {
                        continue;
                    }
                    RenderManager.instance.renderEntity(e, partialTicks);
                }
            }
            RenderHelper.enableStandardItemLighting();
            for (TileEntity te : ((Map<ChunkCoordinates, TileEntity>)here.chunkTileEntityMap).values()) {
                //I warned you about comods, bro! I told you, dawg!
                TileEntityRenderer.instance.renderTileEntity(te, partialTicks);
            }
            checkGLError("FZDS after render");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            glPopMatrix();
            nest--;
            if (nest == 0) {
                renderInfo.renderCounts = (1 + renderInfo.renderCounts) % 60;
                Core.profileEnd();
            } else if (nest == 1) {
                Core.profileEnd();
            }
        }
    }
    
    void discardOldRenderLists() {
        //discard unused renderlists
        //The display list will be deallocated if it hasn't been used recently.
        Iterator<DSRenderInfo> it = renderInfoTracker.iterator();
        while (it.hasNext()) {
            DSRenderInfo renderInfo = it.next();
            if (renderInfo.lastRenderInMegaticks < megatickCount - 1) {
                renderInfo.discardRenderList();
                it.remove();
            }
        }
    }
    
    @ForgeSubscribe
    public void worldChanged(WorldEvent.Unload unloadEvent) {
        //This only happens when a local server is unloaded.
        //This probably happens on a different thread, so let the usual tick handler clean it up.
        megatickCount += 100;
    }

    @Override
    public void tickStart(EnumSet<TickType> type, Object... tickData) {
        megatickCount++;
        if (nest != 0) {
            nest = 0;
            Core.logFine("FZDS render nesting depth was not 0");
        }
    }

    @Override
    public void tickEnd(EnumSet<TickType> type, Object... tickData) {
        discardOldRenderLists();
    }

    EnumSet<TickType> renderTicks = EnumSet.of(TickType.RENDER);
    @Override
    public EnumSet<TickType> ticks() {
        return renderTicks;
    }

    @Override
    public String getLabel() {
        return "fzdsRenderDealloc";
    }

    @Override
    public int nextTickSpacing() {
        return 20*60;
        //20*60 would be "every minute". This actually isn't quite correct, since MC doesn't render at 20 FPS.
        //I mean, other people's MC doesn't render at 20 FPS. So, let's say you're getting 60 FPS.
    }
}
