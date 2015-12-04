package factorization.fzds;

import static org.lwjgl.opengl.GL11.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import factorization.util.NumUtil;
import factorization.util.RenderUtil;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.WorldEvent;

import org.lwjgl.opengl.GL11;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;
import factorization.aabbdebug.AabbDebugger;
import factorization.api.Coord;
import factorization.api.Quaternion;
import factorization.common.FzConfig;
import factorization.fzds.interfaces.DeltaCapability;
import factorization.fzds.interfaces.IFzdsShenanigans;
import factorization.shared.Core;


public class RenderDimensionSliceEntity extends Render implements IFzdsShenanigans {
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

    Vec3 shadowEyeVec = new Vec3(0, 0, 0);
    EntityLivingBase shadowEye = new EntityLivingBase(null) {
        @Override protected void entityInit() { }
        @Override public void readEntityFromNBT(NBTTagCompound var1) { }
        @Override public void writeEntityToNBT(NBTTagCompound var1) { }
        @Override public ItemStack getHeldItem() { return null; }
        @Override public ItemStack getEquipmentInSlot(int var1) { return null; }
        @Override public void setCurrentItemOrArmor(int var1, ItemStack var2) { }
        @Override public ItemStack[] getLastActiveItems() { return null; }
        @Override public void setHealth(float par1) { }
    };
    
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
        
        RenderBlocks rb = new RenderBlocks(DeltaChunk.getClientShadowWorld());
        
        public DSRenderInfo(DimensionSliceEntity dse) {
            this.dse = dse;
            this.corner = dse.getCorner();
            this.far = dse.getFarCorner();
            
            xSize = (far.x - corner.x);
            ySize = (far.y - corner.y);
            zSize = (far.z - corner.z);
            
            int DSC = 16;
            xSizeChunk = (xSize + DSC)/16;
            ySizeChunk = (ySize + DSC)/16;
            zSizeChunk = (zSize + DSC)/16;
            
            if (xSizeChunk <= 0 || ySizeChunk <= 0 || zSizeChunk <= 0) throw new AssertionError();
            
            cubicChunkCount = xSizeChunk * ySizeChunk * zSizeChunk;
            
            renderers = new WorldRenderer[cubicChunkCount];
            int i = 0;
            RenderUtil.checkGLError("FZDS before render");
            int DC = 16;
            for (int y = corner.y; y <= far.y; y += DC) {
                for (int x = corner.x; x <= far.x; x += DC) {
                    for (int z = corner.z; z <= far.z; z += DC) {
                        //We could allocate lists per WR instead?
                        //NORELEASE: w.loadedTileEntityList might be wrong? Might be inefficient?
                        //It creates a list... maybe we should use that instead?
                        renderers[i] = new WorldRenderer(corner.w, corner.w.loadedTileEntityList, x, y, z, getRenderList() + i*wr_display_list_size);
                        renderers[i].posXClip = x - corner.x;
                        renderers[i].posYClip = y - corner.y;
                        renderers[i].posZClip = z - corner.z;
                        renderers[i].markDirty();
                        RenderUtil.checkGLError("FZDS WorldRenderer init");
                        i++;
                    }
                }
            }
            if (i != cubicChunkCount) throw new AssertionError();
        }
        
        int last_update_index = 0;
        int render_skips = 0;
        
        void updateRelativeEyePosition() {
            final Entity player = Minecraft.getMinecraft().renderViewEntity;
            shadowEyeVec.xCoord = player.posX;
            shadowEyeVec.yCoord = player.posY;
            shadowEyeVec.zCoord = player.posZ;
            Vec3 eyepos = dse.real2shadow(shadowEyeVec);
            shadowEye.posX = eyepos.xCoord;
            shadowEye.posY = eyepos.yCoord;
            shadowEye.posZ = eyepos.zCoord;
        }
        
        void update() {
            if (far.getChunk().isEmpty()) {
                return;
            }
            if (!anyRenderersDirty) {
                last_update_index = 0;
                return;
            } // NORELEASE: Can we queue the renderchunks up to something in 1.8?
            boolean start_from_begining = last_update_index == 0;
            Core.profileStart("updateFzdsTerrain");
            RenderUtil.checkGLError("FZDS before WorldRender update");
            final int update_limit = 20; //NORELEASE?
            int updates = 0;
            while (last_update_index < renderers.length) {
                WorldRenderer wr = renderers[last_update_index++];
                if (wr.needsUpdate) {
                    if (updates == 0) {
                        updateRelativeEyePosition();
                    }
                    wr.updateRenderer(shadowEye);
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
            GL11.glPushAttrib(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_LIGHTING_BIT);
            RenderHelper.disableStandardItemLighting();
            Minecraft mc = Minecraft.getMinecraft();
            if (Minecraft.isAmbientOcclusionEnabled() && FzConfig.dimension_slice_allow_smooth) {
                GL11.glShadeModel(GL11.GL_SMOOTH);
            }
            for (int pass = 0; pass < 2; pass++) {
                if (pass == 1) {
                    //setup transparency
                    //NORELEASE: Oh god, this is going to be a pain to get working properly...
                    // Can we just cheat? No transparency?
                    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                    GL11.glEnable(GL11.GL_BLEND);
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
            
            double px = TileEntityRendererDispatcher.instance.field_147560_j;
            double py = TileEntityRendererDispatcher.instance.field_147561_k;
            double pz = TileEntityRendererDispatcher.instance.field_147558_l;
            // The player's position converted to shadow coordinates; these fields used in renderTileEntity()
            TileEntityRendererDispatcher.instance.field_147560_j = shadowEye.posX;
            TileEntityRendererDispatcher.instance.field_147561_k = shadowEye.posY;
            TileEntityRendererDispatcher.instance.field_147558_l = shadowEye.posZ;
            
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
                            /*TileEntityRendererDispatcher.staticPlayerX = te.xCoord;
                            TileEntityRendererDispatcher.staticPlayerY = te.yCoord;
                            TileEntityRendererDispatcher.staticPlayerZ = te.zCoord;*/
                            
                            TileEntityRendererDispatcher.instance.renderTileEntity(te, partialTicks);
                            // NORELEASE (probably): cull if outside camera!
                            // NORELEASE: That's the wrong list? It's every TE, not the TESR'd TEs.
                        }
                        Core.profileEnd();
                    }
                }
            } finally {
                TileEntityRendererDispatcher.staticPlayerX = sx;
                TileEntityRendererDispatcher.staticPlayerY = sy;
                TileEntityRendererDispatcher.staticPlayerZ = sz;
                
                TileEntityRendererDispatcher.instance.field_147560_j = px;
                TileEntityRendererDispatcher.instance.field_147561_k = py;
                TileEntityRendererDispatcher.instance.field_147558_l = pz;
            }
        }
        
        void renderBreakingBlocks(EntityPlayer player, float partial) {
            HashMap<Integer, DestroyBlockProgress> damagedBlocks = HammerClientProxy.shadowRenderGlobal.damagedBlocks;
            if (damagedBlocks.isEmpty()) return;
            Coord a = dse.getCorner();
            Coord b = dse.getFarCorner();
            
            Tessellator tess = Tessellator.instance;
            startDamageDrawing(tess, player, partial);
            Minecraft mc = Minecraft.getMinecraft();

            RenderGlobal realRg = HammerClientProxy.getRealRenderGlobal();
            
            for (Iterator<DestroyBlockProgress> iterator = damagedBlocks.values().iterator(); iterator.hasNext();) {
                DestroyBlockProgress damage = iterator.next();
                if (a.x <= damage.getPartialBlockX() && damage.getPartialBlockX() <= b.x
                        && a.y <= damage.getPartialBlockY() && damage.getPartialBlockY() <= b.y
                        && a.z <= damage.getPartialBlockZ() && damage.getPartialBlockZ() <= b.z) {
                    renderDamage(a.w, damage, realRg.destroyBlockIcons);
                }
            }
            
            endDamageDrawing(tess);
        }
        
        void renderDamage(World world, DestroyBlockProgress damage, IIcon destructionIcons[]) {
            Block block = world.getBlock(damage.getPartialBlockX(), damage.getPartialBlockY(), damage.getPartialBlockZ());

            if (block.getMaterial() != Material.air) {
                rb.renderBlockUsingTexture(block, damage.getPartialBlockX(), damage.getPartialBlockY(), damage.getPartialBlockZ(), destructionIcons[damage.getPartialBlockDamage()]);
            }
        }
        
        void startDamageDrawing(Tessellator tess, EntityPlayer player, float partial) {
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            /*
             * Push all the things!
             * There's a bunch of crazy state stuff here; who knows if the commented stuff below that came with it actually restores the state?
             * In any case, it could still mess up the state in other places.
             */
            
            GL11.glShadeModel(GL11.GL_FLAT);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
            GL11.glEnable(GL11.GL_BLEND);
            OpenGlHelper.glBlendFunc(770 /* GL_SRC_ALPHA */, 1 /* GL_ONE */, 1 /* GL_ONE */, 0 /* GL_ZERO */);
            OpenGlHelper.glBlendFunc(774 /* GL_DST_COLOR */, 768 /* GL_SRC_COLOR */, 1 /* GL_ONE */, 0 /* GL_ZERO */);
            bindTexture(TextureMap.locationBlocksTexture);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.5F);
            GL11.glPolygonOffset(-3.0F, -3.0F);
            GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            tess.startDrawingQuads();
            double dx = NumUtil.interp(player.prevPosX, player.posX, partial);
            double dy = NumUtil.interp(player.prevPosY, player.posY, partial);
            double dz = NumUtil.interp(player.prevPosZ, player.posZ, partial);
            tess.setTranslation(-dx, -dy, -dz);
            tess.disableColor();
        }
        
        void endDamageDrawing(Tessellator tess) {
            tess.draw();
            tess.setTranslation(0.0D, 0.0D, 0.0D);
            /*GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glPolygonOffset(0.0F, 0.0F);
            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glDepthMask(true);
            GL11.glDisable(GL11.GL_BLEND);*/ // See comment above re. these attributes
            GL11.glPopAttrib();
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
            if (NumUtil.intersect(lx, hx, wr.posX, wr.posX + 16) &&
                    NumUtil.intersect(ly, hy, wr.posY, wr.posY + 16) &&
                    NumUtil.intersect(lz, hz, wr.posZ, wr.posZ + 16)) {
                wr.markDirty();
            }
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
        DimensionSliceEntity dse = (DimensionSliceEntity) ent;
        DSRenderInfo renderInfo = getRenderInfo(dse);
        if (nest == 0) {
            if (RenderManager.debugBoundingBox) {
                AabbDebugger.addBox(dse.realArea);
            }
            Core.profileStart("fzds");
            RenderUtil.checkGLError("FZDS before render -- somebody left us a mess!");
            renderInfo.lastRenderInMegaticks = megatickCount;
        } else if (nest == 1) {
            Core.profileStart("recursion");
        } else if (nest > 3) {
            return; //This will never happen, except with outside help.
        }
        EntityPlayer real_player = Minecraft.getMinecraft().thePlayer;
        
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
                GL11.glTranslated(x, y, z);
                Quaternion rotation = dse.getRotation();
                if (!rotation.isZero() || !dse.prevTickRotation.isZero()) {
                    Quaternion rot = dse.prevTickRotation.slerp(rotation, partialTicks);
                    rot.incrNormalize();
                    rot.glRotate();
                }
                Vec3 centerOffset = dse.getRotationalCenterOffset();
                GL11.glTranslated(
                        -centerOffset.xCoord,
                        -centerOffset.yCoord,
                        -centerOffset.zCoord);
                if (dse.scale != 1) {
                    GL11.glScalef(dse.scale, dse.scale, dse.scale);
                }
                if (dse.opacity != 1) {
                    GL11.glColor4f(1, 1, 1, dse.opacity);
                }
                Core.profileStart("renderTerrain");
                renderInfo.renderTerrain();
                Core.profileEnd();
                RenderUtil.checkGLError("FZDS terrain display list render");
                GL11.glTranslated(dse.posX - x, dse.posY - y, dse.posZ - z);
                Coord c = dse.getCorner();
                GL11.glTranslated(-c.x, -c.y, -c.z);
                if (nest == 1) {
                    renderInfo.updateRelativeEyePosition();
                    // renderBreakingBlocks needs to happen before renderEntities due to gl state nonsense.
                    if (oracle) {
                        renderInfo.renderBreakingBlocks(real_player, partialTicks);
                        renderInfo.renderEntities(partialTicks);
                    } else {
                        Hammer.proxy.setShadowWorld();
                        try {
                            renderInfo.renderBreakingBlocks(real_player, partialTicks);
                            renderInfo.renderEntities(partialTicks);
                        } finally {
                            Hammer.proxy.restoreRealWorld();
                        }
                    }
                } else {
                    renderInfo.renderEntities(partialTicks);
                }
                RenderUtil.checkGLError("FZDS entity render");
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
                RenderUtil.checkGLError("FZDS after render");
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
