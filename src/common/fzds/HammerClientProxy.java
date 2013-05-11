package factorization.fzds;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Iterator;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.client.multiplayer.NetClientHandler;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.NetHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.Packet14BlockDig;
import net.minecraft.network.packet.Packet1Login;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumMovingObjectType;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.IWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.event.ForgeSubscribe;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import factorization.api.Coord;
import factorization.client.render.EmptyRender;
import factorization.common.Core;
import factorization.common.FactorizationUtil;
import factorization.fzds.api.IDeltaChunk;

public class HammerClientProxy extends HammerProxy {
    public HammerClientProxy() {
        RenderingRegistry.registerEntityRenderingHandler(DseCollider.class, new EmptyRender());
        RenderDimensionSliceEntity rwe = new RenderDimensionSliceEntity();
        RenderingRegistry.registerEntityRenderingHandler(DimensionSliceEntity.class, rwe);
        TickRegistry.registerScheduledTickHandler(rwe, Side.CLIENT);
    }
    
    //These two classes below make it easy to see in a debugger.
    public static class HammerChunkProviderClient extends ChunkProviderClient {
        public HammerChunkProviderClient(World par1World) {
            super(par1World);
        }
    }
    
    public static class HammerWorldClient extends WorldClient {
        public HammerWorldClient(NetClientHandler par1NetClientHandler, WorldSettings par2WorldSettings, int par3, int par4, Profiler par5Profiler) {
            super(par1NetClientHandler, par2WorldSettings, par3, par4, par5Profiler, Minecraft.getMinecraft().getLogAgent());
        }
        
        @Override
        public void playSoundAtEntity(Entity par1Entity, String par2Str, float par3, float par4) {
            super.playSoundAtEntity(par1Entity, par2Str, par3, par4);
        }
        
        public void clearAccesses() {
            worldAccesses.clear();
        }
    }
    
    @Override
    public World getClientRealWorld() {
        if (real_world == null) {
            return Minecraft.getMinecraft().theWorld;
        }
        return real_world;
    }
    
    /***
     * Inspired, obviously, by RenderGlobal.
     * The World has a list of IWorldAccess, which it passes various events to. This one
     * TODO: Separate file  
     */
    static class ShadowRenderGlobal implements IWorldAccess {
        private World realWorld;
        public ShadowRenderGlobal(World realWorld) {
            this.realWorld = realWorld;
        }
        //The coord arguments are always in shadowspace.
        
        @Override
        public void markBlockForUpdate(int var1, int var2, int var3) {
            markBlocksForUpdate(var1 - 1, var2 - 1, var3 - 1, var1 + 1, var2 + 1, var3 + 1);
        }

        @Override
        public void markBlockForRenderUpdate(int var1, int var2, int var3) {
            markBlocksForUpdate(var1 - 1, var2 - 1, var3 - 1, var1 + 1, var2 + 1, var3 + 1);
        }

        @Override
        public void markBlockRangeForRenderUpdate(int var1, int var2, int var3,
                int var4, int var5, int var6) {
            markBlocksForUpdate(var1 - 1, var2 - 1, var3 - 1, var4 + 1, var5 + 1, var6 + 1);
        }
        
        void markBlocksForUpdate(int lx, int ly, int lz, int hx, int hy, int hz) {
            //Could this be more efficient?
            World realClientWorld = DeltaChunk.getClientRealWorld();
            Iterator<IDeltaChunk> it = DeltaChunk.getSlices(realClientWorld).iterator();
            while (it.hasNext()) {
                DimensionSliceEntity dse = (DimensionSliceEntity) it.next();
                if (dse.isDead) {
                    it.remove(); //shouldn't happen. Keeping it anyways.
                    continue;
                }
                
                Coord near = dse.getCorner(), far = dse.getFarCorner();
                if (FactorizationUtil.intersect(near.x, far.x, lx, hx)
                        && FactorizationUtil.intersect(near.y, far.y, ly, hy)
                        && FactorizationUtil.intersect(near.z, far.z, lz, hz)) {
                    RenderDimensionSliceEntity.markBlocksForUpdate((DimensionSliceEntity) dse, lx, ly, lz, hx, hy, hz);
                    dse.blocksChanged(lx, ly, lz);
                    dse.blocksChanged(hx, hy, hz);
                }
            }
        }

        @Override
        public void playSound(String sound, double x, double y, double z, float volume, float pitch) {
            Vec3 realCoords = DeltaChunk.shadow2nearestReal(Minecraft.getMinecraft().thePlayer, x, y, z);
            if (realCoords == null) {
                return;
            }
            realWorld.playSound(realCoords.xCoord, realCoords.yCoord, realCoords.zCoord, sound, volume, pitch, false);
        }
        
        @Override
        public void playSoundToNearExcept(EntityPlayer entityplayer, String s, double d0, double d1, double d2, float f, float f1) { }

        @Override
        public void spawnParticle(String particle, double x, double y, double z, double vx, double vy, double vz) {
            Vec3 realCoords = DeltaChunk.shadow2nearestReal(Minecraft.getMinecraft().thePlayer, x, y, z);
            if (realCoords == null) {
                return;
            }
            realWorld.spawnParticle(particle, realCoords.xCoord, realCoords.yCoord, realCoords.zCoord, vx, vy, vz);
        }
        
        @Override
        public void onEntityCreate(Entity entity) {
            Minecraft.getMinecraft().renderGlobal.onEntityCreate(entity);
        }
        
        @Override
        public void onEntityDestroy(Entity entity) {
            Minecraft.getMinecraft().renderGlobal.onEntityDestroy(entity);
        }

        @Override
        public void playRecord(String recordName, int x, int y, int z) {
            Vec3 realCoords = DeltaChunk.shadow2nearestReal(Minecraft.getMinecraft().thePlayer, x, y, z);
            if (realCoords == null) {
                return;
            }
            Minecraft.getMinecraft().renderGlobal.playRecord(recordName, (int)realCoords.xCoord, (int)realCoords.yCoord, (int)realCoords.zCoord);
        }

        @Override
        public void broadcastSound(int var1, int var2, int var3, int var4,
                int var5) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void playAuxSFX(EntityPlayer var1, int var2, int var3, int var4,
                int var5, int var6) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void destroyBlockPartially(int var1, int var2, int var3,
                int var4, int var5) {
            // TODO Auto-generated method stub
            //Prooooobably not.
        }
        
    }
    
    private static World lastWorld = null;
    @Override
    public void checkForWorldChange() {
        WorldClient currentWorld = Minecraft.getMinecraft().theWorld;
        if (currentWorld != lastWorld) {
            lastWorld = currentWorld;
            if (lastWorld == null) {
                return;
            }
            ((HammerWorldClient)Hammer.worldClient).clearAccesses();
            Hammer.worldClient.addWorldAccess(new ShadowRenderGlobal(currentWorld));
        }
    }
    
    @Override
    public void clientLogin(NetHandler clientHandler, INetworkManager manager, Packet1Login login) {
        if (Core.enable_dimension_slice && FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
            NetClientHandler nch = (NetClientHandler) clientHandler;
            Hammer.worldClient = new HammerWorldClient(nch,
                    new WorldSettings(0L, login.gameType, false, login.hardcoreMode, login.terrainType),
                    Hammer.dimensionID,
                    login.difficultySetting,
                    Core.proxy.getProfiler());
            send_queue = Minecraft.getMinecraft().thePlayer.sendQueue;
            NCH_class = (Class<NetClientHandler>)send_queue.getClass();
            NCH_worldClient_field = ReflectionHelper.findField(NCH_class, "worldClient", "field_72564_i", "i");
            Minecraft mc = Minecraft.getMinecraft();
        }
    }
    
    @Override
    public void clientLogout(INetworkManager manager) {
        //TODO: what else we can do here to cleanup?
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
            if (Hammer.worldClient != null) {
                ((HammerWorldClient)Hammer.worldClient).clearAccesses();
            }
            Hammer.worldClient = null;
            send_queue = null;
            fake_player = null;
        }
    }
    
    private static NetClientHandler send_queue;
    private static Class<NetClientHandler> NCH_class;
    private static Field NCH_worldClient_field;
    
    private void setSendQueueWorld(WorldClient wc) {
        try {
            NCH_worldClient_field.set(send_queue, wc);
        } catch (IllegalArgumentException e) {
            new RuntimeException("Failed to set SendQueue world due to reflection failure", e);
        } catch (IllegalAccessException e) {
            new RuntimeException("Failed to set SendQueue world due to reflection failure", e);
        }
    }
    
    private void setWorldAndPlayer(WorldClient wc, EntityClientPlayerMP player) {
        Minecraft mc = Minecraft.getMinecraft();
        if (wc == null) {
            Core.logSevere("Setting client world to null. Remember: Crashing is fun!");
        }
        //For logic
        mc.theWorld = wc;
        mc.thePlayer = player;
        mc.thePlayer.worldObj = wc;
        setSendQueueWorld(wc);
        
        //For rendering
        mc.renderViewEntity = player; //TODO NOTE: This make mess up in third person!
        if (TileEntityRenderer.instance.worldObj != null) {
            TileEntityRenderer.instance.worldObj = wc;
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
            //TODO NORELEASE: Cache
            fake_player = new EntityClientPlayerMP(mc, mc.theWorld /* why is this real world? */, mc.session, real_player.sendQueue /* not sure about this one. */);
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
    
    @Override
    public void runShadowTick() {
        if (Minecraft.getMinecraft().isGamePaused) {
            return;
        }
        WorldClient w = (WorldClient) DeltaChunk.getClientShadowWorld();
        if (w == null) {
            return;
        }
        setShadowWorld();
        Core.profileStart("FZ.DStick");
        try {
            //Inspired by Minecraft.runTick()
            w.updateEntities();
            w.func_73029_E(32, 7, 32);
        } finally {
            Core.profileEnd();
            restoreRealWorld();
        }
        
    }
    
    @Override
    public void clientInit() {
        
    }
    
    MovingObjectPosition shadowSelected = null;
    DseRayTarget rayTarget = null;
    AxisAlignedBB selectionBlockBounds = null;
    
    @ForgeSubscribe
    public void renderSelection(DrawBlockHighlightEvent event) {
        //System.out.println(event.target.hitVec);
        if (!(event.target.entityHit instanceof DseRayTarget)) {
            return;
        }
        if (shadowSelected == null) {
            return;
        }
        if (shadowSelected.typeOfHit != EnumMovingObjectType.TILE) {
            return;
        }
        if (event.isCanceled()) {
            return;
        }
        EntityPlayer player = event.player;
        RenderGlobal rg = event.context;
        ItemStack is = event.currentItem;
        float partialTicks = event.partialTicks;
        DimensionSliceEntity dse = rayTarget.parent;
        Coord here = null;
        if (selectionBlockBounds != null && shadowSelected.typeOfHit == EnumMovingObjectType.TILE) {
            here = new Coord(DeltaChunk.getClientShadowWorld(), shadowSelected.blockX, shadowSelected.blockY, shadowSelected.blockZ);
            here.getBlock().setBlockBounds(
                    (float)(selectionBlockBounds.minX - here.x), (float)(selectionBlockBounds.minY - here.y), (float)(selectionBlockBounds.minZ - here.z),
                    (float)(selectionBlockBounds.maxX - here.x), (float)(selectionBlockBounds.maxY - here.y), (float)(selectionBlockBounds.maxZ - here.z)
                );
        }
        //GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glPushMatrix();
        setShadowWorld();
        try {
            //TODO: Rotation transform
            Coord corner = dse.getCenter();
            GL11.glTranslatef(-corner.x, -corner.y, -corner.z);
            GL11.glTranslatef((float)(+dse.posX), (float)(+dse.posY), (float)(+dse.posZ));
            
            //Could glPushAttr for the mask. Nah.
            GL11.glColorMask(true, true, false, true);
            if (!ForgeHooksClient.onDrawBlockHighlight(rg, player, shadowSelected, shadowSelected.subHit, is, partialTicks)) {
                event.context.drawBlockBreaking(player, shadowSelected, 0, is, partialTicks);
                event.context.drawSelectionBox(player, shadowSelected, 0, is, partialTicks);
            }
        } finally {
            GL11.glColorMask(true, true, true, true);
            restoreRealWorld();
            GL11.glPopMatrix();
            //GL11.glEnable(GL11.GL_ALPHA_TEST);
        }
        //shadowSelected = null;
    }
    
    void updateRayPosition(DseRayTarget ray) {
        if (ray.parent.centerOffset == null) {
            return;
        }
        //mc.renderViewEntity.rayTrace(reachDistance, partialTicks) Just this function would work if we didn't care about entities.
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        double origX = player.posX;
        double origY = player.posY;
        double origZ = player.posZ;
        Vec3 shadowPos = ray.parent.real2shadow(Vec3.createVectorHelper(origX, origY, origZ));
        MovingObjectPosition origMouseOver = mc.objectMouseOver;
        final int pointedEntity_field_index = 6;
        Entity origPointed = ReflectionHelper.getPrivateValue(EntityRenderer.class, mc.entityRenderer, pointedEntity_field_index);
        //It's private! It's used in one function! Why is this even a field?
        
        try {
            AxisAlignedBB bb;
            Hammer.proxy.setShadowWorld();
            try {
                mc.thePlayer.posX = shadowPos.xCoord;
                mc.thePlayer.posY = shadowPos.yCoord;
                mc.thePlayer.posZ = shadowPos.zCoord;
                //TODO: Need to rotate the player if the DSE has rotated
                mc.thePlayer.rotationPitch = player.rotationPitch;
                mc.thePlayer.rotationYaw = player.rotationYaw;
                
                mc.entityRenderer.getMouseOver(1F);
                shadowSelected = mc.objectMouseOver;
                if (shadowSelected == null) {
                    rayTarget = null;
                    return;
                }
                switch (shadowSelected.typeOfHit) {
                case ENTITY:
                    bb = shadowSelected.entityHit.boundingBox;
                    selectionBlockBounds = null;
                    break;
                case TILE:
                    Coord hit = new Coord(DeltaChunk.getClientShadowWorld(), shadowSelected.blockX, shadowSelected.blockY, shadowSelected.blockZ);
                    Block block = hit.getBlock();
                    bb = block.getSelectedBoundingBoxFromPool(hit.w, hit.x, hit.y, hit.z);
                    selectionBlockBounds = bb;
                    break;
                default: return;
                }
            } finally {
                Hammer.proxy.restoreRealWorld();
            }
            //TODO: Rotations!
            if (bb == null) {
                System.out.println("NORELEASE?");
            }
            Vec3 min = ray.parent.shadow2real(FactorizationUtil.getMin(bb));
            Vec3 max = ray.parent.shadow2real(FactorizationUtil.getMax(bb));
            FactorizationUtil.setMin(ray.boundingBox, min);
            FactorizationUtil.setMax(ray.boundingBox, max);
            rayTarget = ray;
        } finally {
            mc.objectMouseOver = origMouseOver;
            ReflectionHelper.setPrivateValue(EntityRenderer.class, mc.entityRenderer, origPointed, pointedEntity_field_index);
        }
    }
    
    @Override
    MovingObjectPosition getShadowHit() {
        return shadowSelected;
    }
    
    @Override
    void mineBlock(final MovingObjectPosition mop) {
        final Minecraft mc = Minecraft.getMinecraft();
        final EntityClientPlayerMP player = mc.thePlayer;
        final PlayerControllerMP origController = mc.playerController;
        
        mc.playerController = new PlayerControllerMP(mc, player.sendQueue) {
            void pushWrapper() {
                setShadowWorld();
            }
            
            void popWrapper() {
                restoreRealWorld();
            }
            
            void sendDigPacket(Packet toWrap) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos);
                try {
                    dos.write(toWrap.getPacketId());
                    toWrap.writePacketData(dos);
                } catch (IOException e) {
                    e.printStackTrace(); //Isn't there some guava thing for this?
                }
                Packet toSend = PacketDispatcher.getTinyPacket(Hammer.instance, HammerNet.HammerNetType.digPacket, baos.toByteArray());
                System.out.println("SEND: " + toWrap); //NORELEASE
                PacketDispatcher.sendPacketToServer(toSend);
                
            }
            
            void resetController() {
                mc.playerController = origController;
                System.out.println("Resetting controller"); //NORELEASE
            }
            
            //NOTE: Incomplete, because java sucks. Are there any other functions we'll need?
            
            @Override
            public void resetBlockRemoving() {
                System.out.println("HCP.PCMP.resetBlockRemoving"); //NORELEASE
                sendDigPacket(new Packet14BlockDig(1, mop.blockX, mop.blockY, mop.blockZ, mop.sideHit));
                resetController();
            }
            
            @Override
            public void clickBlock(int x, int y, int z, int side) {
                System.out.println("HCP.PCMP.clickBlock"); //NORELEASE
                //Unlike vanilla's, this should only be called during the mining. Not at the start. Not at the cancellation. Not when we switch to a different one. (HOPEFULLY)
                pushWrapper();
                try {
                    clickBlock_implementation(x, y, z, side);
                } finally {
                    popWrapper();
                }
            }
            
            public void clickBlock_implementation(int x, int y, int z, int side) {
                //Very terribly copied from clickBlock with this change: sendDigPacket instead of using addToSendQueue
                //And also adding resetController call after the block gets broken
                if (!this.currentGameType.isAdventure() || mc.thePlayer.canCurrentToolHarvestBlock(x, y, z))
                {
                    if (this.currentGameType.isCreative())
                    {
                        sendDigPacket(new Packet14BlockDig(0, x, y, z, side));
                        clickBlockCreative(this.mc, this, x, y, z, side);
                        this.blockHitDelay = 5;
                    }
                    else if (!this.isHittingBlock || !this.func_85182_a(x, y, z) /* sameToolAndBlock */)
                    {
                        if (this.isHittingBlock)
                        {
                            sendDigPacket(new Packet14BlockDig(1, this.currentBlockX, this.currentBlockY, this.currentblockZ, side));
                        }

                        sendDigPacket(new Packet14BlockDig(0, x, y, z, side));
                        int i1 = this.mc.theWorld.getBlockId(x, y, z);

                        if (i1 > 0 && this.curBlockDamageMP == 0.0F)
                        {
                            Block.blocksList[i1].onBlockClicked(this.mc.theWorld, x, y, z, this.mc.thePlayer);
                        }

                        if (i1 > 0 && Block.blocksList[i1].getPlayerRelativeBlockHardness(this.mc.thePlayer, this.mc.thePlayer.worldObj, x, y, z) >= 1.0F)
                        {
                            this.onPlayerDestroyBlock(x, y, z, side);
                            resetController();
                        }
                        else
                        {
                            this.isHittingBlock = true;
                            this.currentBlockX = x;
                            this.currentBlockY = y;
                            this.currentblockZ = z;
                            this.field_85183_f = this.mc.thePlayer.getHeldItem();
                            this.curBlockDamageMP = 0.0F;
                            this.stepSoundTickCounter = 0.0F;
                            this.mc.theWorld.destroyBlockInWorldPartially(this.mc.thePlayer.entityId, this.currentBlockX, this.currentBlockY, this.currentblockZ, (int)(this.curBlockDamageMP * 10.0F) - 1);
                        }
                    }
                }
            }
            
            
            
            @Override
            public void attackEntity(EntityPlayer par1EntityPlayer, Entity par2Entity) {
                //likely will need stuff in here
                // TODO Auto-generated method stub
                System.out.println("HCP.PCMP.attackEntity"); //NORELEASE
                super.attackEntity(par1EntityPlayer, par2Entity);
            }
        };
        mc.playerController.clickBlock(mop.blockX, mop.blockY, mop.blockZ, mop.sideHit);
    }
}
