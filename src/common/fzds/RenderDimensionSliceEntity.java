package factorization.fzds;

import static org.lwjgl.opengl.GL11.*;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.entity.Entity;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.world.World;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.world.WorldEvent;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import cpw.mods.fml.common.IScheduledTickHandler;
import cpw.mods.fml.common.TickType;
import factorization.api.Coord;
import factorization.common.Core;


public class RenderDimensionSliceEntity extends Render implements IScheduledTickHandler {
    static void checkGLError(String op) {
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
    static long megatickCount = 0;
    
    class DSRenderInfo {
        final int width = Hammer.cellWidth;
        final int height = 4;
        final int cubicChunkCount = width*width*height;
        final int wr_display_list_size = 3; //how many display lists a WorldRenderer uses
        
        int renderCounts = 0;
        long lastRenderInMegaticks = megatickCount;
        boolean dirty = false;
        private int renderList = -1;
        WorldRenderer renderers[] = new WorldRenderer[cubicChunkCount];
        Coord corner;
        
        public DSRenderInfo(Coord corner) {
            this.corner = corner;
            int xzSize = width*width;
            int i = 0;
            checkGLError("FZDS before render");
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    for (int z = 0; z < width; z++) {
                        //We could allocate lists per WR instead?
                        renderers[i] = new WorldRenderer(corner.w, corner.w.loadedTileEntityList, corner.x + x*16, corner.y + y*16, corner.z + z*16, getRenderList() + i*wr_display_list_size);
                        renderers[i].posXClip = x*16;
                        renderers[i].posYClip = y*16;
                        renderers[i].posZClip = z*16;
                        checkGLError("FZDS WorldRenderer init");
                        i++;
                    }
                }
            }
        }
        
        void update() {
            Core.profileStart("update");
            if (renderCounts != 0) {
                return;
            }
            checkGLError("FZDS before WorldRender update");
            for (int i = 0; i < renderers.length; i++) {
                renderers[i].needsUpdate = true;
                renderers[i].updateRenderer();
                checkGLError("FZDS WorldRender update");
            }
            Core.profileEnd();
        }
        
        void renderTerrain() {
            RenderHelper.disableStandardItemLighting();
            if (Minecraft.getMinecraft().isAmbientOcclusionEnabled() && Core.dimension_slice_allow_smooth) {
                GL11.glShadeModel(GL11.GL_SMOOTH);
            }
            
            for (int pass = 0; pass < 2; pass++) {
                for (int i = 0; i < renderers.length; i++) {
                    WorldRenderer wr = renderers[i];
                    wr.isInFrustum = true; //XXX might not be necessary
                    int displayList = wr.getGLCallListForPass(pass);
                    if (displayList >= 0) {
                        loadTexture("/terrain.png");
                        glCallList(displayList);
                    }
                }
            }
        }
        
        void renderEntities(float partialTicks) {
            RenderHelper.enableStandardItemLighting();
            //Maybe we should use RenderGlobal.renderEntities ???
            double sx = TileEntityRenderer.instance.playerX;
            double sy = TileEntityRenderer.instance.playerY;
            double sz = TileEntityRenderer.instance.playerZ;
            for (int cdx = 0; cdx < width; cdx++) {
                for (int cdz = 0; cdz < width; cdz++) {
                    Chunk here = corner.w.getChunkFromBlockCoords(corner.x + cdx*16, corner.z + cdz*16);
                    for (int i1 = 0; i1 < here.entityLists.length; i1++) {
                        List<Entity> ents = here.entityLists[i1];
                        for (int i2 = 0; i2 < ents.size(); i2++) {
                            Entity e = ents.get(i2);
                            if (e instanceof DimensionSliceEntity && nest >= 3) {
                                continue;
                            }
                            //if e is a proxying player, don't render it?
                            RenderManager.instance.renderEntity(e, partialTicks);
                        }
                    }
                    for (TileEntity te : ((Map<ChunkCoordinates, TileEntity>)here.chunkTileEntityMap).values()) {
                        //I warned you about comods, bro! I told you, dawg!
                        //(Shouldn't actually be a problem if we're rendering properly)
                        //Since we don't know the actual distance from the player to the TE, we need to cheat.
                        TileEntityRenderer.instance.playerX = te.xCoord;
                        TileEntityRenderer.instance.playerY = te.yCoord;
                        TileEntityRenderer.instance.playerZ = te.zCoord;
                        TileEntityRenderer.instance.renderTileEntity(te, partialTicks);
                    }
                }
            }
            TileEntityRenderer.instance.playerX = sx;
            TileEntityRenderer.instance.playerY = sy;
            TileEntityRenderer.instance.playerZ = sz;
        }
        
        int getRenderList() {
            if (renderList == -1) {
                renderList = GLAllocation.generateDisplayLists(wr_display_list_size*cubicChunkCount);
                renderInfoTracker.add(this);
            }
            return renderList;
        }
        
        void discardRenderList() {
            if (renderList != -1) {
                GLAllocation.deleteDisplayLists(renderList);
                renderList = -1;
            }
            Arrays.fill(renderers, (WorldRenderer) null);
        }
    }
    
    public static int nest = 0;
    @Override
    public void doRender(Entity ent, double x, double y, double z, float yaw, float partialTicks) {
        //XXX TODO: Don't render if we're far away! (This should maybe be done in some other function?)
        if (ent.isDead) {
            return;
        }
        DimensionSliceEntity we = (DimensionSliceEntity) ent;
        if (we.renderInfo == null) {
            //we.renderInfo = new DSRenderInfo(new Coord(Minecraft.getMinecraft().theWorld, 0, 0, 0)); //The real world
            we.renderInfo = new DSRenderInfo(Hammer.getCellCorner(we.cell)); //The shadow world
        }
        DSRenderInfo renderInfo = (DSRenderInfo) we.renderInfo;
        if (nest == 0) {
            Core.profileStart("fzds");
            renderInfo.lastRenderInMegaticks = megatickCount;
        } else if (nest == 1) {
            Core.profileStart("recursion");
        }
        
        nest++;
        try {
            if (nest == 1) {
                Core.profileStart("build");
                Hammer.proxy.setClientWorld(Hammer.getClientShadowWorld());
                try {
                    renderInfo.update();
                } finally {
                    Hammer.proxy.restoreClientWorld();
                    Core.profileEnd();
                }
            }
            glPushMatrix();
            try {
                float s = 1/4F;
                glTranslatef((float)x, (float)y, (float)z);
                glScalef(s, s, s);
                renderInfo.renderTerrain();
                glTranslatef((float)-x, (float)-y, (float)-z);
                glTranslatef((float)we.posX, (float)we.posY, (float)we.posZ);
                Hammer.proxy.setClientWorld(Hammer.getClientShadowWorld());
                try {
                    renderInfo.renderEntities(partialTicks);
                } finally {
                    Hammer.proxy.restoreClientWorld();
                }
                checkGLError("FZDS after render");
            } finally {
                glPopMatrix();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
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
