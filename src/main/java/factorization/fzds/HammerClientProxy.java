package factorization.fzds;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import cpw.mods.fml.relauncher.Side;
import factorization.api.Coord;
import factorization.api.Quaternion;
import factorization.fzds.api.IDeltaChunk;
import factorization.shared.Core;
import factorization.shared.FzUtil;

public class HammerClientProxy extends HammerProxy {
    public HammerClientProxy() {
        RenderDimensionSliceEntity rwe = new RenderDimensionSliceEntity();
        RenderingRegistry.registerEntityRenderingHandler(DimensionSliceEntity.class, rwe);
        Core.loadBus(rwe);
    }
    
    //These two classes below make it easy to see in a debugger.
    public static class HammerChunkProviderClient extends ChunkProviderClient {
        public HammerChunkProviderClient(World par1World) {
            super(par1World);
        }
    }
    
    public static class HammerWorldClient extends WorldClient {
        public HammerWorldClient(NetHandlerPlayClient par1NetClientHandler, WorldSettings par2WorldSettings, int par3, EnumDifficulty par4, Profiler par5Profiler) {
            super(par1NetClientHandler, par2WorldSettings, par3, par4, par5Profiler);
        }
        
        @Override
        public void playSoundAtEntity(Entity par1Entity, String par2Str, float par3, float par4) {
            super.playSoundAtEntity(par1Entity, par2Str, par3, par4);
        }
        
        public void clearAccesses() {
            worldAccesses.clear();
        }
        
        public void shadowTick() {
            clientChunkProvider.unloadQueuedChunks();
        }
    }
    
    @Override
    public World getClientRealWorld() {
        if (real_world == null) {
            return Minecraft.getMinecraft().theWorld;
        }
        return real_world;
    }
    
    private static World lastWorld = null;
    static ShadowRenderGlobal shadowRenderGlobal = null;
    void checkForWorldChange() {
        WorldClient currentWorld = Minecraft.getMinecraft().theWorld;
        if (currentWorld != lastWorld) {
            lastWorld = currentWorld;
            if (lastWorld == null) {
                return;
            }
            if (Hammer.worldClient != null) {
                ((HammerWorldClient)Hammer.worldClient).clearAccesses();
                Hammer.worldClient.addWorldAccess(shadowRenderGlobal = new ShadowRenderGlobal(currentWorld));
            }
        }
    }
    
    @SubscribeEvent
    public void tick(ClientTickEvent event) {
        if (event.phase != Phase.START) return;
        checkForWorldChange(); // Is there an event for this?
        runShadowTick();
        if (shadowRenderGlobal != null) {
            shadowRenderGlobal.removeStaleDamage();
        }
    }
    
    @Override
    public void createClientShadowWorld() {
        final Minecraft mc = Minecraft.getMinecraft();
        World world = mc.theWorld;
        if (world == null || mc.thePlayer == null) {
            Hammer.worldClient = null;
            send_queue = null;
            fake_player = null;
            return;
        }
        send_queue = mc.thePlayer.sendQueue;
        WorldInfo wi = world.getWorldInfo();
        Hammer.worldClient = new HammerWorldClient(send_queue,
                new WorldSettings(wi),
                Hammer.dimensionID,
                world.difficultySetting,
                Core.proxy.getProfiler());
        Hammer.worldClient.addWorldAccess(shadowRenderGlobal = new ShadowRenderGlobal(mc.theWorld));
    }
    
    
    @SubscribeEvent
    public void onClientLogout(ClientDisconnectionFromServerEvent event) {
        //TODO: what else we can do here to cleanup?
        if (FMLCommonHandler.instance().getEffectiveSide() != Side.CLIENT) {
            return;
        }
        if (Hammer.worldClient != null) {
            ((HammerWorldClient)Hammer.worldClient).clearAccesses();
        }
        Hammer.worldClient = null;
        send_queue = null;
        fake_player = null;
    }
    
    private static NetHandlerPlayClient send_queue;
    private boolean send_queue_spam = false;
    private void setSendQueueWorld(WorldClient wc) {
        if (send_queue == null) {
            if (!send_queue_spam) {
                Core.logSevere("send_queue is null!?");
                send_queue_spam = true;
            }
            return;
        }
        send_queue.clientWorldController = wc;
    }
    
    private void setWorldAndPlayer(WorldClient wc, EntityClientPlayerMP player) {
        Minecraft mc = Minecraft.getMinecraft();
        if (wc == null) {
            Core.logSevere("Setting client world to null. PREPARE FOR IMPACT.");
        }
        //For logic
        mc.theWorld = wc;
        mc.thePlayer = player;
        mc.thePlayer.worldObj = wc;
        setSendQueueWorld(wc);
        
        //For rendering
        mc.renderViewEntity = player; //TODO NOTE: This may mess up in third person!
        if (TileEntityRendererDispatcher.instance.field_147550_f != null) {
            TileEntityRendererDispatcher.instance.field_147550_f = wc;
        }
        if (RenderManager.instance.worldObj != null) {
            RenderManager.instance.worldObj = wc;
        }
        mc.renderGlobal.theWorld = wc;
    }
    
    private EntityClientPlayerMP real_player = null;
    private WorldClient real_world = null;
    private EntityClientPlayerMP fake_player = null;
    
    @Override
    public void setShadowWorld() {
        //System.out.println("Setting world");
        Minecraft mc = Minecraft.getMinecraft();
        WorldClient w = (WorldClient) DeltaChunk.getClientShadowWorld();
        assert w != null;
        if (real_player != null || real_world != null) {
            Core.logSevere("Tried to switch to Shadow world, but we're already in the shadow world");
            return;
        }
        if (real_player == null) {
            real_player = mc.thePlayer;
            if (real_player == null) {
                Core.logSevere("Swapping out to hammer world, but thePlayer is null");
            }
        }
        if (real_world == null) {
            real_world = mc.theWorld;
            if (real_world == null) {
                Core.logSevere("Swapping out to hammer world, but theWorld is null");
            }
        }
        real_player.worldObj = w;
        if (fake_player == null || w != fake_player.worldObj) {
            fake_player = new EntityClientPlayerMP(
                    mc,
                    mc.theWorld /* why is this real world? */,
                    mc.getSession(), real_player.sendQueue /* not sure about this one. */,
                    real_player.getStatFileWriter());
        }
        setWorldAndPlayer((WorldClient) w, fake_player);
    }
    
    @Override
    public void restoreRealWorld() {
        //System.out.println("Restoring world");
        setWorldAndPlayer(real_world, real_player);
        real_world = null;
        real_player = null;
    }
    
    @Override
    public boolean isInShadowWorld() {
        return real_world != null;
    }
    
    void runShadowTick() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.isGamePaused()) {
            return;
        }
        EntityPlayer player = mc.thePlayer;
        if (player == null) {
            return;
        }
        HammerWorldClient w = (HammerWorldClient) DeltaChunk.getClientShadowWorld();
        if (w == null) {
            return;
        }
        int range = 10;
        AxisAlignedBB nearby = player.boundingBox.expand(range, range, range);
        Iterable<IDeltaChunk> nearbyChunks = mc.theWorld.getEntitiesWithinAABB(IDeltaChunk.class, nearby);
        setShadowWorld();
        Core.profileStart("FZDStick");
        try {
            //Inspired by Minecraft.runTick()
            w.updateEntities();
            Vec3 playerPos = Vec3.createVectorHelper(player.posX, player.posY, player.posZ);
            for (IDeltaChunk idc : nearbyChunks) {
                Vec3 center = idc.real2shadow(playerPos);
                w.doVoidFogParticles((int) center.xCoord, (int) center.yCoord, (int) center.zCoord);
            }
            w.shadowTick();
        } finally {
            Core.profileEnd();
            restoreRealWorld();
        }
    }
    
    @Override
    public void clientInit() {
        
    }
    
    final Minecraft mc = Minecraft.getMinecraft();
    
    @SubscribeEvent
    public void resetTracing(ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        distance = Double.POSITIVE_INFINITY;
        // _shadowSelected = null;
        //_rayTarget = null;
        //_selectionBlockBounds = null;
        //_hitSlice = null;
    }
    
    public void offerHit(MovingObjectPosition mop, DseRayTarget rayTarget, AxisAlignedBB moppedBounds) {
        double d = rayTarget.getDistanceSqToEntity(mc.thePlayer);
        if (d > distance) return;
        _shadowSelected = mop;
        _rayTarget = rayTarget;
        _selectionBlockBounds = moppedBounds;
        _hitSlice = rayTarget.parent;
    }
    
    double distance;
    MovingObjectPosition _shadowSelected;
    DseRayTarget _rayTarget;
    AxisAlignedBB _selectionBlockBounds;
    DimensionSliceEntity _hitSlice;
    
    @SubscribeEvent
    public void renderSelection(DrawBlockHighlightEvent event) {
        if (!(event.target.entityHit instanceof DseRayTarget)) {
            return;
        }
        AxisAlignedBB box = _selectionBlockBounds;
        MovingObjectPosition shadowSelected = _shadowSelected;
        if (box == null || shadowSelected.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            return;
        }
        if (event.isCanceled()) {
            return;
        }
        Coord here = new Coord(DeltaChunk.getClientShadowWorld(), shadowSelected.blockX, shadowSelected.blockY, shadowSelected.blockZ);
        here.getBlock().setBlockBounds(
                (float)(box.minX - here.x), (float)(box.minY - here.y), (float)(box.minZ - here.z),
                (float)(box.maxX - here.x), (float)(box.maxY - here.y), (float)(box.maxZ - here.z)
            );
        EntityPlayer player = event.player;
        RenderGlobal rg = event.context;
        ItemStack is = event.currentItem;
        float partialTicks = event.partialTicks;
        DimensionSliceEntity dse = _hitSlice;
        Coord corner = dse.getCorner();
        Quaternion rotation = dse.getRotation();
        if (!rotation.isZero() || !dse.prevTickRotation.isZero()) {
            rotation = dse.prevTickRotation.slerp(rotation, partialTicks);
        }
        try {
            // NORELEASE: Has partial tick/lag problems
            GL11.glPushMatrix();
            setShadowWorld();
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            if (Core.dev_environ) {
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                GL11.glColorMask(false, true, true, true);
            }
            GL11.glTranslated(
                    FzUtil.interp(dse.lastTickPosX - player.lastTickPosX, dse.posX - player.posX, partialTicks),
                    FzUtil.interp(dse.lastTickPosY - player.lastTickPosY, dse.posY - player.posY, partialTicks),
                    FzUtil.interp(dse.lastTickPosZ - player.lastTickPosZ, dse.posZ - player.posZ, partialTicks));
            Quaternion rot = new Quaternion(rotation);
            rot.incrNormalize();
            rot.glRotate();
            Vec3 centerOffset = dse.getRotationalCenterOffset();
            GL11.glTranslated(
                    -centerOffset.xCoord - corner.x,
                    -centerOffset.yCoord - corner.y,
                    -centerOffset.zCoord - corner.z);
            
            double savePlayerX = player.posX;
            double savePlayerY = player.posY;
            double savePlayerZ = player.posZ;
            partialTicks = 1;
            player.posX = player.posY = player.posZ = 0;
            if (!ForgeHooksClient.onDrawBlockHighlight(rg, player, shadowSelected, shadowSelected.subHit, is, partialTicks)) {
                rg.drawSelectionBox(player, shadowSelected, 0, partialTicks);
            }
            player.posX = savePlayerX;
            player.posY = savePlayerY;
            player.posZ = savePlayerZ;
        } finally {
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            restoreRealWorld();
            GL11.glPopMatrix();
            if (Core.dev_environ) {
                GL11.glColorMask(true, true, true, true);
                GL11.glEnable(GL11.GL_DEPTH_TEST);
            }
        }
    }
    
    @Override
    void updateRayPosition(DseRayTarget ray) {
        if (ray.parent.metaAABB == null) return;
        // If we didn't care about entities, we could call:
        //    mc.renderViewEntity.rayTrace(reachDistance, partialTicks)
        // But we need entities, so we'll just invoke MC's ray trace code.
        
        // Save values
        MovingObjectPosition origMouseOver = mc.objectMouseOver;
        mc.objectMouseOver = null; // The ray trace function will do the wrong thing if this isn't nulled.
        
        // Change the player's look to shadow rotation
        // (Hmm, this could probably be done better)
        EntityPlayer player = mc.thePlayer;
        Vec3 realPos = player.getPosition(1);
        Vec3 tmp = player.getLookVec();
        FzUtil.incrAdd(tmp, realPos);
        Vec3 realLookEnd = tmp;
        Vec3 shadowPos = ray.parent.real2shadow(realPos); // This used to be the raw ent pos, which isn't the same.
        Vec3 tmp_shadowLookEnd = ray.parent.real2shadow(realLookEnd);
        FzUtil.incrSubtract(tmp_shadowLookEnd, shadowPos);
        Vec3 shadowLook = tmp_shadowLookEnd;
        double xz_len = Math.hypot(shadowLook.xCoord, shadowLook.zCoord);
        double shadow_pitch = -Math.toDegrees(Math.atan2(shadowLook.yCoord, xz_len)); // erm, negative? Dunno.
        double shadow_yaw = Math.toDegrees(Math.atan2(-shadowLook.xCoord, shadowLook.zCoord)); // Another weird negative!
        
        try {
            MovingObjectPosition mop = null;
            AxisAlignedBB mopBox = null;
            setShadowWorld();
            try {
                mc.thePlayer.posX = shadowPos.xCoord;
                mc.thePlayer.posY = shadowPos.yCoord;
                mc.thePlayer.posZ = shadowPos.zCoord;
                mc.thePlayer.rotationPitch = (float) shadow_pitch;
                mc.thePlayer.rotationYaw = (float) shadow_yaw;
                
                WorldSettings.GameType origType = mc.playerController.currentGameType;
                mc.playerController.currentGameType = WorldSettings.GameType.CREATIVE;
                try {
                    mc.entityRenderer.getMouseOver(1F);
                } finally {
                    mc.playerController.currentGameType = origType;
                }
                mop = mc.objectMouseOver;
                if (mop == null) {
                    return;
                }
                switch (mop.typeOfHit) {
                case ENTITY:
                    mopBox = mop.entityHit.boundingBox;
                    break;
                case BLOCK:
                    World w = DeltaChunk.getClientShadowWorld();
                    Block block = w.getBlock(mop.blockX, mop.blockY, mop.blockZ);
                    mopBox = block.getSelectedBoundingBoxFromPool(w, mop.blockX, mop.blockY, mop.blockZ);
                    break;
                default: return;
                }
            } finally {
                restoreRealWorld();
            }
            if (mopBox == null) {
                ray.setPosition(0, -1000, 0);
                return;
            }
            // Create a realbox that contains the entirety of the shadowbox
            Vec3 min, max;
            {
                Vec3 corners[] = FzUtil.getCorners(mopBox);
                min = ray.parent.shadow2real(corners[0]);
                max = min.addVector(0, 0, 0);
                for (int i = 1; i < corners.length; i++) {
                    Vec3 c = ray.parent.shadow2real(corners[i]);
                    min.xCoord = Math.min(c.xCoord, min.xCoord);
                    min.yCoord = Math.min(c.yCoord, min.yCoord);
                    min.zCoord = Math.min(c.zCoord, min.zCoord);
                    max.xCoord = Math.max(c.xCoord, max.xCoord);
                    max.yCoord = Math.max(c.yCoord, max.yCoord);
                    max.zCoord = Math.max(c.zCoord, max.zCoord);
                }
            }
            ray.setPosition((min.xCoord + max.xCoord) / 2, (min.yCoord + max.yCoord) / 2, (min.zCoord + max.zCoord) / 2);
            FzUtil.setMin(ray.boundingBox, min);
            FzUtil.setMax(ray.boundingBox, max);
            offerHit(mop, ray, mopBox);
        } finally {
            mc.objectMouseOver = origMouseOver;
        }
    }
    
    @Override
    MovingObjectPosition getShadowHit() {
        return _shadowSelected;
    }
    
    @Override
    IDeltaChunk getHitIDC() {
        return _hitSlice;
    }
}
