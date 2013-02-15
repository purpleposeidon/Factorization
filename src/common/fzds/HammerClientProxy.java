package factorization.fzds;

import java.lang.reflect.Field;
import java.util.Iterator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.client.multiplayer.NetClientHandler;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.NetHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.Packet1Login;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.Vec3;
import net.minecraft.world.IWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import factorization.api.Coord;
import factorization.client.render.EmptyRender;
import factorization.common.Core;

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
            super(par1NetClientHandler, par2WorldSettings, par3, par4, par5Profiler);
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
    
    @Override
    public World getOppositeWorld() {
        if (reverse_shadow_world != null) {
            return reverse_shadow_world;
        }
        return Hammer.getClientShadowWorld();
    }
    
    /***
     * Inspired, obviously, by RenderGlobal.
     * The World has a list of IWorldAccess, which it passes various events to. This one  
     */
    static class HammerRenderGlobal implements IWorldAccess {
        private World realWorld;
        public HammerRenderGlobal(World realWorld) {
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
        
        Coord center = new Coord(Hammer.getClientShadowWorld(), 0, 0, 0);
        void markBlocksForUpdate(int lx, int ly, int lz, int hx, int hy, int hz) {
            //Sorry, it could probably be a bit more efficient.
            World realClientWorld = Hammer.getClientRealWorld();
            World shadow = Hammer.getWorld(realClientWorld);
            center.set(shadow, (lx + hx)/2, (ly + hy)/2, (lz + hz)/2);
            int cellId = Hammer.getIdFromCoord(center);
            if (cellId < 0) {
                return;
            }
            Iterator<DimensionSliceEntity> it = Hammer.getSlices(realClientWorld).iterator();
            while (it.hasNext()) {
                DimensionSliceEntity dse = it.next();
                if (dse.isDead) {
                    it.remove(); //should be handled now. Keeping it anyways.
                    continue;
                }
                if (dse.cell == cellId && dse.worldObj == realClientWorld) {
                    RenderDimensionSliceEntity.markBlocksForUpdate(dse, lx, ly, lz, hx, hy, hz);
                    dse.blocksChanged(lx, ly, lz);
                    dse.blocksChanged(hx, hy, hz);
                }
            }
        }

        @Override
        public void playSound(String sound, double x, double y, double z, float volume, float pitch) {
            Vec3 realCoords = Hammer.shadow2nearestReal(Minecraft.getMinecraft().thePlayer, x, y, z);
            if (realCoords == null) {
                return;
            }
            realWorld.playSound(realCoords.xCoord, realCoords.yCoord, realCoords.zCoord, sound, volume, pitch, false);
        }

        @Override
        public void func_85102_a(EntityPlayer var1, String var2, double var3,
                double var5, double var7, float var9, float var10) {
            // TODO Auto-generated method stub
            //This is another sound-placing method; appears to be murder-related.
        }

        @Override
        public void spawnParticle(String particle, double x, double y, double z, double vx, double vy, double vz) {
            Vec3 realCoords = Hammer.shadow2nearestReal(Minecraft.getMinecraft().thePlayer, x, y, z);
            if (realCoords == null) {
                return;
            }
            realWorld.spawnParticle(particle, realCoords.xCoord, realCoords.yCoord, realCoords.zCoord, vx, vy, vz);
        }

        @Override
        public void obtainEntitySkin(Entity var1) {
            Minecraft.getMinecraft().renderGlobal.obtainEntitySkin(var1);
        }

        @Override
        public void releaseEntitySkin(Entity var1) {
            Minecraft.getMinecraft().renderGlobal.releaseEntitySkin(var1);
        }

        @Override
        public void playRecord(String recordName, int x, int y, int z) {
            Vec3 realCoords = Hammer.shadow2nearestReal(Minecraft.getMinecraft().thePlayer, x, y, z);
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
            Hammer.worldClient.addWorldAccess(new HammerRenderGlobal(currentWorld));
        }
    }
    
    @Override
    public void clientLogin(NetHandler clientHandler, INetworkManager manager, Packet1Login login) {
        if (Core.enable_dimension_slice) {
            NetClientHandler nch = (NetClientHandler) clientHandler;
            Hammer.worldClient = new HammerWorldClient(nch,
                    new WorldSettings(0L, login.gameType, false, login.hardcoreMode, login.terrainType),
                    Hammer.dimensionID,
                    login.difficultySetting,
                    Core.proxy.getProfiler());
            send_queue = Minecraft.getMinecraft().getSendQueue();
            NCH_class = (Class<NetClientHandler>)send_queue.getClass();
            NCH_worldClient_field = ReflectionHelper.findField(NCH_class, "worldClient", "i");
            Minecraft mc = Minecraft.getMinecraft();
        }
    }
    
    @Override
    public void clientLogout(INetworkManager manager) {
        //TODO: what else we can do here to cleanup?
        Hammer.worldClient = null;
        send_queue = null;
        fake_player = null;
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
            Core.logSevere("Client-side hammer world is null. Remember: Crashing is fun!");
        }
        //For logic
        mc.theWorld = wc;
        mc.thePlayer = player;
        mc.thePlayer.worldObj = wc;
        setSendQueueWorld(wc);
        
        //For rendering
        TileEntityRenderer.instance.worldObj = wc;
        RenderManager.instance.worldObj = wc;
    }
    
    EntityClientPlayerMP real_player = null;
    WorldClient real_world = null;
    EntityClientPlayerMP fake_player = null;
    
    WorldClient reverse_shadow_world = null; //the world that the DSE is in when the client player is embedded in the DSE
    
    @Override
    public void setClientWorld(World w) {
        //System.out.println("Setting world");
        Minecraft mc = Minecraft.getMinecraft();
        assert w != null;
        if (real_player == null) {
            real_player = mc.thePlayer;
        }
        if (real_world == null) {
            real_world = mc.theWorld;
        }
        real_player.worldObj = w;
        if (fake_player == null || real_world != fake_player.worldObj) {
            fake_player = new EntityClientPlayerMP(mc, mc.theWorld, mc.session, real_player.sendQueue /* not sure about this one. */);
        }
        setWorldAndPlayer((WorldClient) w, fake_player);
    }
    
    @Override
    public void restoreClientWorld() {
        //System.out.println("Restoring world");
        real_player.worldObj = real_world;
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
        WorldClient w = (WorldClient) Hammer.getClientShadowWorld();
        if (w == null) {
            return;
        }
        if (isInShadowWorld()) {
            return;
        }
        if (Minecraft.getMinecraft().isGamePaused) {
            return;
        }
        setClientWorld(w);
        Core.profileStart("FZ.DStick");
        try {
            //Inspired by Minecraft.runTick()
            w.updateEntities();
            w.func_73029_E(32, 7, 32);
        } finally {
            Core.profileEnd();
            restoreClientWorld();
        }
        
    }
    
    @Override
    public void clientInit() {
        Packet.addIdClassMapping(220, true /* client side */, false /* server side */, Packet220FzdsWrap.class);
    }
    
}
