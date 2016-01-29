package factorization.fzds;

import factorization.aabbdebug.AabbDebugger;
import factorization.api.Coord;
import factorization.api.Quaternion;
import factorization.fzds.interfaces.DeltaCapability;
import factorization.fzds.interfaces.IFzdsShenanigans;
import factorization.shared.Core;
import factorization.util.NORELEASE;
import factorization.util.NumUtil;
import factorization.util.RenderUtil;
import factorization.util.SpaceUtil;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.chunk.ListedRenderChunk;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Iterator;

import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;


public class RenderDimensionSliceEntity extends Render<DimensionSliceEntity> implements IFzdsShenanigans {
    public static int update_frequency = 16;
    public static RenderDimensionSliceEntity instance;
    
    private static long megatickCount = 0;

    protected RenderDimensionSliceEntity(RenderManager renderManager) {
        super(renderManager);
        instance = this;
        Core.loadBus(this);
    }

    @Override
    protected ResourceLocation getEntityTexture(DimensionSliceEntity entity) { return null; }

    EntityLivingBase shadowEye = new EntityLivingBase(null) {
        @Override protected void entityInit() { }
        @Override public void readEntityFromNBT(NBTTagCompound var1) { }
        @Override public void writeEntityToNBT(NBTTagCompound var1) { }
        @Override public ItemStack getHeldItem() { return null; }
        @Override public ItemStack getEquipmentInSlot(int var1) { return null; }
        @Override public ItemStack getCurrentArmor(int slotIn) { return null; }
        @Override public void setCurrentItemOrArmor(int var1, ItemStack var2) { }
        @Override public ItemStack[] getInventory() { return null; }
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
        private RenderChunk renderers[] = null;
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
            
            int DSC = 16;
            xSizeChunk = (xSize + DSC)/16;
            ySizeChunk = (ySize + DSC)/16;
            zSizeChunk = (zSize + DSC)/16;
            
            if (xSizeChunk <= 0 || ySizeChunk <= 0 || zSizeChunk <= 0) throw new AssertionError();
            
            cubicChunkCount = xSizeChunk * ySizeChunk * zSizeChunk;

            renderers = new RenderChunk[cubicChunkCount];
            int i = 0;
            RenderGlobal rg = Minecraft.getMinecraft().renderGlobal;
            RenderUtil.checkGLError("FZDS before render");
            int DC = 16;
            for (int y = corner.y; y <= far.y; y += DC) {
                for (int x = corner.x; x <= far.x; x += DC) {
                    for (int z = corner.z; z <= far.z; z += DC) {
                        //We could allocate lists per WR instead?
                        //NORELEASE: w.loadedTileEntityList might be wrong? Might be inefficient?
                        //It creates a list... maybe we should use that instead?
                        NORELEASE.fixme("Pick the right one");
                        RenderChunk rc = new ListedRenderChunk(corner.w, rg, new BlockPos(x, y, z), i /* index; not actually used. */);
                        renderers[i] = rc;
                        chunkNeedsRedraw(rg, renderers[i]);
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
            final Entity player = Minecraft.getMinecraft().getRenderViewEntity();
            Vec3 eyepos = dse.real2shadow(SpaceUtil.fromEntPos(player));
            shadowEye.posX = eyepos.xCoord;
            shadowEye.posY = eyepos.yCoord;
            shadowEye.posZ = eyepos.zCoord;
            renderContainer.initialize(shadowEye.posX, shadowEye.posY, shadowEye.posZ);
        }

        void renderTerrain(int pass, double partial) {
            //GL11.glPushAttrib(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_LIGHTING_BIT);
            RenderHelper.disableStandardItemLighting();
            Minecraft mc = Minecraft.getMinecraft();
            //if (Minecraft.isAmbientOcclusionEnabled() && FzConfig.dimension_slice_allow_smooth) {
            //    GL11.glShadeModel(GL11.GL_SMOOTH);
            //}


            GlStateManager.disableAlpha();
            renderBlockLayer(EnumWorldBlockLayer.SOLID, 0, pass, shadowEye);
            GlStateManager.enableAlpha();
            renderBlockLayer(EnumWorldBlockLayer.CUTOUT_MIPPED, partial, pass, shadowEye);
            mc.getTextureManager().getTexture(TextureMap.locationBlocksTexture).setBlurMipmap(false, false);
            renderBlockLayer(EnumWorldBlockLayer.CUTOUT, partial, pass, shadowEye);
            mc.getTextureManager().getTexture(TextureMap.locationBlocksTexture).restoreLastBlurMipmap();
            //NORELEASE (alpha): Oh god, this is going to be a pain to get working properly...
            renderBlockLayer(EnumWorldBlockLayer.TRANSLUCENT, partial, pass, shadowEye);
            //Entities can do multi-pass rendering, right?

            //GL11.glPopAttrib();
        }

        ChunkRenderContainer renderContainer = new RenderList(); // NORELEASE: Check VBO enabled

        private void renderBlockLayer(EnumWorldBlockLayer drawMode, double partial, int pass, EntityLivingBase shadowEye) {
            // NORELEASE: See RenderGlobal.renderBlockLayer for info on transparency sorting
            for (int i = 0; i < renderers.length; i++) {
                RenderChunk rc = renderers[i];
                if (rc.getCompiledChunk().isLayerEmpty(drawMode)) continue;
                renderContainer.addRenderChunk(rc, drawMode);
            }
            // NORELEASE: Also needs DisplayList vs. VBO stuff? We should probably be replacing this class with render global!
            renderContainer.renderChunkLayer(drawMode);
        }

        void renderEntities(float partialTicks) {
            RenderHelper.enableStandardItemLighting();
            //Maybe we should use RenderGlobal.renderEntities ???
            double sx = TileEntityRendererDispatcher.staticPlayerX;
            double sy = TileEntityRendererDispatcher.staticPlayerY;
            double sz = TileEntityRendererDispatcher.staticPlayerZ;
            
            double px = TileEntityRendererDispatcher.instance.entityX;
            double py = TileEntityRendererDispatcher.instance.entityY;
            double pz = TileEntityRendererDispatcher.instance.entityZ;
            // The player's position converted to shadow coordinates; these fields used in renderTileEntity()
            TileEntityRendererDispatcher.instance.entityX = shadowEye.posX;
            TileEntityRendererDispatcher.instance.entityY = shadowEye.posY;
            TileEntityRendererDispatcher.instance.entityZ = shadowEye.posZ;

            Minecraft mc = Minecraft.getMinecraft();
            RenderManager renderManager = mc.getRenderManager();
            
            try {
                int xwidth = far.x - corner.x;
                int height = far.y - corner.y;
                int zwidth = far.z - corner.z;
                
                for (int cdx = 0; cdx < xwidth; cdx++) {
                    for (int cdz = 0; cdz < zwidth; cdz++) {
                        Chunk here = corner.w.getChunkFromBlockCoords(new BlockPos(corner.x + cdx*16, corner.y, corner.z + cdz*16));
                        Core.profileStart("entity");
                        ClassInheritanceMultiMap<Entity>[] entityLists = here.getEntityLists();
                        for (ClassInheritanceMultiMap<Entity> entityList : entityLists) {
                            for (Entity ent : entityList) {
                                if (ent.posY < corner.y - entity_buffer) {
                                    continue;
                                }
                                if (ent.posY > far.y + entity_buffer) {
                                    continue;
                                }
                                if (nest == 3 && ent instanceof DimensionSliceEntity) {
                                    continue;
                                }
                                //if ent is a proxying player, don't render it?
                                renderManager.renderEntitySimple(ent, partialTicks);
                            }
                        }
                        Core.profileEnd();
                        Core.profileStart("tesr");
                        for (TileEntity te : here.getTileEntityMap().values()) {
                            //I warned you about comods, bro! I told you, dawg! (Shouldn't actually be a problem if we're rendering properly)
                            
                            //Since we don't know the actual distance from the player to the TE, we need to cheat.
                            //(We *could* calculate it, I suppose... Or maybe just not render entities when the player's far away)
                            /*TileEntityRendererDispatcher.staticPlayerX = te.xCoord;
                            TileEntityRendererDispatcher.staticPlayerY = te.yCoord;
                            TileEntityRendererDispatcher.staticPlayerZ = te.zCoord;*/
                            
                            TileEntityRendererDispatcher.instance.renderTileEntity(te, partialTicks, 0 /* NORELEASE: Damage */);
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
                
                TileEntityRendererDispatcher.instance.entityX = px;
                TileEntityRendererDispatcher.instance.entityY = py;
                TileEntityRendererDispatcher.instance.entityZ = pz;
            }
        }
        
        void renderBreakingBlocks(EntityPlayer player, float partial) {
            HashMap<Integer, DestroyBlockProgress> damagedBlocks = HammerClientProxy.shadowRenderGlobal.damagedBlocks;
            if (damagedBlocks.isEmpty()) return;
            Coord a = dse.getCorner();
            Coord b = dse.getFarCorner();
            
            Tessellator tess = Tessellator.getInstance();
            startDamageDrawing(player, partial);
            Minecraft mc = Minecraft.getMinecraft();

            RenderGlobal realRg = HammerClientProxy.getRealRenderGlobal();

            TextureAtlasSprite[] crackIcons = mc.renderGlobal.destroyBlockIcons;
            BlockRendererDispatcher brd = mc.getBlockRendererDispatcher();
            
            for (Iterator<DestroyBlockProgress> iterator = damagedBlocks.values().iterator(); iterator.hasNext();) {
                DestroyBlockProgress damage = iterator.next();
                int damage_getPartialBlockX = damage.getPosition().getX();
                int damage_getPartialBlockY = damage.getPosition().getY();
                int damage_getPartialBlockZ = damage.getPosition().getZ();
                if (a.x <= damage_getPartialBlockX && damage_getPartialBlockX <= b.x
                        && a.y <= damage_getPartialBlockY && damage_getPartialBlockY <= b.y
                        && a.z <= damage_getPartialBlockZ && damage_getPartialBlockZ <= b.z) {
                    renderDamage(a.w, damage, crackIcons, brd);
                }
            }
            
            endDamageDrawing(tess);
        }
        
        void renderDamage(World world, DestroyBlockProgress damage, TextureAtlasSprite[] icons, BlockRendererDispatcher brd) {
            IBlockState bs = world.getBlockState(damage.getPosition());
            Block block = bs.getBlock();
            if (block.getMaterial() == Material.air) return;
            int dmg = damage.getPartialBlockDamage();
            brd.renderBlockDamage(bs, damage.getPosition(), icons[dmg], world);
        }
        
        void startDamageDrawing(EntityPlayer player, float partial) {
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            /*
             * Push all the things!
             * There's a bunch of crazy state stuff here; who knows if the commented stuff below that came with it actually restores the state?
             * In any case, it could still mess up the state in other places.
             */
            // NORELEASE: Vanilla seems to have less z-fighting than we do. How?
            
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
            Tessellator tessI = Tessellator.getInstance();
            WorldRenderer tess = tessI.getWorldRenderer();
            tess.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
            double dx = NumUtil.interp(player.prevPosX, player.posX, partial);
            double dy = NumUtil.interp(player.prevPosY, player.posY, partial);
            double dz = NumUtil.interp(player.prevPosZ, player.posZ, partial);
            tess.setTranslation(-dx, -dy, -dz);
        }
        
        void endDamageDrawing(Tessellator tess) {
            tess.draw();
            /*GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glPolygonOffset(0.0F, 0.0F);
            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glDepthMask(true);
            GL11.glDisable(GL11.GL_BLEND);*/ // See comment above re. these attributes
            GL11.glPopAttrib();
        }
    }
    
    static void markBlocksForUpdate(DimensionSliceEntity dse, int lx, int ly, int lz, int hx, int hy, int hz) {
        if (dse.renderInfo == null) {
            dse.renderInfo = instance.new DSRenderInfo(dse);
            return;
        }
        DSRenderInfo renderInfo = (DSRenderInfo) dse.renderInfo;
        renderInfo.anyRenderersDirty = true;
        RenderGlobal rg = Minecraft.getMinecraft().renderGlobal;
        for (int i = 0; i < renderInfo.renderers.length; i++) {
            RenderChunk wr = renderInfo.renderers[i];
            int wr_posX = wr.getPosition().getX();
            int wr_posY = wr.getPosition().getY();
            int wr_posZ = wr.getPosition().getZ();
            if (NumUtil.intersect(lx, hx, wr_posX, wr_posX + 16) &&
                    NumUtil.intersect(ly, hy, wr_posY, wr_posY + 16) &&
                    NumUtil.intersect(lz, hz, wr_posZ, wr_posZ + 16)) {
                chunkNeedsRedraw(rg, wr);
            }
        }
    }

    static void chunkNeedsRedraw(RenderGlobal rg, RenderChunk wr) {
        wr.setNeedsUpdate(true);
        //rg.renderDispatcher.updateChunkLater(wr);
        rg.renderDispatcher.updateChunkNow(wr);
        NORELEASE.fixme("This is bad. Multiple block updates could potentially cause thrashing.");
    }
    
    DSRenderInfo getRenderInfo(DimensionSliceEntity dse) {
        if (dse.renderInfo == null) {
            dse.renderInfo = new DSRenderInfo(dse);
        }
        return (DSRenderInfo) dse.renderInfo;
    }
    
    public static int nest = 0; //is 0 usually. Gets incremented right before we start actually rendering.
    @Override
    public void doRender(DimensionSliceEntity dse, double x, double y, double z, float yaw, float partialTicks) {
        //need to do: Don't render if we're far away! (This should maybe be done in some other function?)
        if (dse.isDead) {
            return;
        }
        DSRenderInfo renderInfo = getRenderInfo(dse);
        Minecraft mc = Minecraft.getMinecraft();
        if (nest == 0) {
            if (mc.getRenderManager().isDebugBoundingBox()) {
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
                if (nest == 1) {
                    renderInfo.updateRelativeEyePosition();
                }
                Core.profileStart("renderTerrain");
                renderInfo.renderTerrain(0, partialTicks);
                Core.profileEnd();
                RenderUtil.checkGLError("FZDS terrain display list render");
                GL11.glTranslated(dse.posX - x, dse.posY - y, dse.posZ - z);
                Coord c = dse.getCorner();
                GL11.glTranslated(-c.x, -c.y, -c.z);
                if (nest == 1) {
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
        }
    }
    
    public void tickStart() {
        megatickCount++;
        if (nest != 0) {
            nest = 0;
            Core.logSevere("FZDS render nesting depth was not 0");
        }
    }
}
