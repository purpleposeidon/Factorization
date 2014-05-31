package factorization.fzds;

import static org.lwjgl.opengl.GL11.glCallList;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;
import static org.lwjgl.opengl.GL11.glTranslatef;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.WorldEvent;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.RenderTickEvent;
import factorization.api.Coord;
import factorization.api.Quaternion;
import factorization.common.FzConfig;
import factorization.fzds.api.DeltaCapability;
import factorization.shared.Core;
import factorization.shared.FzUtil;


public class RenderDimensionSliceEntity extends Render {
    public static int update_frequency = 16;
    public static RenderDimensionSliceEntity instance;
    
    private Set<DSRenderInfo> renderInfoTracker = new HashSet<DSRenderInfo>();
    private static long megatickCount = 0;
    
    public RenderDimensionSliceEntity() {
        instance = this;
        Core.loadBus(this);
    }
    
    @Override
    protected ResourceLocation getEntityTexture(Entity entity) { return null; }

    class DSRenderInfo {
        //final int width = Hammer.cellWidth;
        //final int height = 4;
        //final int cubicChunkCount = width*width*height;
        private final int wr_display_list_size = 3; //how many display lists a WorldRenderer uses
        final int entity_buffer = 8;
        
        int renderCounts = 0;
        long lastRenderInMegaticks = megatickCount;
        boolean anyRenderersDirty = true;
        private int renderList = -1;
        private WorldRenderer renderers[] = null;
        Coord corner, far;
        DimensionSliceEntity dse;
        
        int xSize, ySize, zSize;
        int xSizeChunk, ySizeChunk, zSizeChunk;
        int cubicChunkCount;
        
        public DSRenderInfo(DimensionSliceEntity dse) {
            this.dse = dse;
            this.corner = dse.getCorner();
            this.far = dse.getFarCorner();
            
            xSize = (far.x - corner.x);
            ySize = (far.y - corner.y);
            zSize = (far.z - corner.z);
            
            xSizeChunk = xSize/16;
            ySizeChunk = ySize/16;
            zSizeChunk = zSize/16;
            
            
            int xzSizeChunk = xSizeChunk*zSizeChunk;
            cubicChunkCount = (1 + xSizeChunk)*(1 + ySizeChunk)*(1 + zSizeChunk);
            
            renderers = new WorldRenderer[cubicChunkCount];
            int i = 0;
            FzUtil.checkGLError("FZDS before render");
            for (int y = 0; y <= ySizeChunk; y++) {
                for (int x = 0; x <= xSizeChunk; x++) {
                    for (int z = 0; z <= zSizeChunk; z++) {
                        //We could allocate lists per WR instead?
                        //NORELEASE: w.loadedTileEntityList might be wrong? Might be inefficient?
                        //It creates a list... maybe we should use that instead?
                        renderers[i] = new WorldRenderer(corner.w, corner.w.loadedTileEntityList, corner.x + x*16, corner.y + y*16, corner.z + z*16, getRenderList() + i*wr_display_list_size);
                        renderers[i].posXClip = x*16;
                        renderers[i].posYClip = y*16;
                        renderers[i].posZClip = z*16;
                        //renderers[i].markDirty();
                        FzUtil.checkGLError("FZDS WorldRenderer init");
                        i++;
                    }
                }
            }
            assert i == cubicChunkCount;
        }
        
        int last_update_index = 0;
        int render_skips = 0;
        
        void update() {
            if (!anyRenderersDirty) {
                last_update_index = 0;
                return;
            }
            boolean start_from_begining = last_update_index == 0;
            Core.profileStart("updateFzdsTerrain");
            FzUtil.checkGLError("FZDS before WorldRender update");
            final int update_limit = 20; //NORELEASE?
            int updates = 0;
            while (last_update_index < renderers.length) {
                WorldRenderer wr = renderers[last_update_index++];
                if (wr.needsUpdate) {
                    wr.updateRenderer(Minecraft.getMinecraft().thePlayer /* NORELEASE: Need an entity located at the players position in shadowspace? */);
                    if (++updates == update_limit) {
                        break;
                    }
                }
            }
            if (last_update_index == renderers.length) {
                last_update_index = 0;
            }
            if (start_from_begining) {
                anyRenderersDirty = false;
            }
            Core.profileEnd();
        }
        
        void renderTerrain() {
            RenderHelper.disableStandardItemLighting();
            Minecraft mc = Minecraft.getMinecraft();
            if (Minecraft.isAmbientOcclusionEnabled() && FzConfig.dimension_slice_allow_smooth) {
                GL11.glShadeModel(GL11.GL_SMOOTH);
            }
            GL11.glPushAttrib(GL11.GL_COLOR_BUFFER_BIT);
            for (int pass = 0; pass < 2; pass++) {
                if (pass == 1) {
                    //setup transparency
                    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                    GL11.glEnable(GL11.GL_BLEND);
                    //GL11.glDisable(GL11.GL_CULL_FACE);
                }
                for (int i = 0; i < renderers.length; i++) {
                    WorldRenderer wr = renderers[i];
                    wr.isInFrustum = true; //XXX might not be necessary
                    int displayList = wr.getGLCallListForPass(pass);
                    if (displayList >= 0) {
                        bindTexture(Core.blockAtlas);
                        glCallList(displayList);
                    }
                }
            }
            GL11.glPopAttrib();
        }
        
        void renderEntities(float partialTicks) {
            RenderHelper.enableStandardItemLighting();
            //Maybe we should use RenderGlobal.renderEntities ???
            double sx = TileEntityRendererDispatcher.staticPlayerX;
            double sy = TileEntityRendererDispatcher.staticPlayerY;
            double sz = TileEntityRendererDispatcher.staticPlayerZ;
            try {
                int xwidth = far.x - corner.x;
                int height = far.y - corner.y;
                int zwidth = far.z - corner.z;
                
                for (int cdx = 0; cdx < xwidth; cdx++) {
                    for (int cdz = 0; cdz < zwidth; cdz++) {
                        Chunk here = corner.w.getChunkFromBlockCoords(corner.x + cdx*16, corner.z + cdz*16);
                        Core.profileStart("entity");
                        for (int i1 = 0; i1 < here.entityLists.length; i1++) {
                            List<Entity> ents = (List<Entity>)here.entityLists[i1];
                            for (int i2 = 0; i2 < ents.size(); i2++) {
                                Entity e = ents.get(i2);
                                if (e.posY < corner.y - entity_buffer) {
                                    continue;
                                }
                                if (e.posY > far.y + entity_buffer) {
                                    continue;
                                }
                                if (nest == 3 && e instanceof DimensionSliceEntity) {
                                    continue;
                                }
                                //if e is a proxying player, don't render it?
                                RenderManager.instance.renderEntitySimple(e, partialTicks);
                            }
                        }
                        Core.profileEnd();
                        Core.profileStart("tesr");
                        for (TileEntity te : ((Map<ChunkPosition, TileEntity>)here.chunkTileEntityMap).values()) {
                            //I warned you about comods, bro! I told you, dawg! (Shouldn't actually be a problem if we're rendering properly)
                            
                            //Since we don't know the actual distance from the player to the TE, we need to cheat.
                            //(We *could* calculate it, I suppose... Or maybe just not render entities when the player's far away)
                            TileEntityRendererDispatcher.staticPlayerX = te.xCoord;
                            TileEntityRendererDispatcher.staticPlayerY = te.yCoord;
                            TileEntityRendererDispatcher.staticPlayerZ = te.zCoord;
                            TileEntityRendererDispatcher.instance.renderTileEntity(te, partialTicks);
                        }
                        Core.profileEnd();
                    }
                }
            } finally {
                TileEntityRendererDispatcher.staticPlayerX = sx;
                TileEntityRendererDispatcher.staticPlayerY = sy;
                TileEntityRendererDispatcher.staticPlayerZ = sz;
            }
        }
        
        int getRenderList() {
            if (renderList == -1) {
                renderList = GLAllocation.generateDisplayLists(wr_display_list_size*cubicChunkCount);
                renderInfoTracker.add(this);
                if (renderList == -1) {
                    Core.logWarning("GL display list allocation failed!");
                }
            }
            return renderList;
        }
        
        void discardRenderList() {
            if (renderList != -1) {
                GLAllocation.deleteDisplayLists(renderList);
                renderList = -1;
            }
            dse.renderInfo = null;
        }
    }
    
    static void markBlocksForUpdate(DimensionSliceEntity dse, int lx, int ly, int lz, int hx, int hy, int hz) {
        if (dse.renderInfo == null) {
            dse.renderInfo = instance.new DSRenderInfo(dse);
        }
        DSRenderInfo renderInfo = (DSRenderInfo) dse.renderInfo;
        renderInfo.anyRenderersDirty = true;
        for (int i = 0; i < renderInfo.renderers.length; i++) {
            WorldRenderer wr = renderInfo.renderers[i];
            wr.markDirty();
            //TODO NORELEASE IMPORTANT GAH
            /*if (FactorizationUtil.intersect(lx, hx, wr.posX, wr.posX + 16) &&
                    FactorizationUtil.intersect(ly, hy, wr.posY, wr.posY + 16) && 
                    FactorizationUtil.intersect(lz, hz, wr.posZ, wr.posZ + 16)) {
                wr.markDirty();
            }*/
        }
    }
    
    DSRenderInfo getRenderInfo(DimensionSliceEntity dse) {
        if (dse.renderInfo == null) {
            dse.renderInfo = new DSRenderInfo(dse);
        }
        return (DSRenderInfo) dse.renderInfo;
    }
    
    public static int nest = 0; //is 0 usually. Gets incremented right before we start actually rendering.
    @Override
    public void doRender(Entity ent, double x, double y, double z, float yaw, float partialTicks) {
        //need to do: Don't render if we're far away! (This should maybe be done in some other function?)
        if (ent.isDead) {
            return;
        }
        if (ent.ticksExisted < 5) {
            //TODO: Sometimes it fails to draw (Probably because the chunk data isn't loaded as it draws, it draws, does not dirty properly)
            return;
        }
        if (nest > 3) {
            return; //This will never happen, except with outside help.
        }
        DimensionSliceEntity dse = (DimensionSliceEntity) ent;
        DSRenderInfo renderInfo = getRenderInfo(dse);
        if (nest == 0) {
            Core.profileStart("fzds");
            FzUtil.checkGLError("FZDS before render -- somebody left us a mess!");
            renderInfo.lastRenderInMegaticks = megatickCount;
        } else if (nest == 1) {
            Core.profileStart("recursion");
        }
        
        nest++;
        try {
            final boolean oracle = dse.can(DeltaCapability.ORACLE);
            if (nest == 1) {
                Core.profileStart("update");
                try {
                    if (oracle) {
                        renderInfo.update();
                    } else {
                        Hammer.proxy.setShadowWorld();
                        try {
                            renderInfo.update();
                        } finally {
                            Hammer.proxy.restoreRealWorld();
                        }
                    }
                } finally {
                    Core.profileEnd();
                }
            }
            glPushMatrix();
            try {
                float pdx = (float) ((dse.posX - dse.lastTickPosX)*partialTicks);
                float pdy = (float) ((dse.posY - dse.lastTickPosY)*partialTicks); //err, not used? XXX
                float pdz = (float) ((dse.posZ - dse.lastTickPosZ)*partialTicks);
                glTranslatef((float)(x), (float)(y), (float)(z));
                Quaternion rotation = dse.getRotation();
                if (!rotation.isZero()) {
                    Quaternion quat = rotation.add(dse.prevTickRotation);
                    quat.incrScale(0.5);
                    Vec3 vec = Vec3.createVectorHelper(0, 0, 0);
                    quat.glRotate();
                }
                glTranslatef((float)(-dse.centerOffset.xCoord),
                        (float)(-dse.centerOffset.yCoord),
                        (float)(-dse.centerOffset.zCoord)
                        );
                if (dse.scale != 1) {
                    GL11.glScalef(dse.scale, dse.scale, dse.scale);
                }
                if (dse.opacity != 1) {
                    GL11.glColor4f(1, 1, 1, dse.opacity);
                }
                Core.profileStart("renderTerrain");
                renderInfo.renderTerrain();
                Core.profileEnd();
                FzUtil.checkGLError("FZDS terrain display list render");
                glTranslatef((float)(dse.posX - x), (float)(dse.posY - y), (float)(dse.posZ - z));
                Coord c = dse.getCorner();
                glTranslatef(-c.x, -c.y, -c.z);
                if (nest == 1) {
                    if (oracle) {
                        renderInfo.renderEntities(partialTicks);
                    } else {
                        Hammer.proxy.setShadowWorld();
                        try {
                            renderInfo.renderEntities(partialTicks);
                        } finally {
                            Hammer.proxy.restoreRealWorld();
                        }
                    }
                } else {
                    renderInfo.renderEntities(partialTicks);
                }
                FzUtil.checkGLError("FZDS entity render");
            } finally {
                if (dse.opacity != 1) {
                    GL11.glColor4f(1, 1, 1, 1);
                }
                glPopMatrix();
            }
        } catch (Exception e) {
            Core.logSevere("FZDS failed to render");
            e.printStackTrace(System.err);
        }
        finally {
            nest--;
            if (nest == 0) {
                FzUtil.checkGLError("FZDS after render");
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
    
    @SubscribeEvent
    public void worldChanged(WorldEvent.Unload unloadEvent) {
        //This only happens when a local server is unloaded.
        //This probably happens on a different thread, so let the usual tick handler clean it up.
        megatickCount += 100;
    }

    private int tickDelay = 0;
    
    @SubscribeEvent
    public void tick(RenderTickEvent event) {
        if (tickDelay++ <= 20) return;
        // This is FPS-based, not worldtick based. It just needs to happen occasionally
        tickDelay = 0;
        if (event.phase == Phase.START) {
            tickStart();
        } else if (event.phase == Phase.END) {
            tickEnd();
        }
    }
    
    public void tickStart() {
        megatickCount++;
        if (nest != 0) {
            nest = 0;
            Core.logSevere("FZDS render nesting depth was not 0");
        }
    }

    public void tickEnd() {
        discardOldRenderLists();
    }
}
