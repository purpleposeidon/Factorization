package factorization.fzds;

import java.lang.reflect.Field;

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
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import factorization.api.Coord;
import factorization.common.Core;

public class HammerClientProxy extends HammerProxy {
    public HammerClientProxy() {
        MinecraftForge.EVENT_BUS.register(this);
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
    }
    
    @Override
    public World getClientRealWorld() {
        if (real_world != null) {
            return real_world;
        }
        return Minecraft.getMinecraft().theWorld;
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
            for (DimensionSliceEntity dse : Hammer.getSlices(realClientWorld)) {
                if (dse.cell == cellId && dse.worldObj == realClientWorld) {
                    RenderDimensionSliceEntity.markBlocksForUpdate(dse, lx, ly, lz, hx, hy, hz);
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
            //this doesn't happen because world doesn't get update when you switch...
            realWorld.spawnParticle(particle, realCoords.xCoord, realCoords.yCoord, realCoords.zCoord, vx, vy, vz);
        }

        @Override
        public void obtainEntitySkin(Entity var1) {
            // TODO: This is probably used mainly for player skins. Likely need (even more) more ATs
        }

        @Override
        public void releaseEntitySkin(Entity var1) {
            // TODO: This is probably used mainly for player skins. Likely need (even more) more ATs
        }

        @Override
        public void playRecord(String var1, int var2, int var3, int var4) {
            // TODO Auto-generated method stub
            
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
            
        }
        
    }
    
    @Override
    public void checkForWorldChange() {
        
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
            Hammer.worldClient.addWorldAccess(new HammerRenderGlobal(Minecraft.getMinecraft().theWorld));
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
            e.printStackTrace();
            new RuntimeException("Failed to set SendQueue world due to reflection failure");
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            new RuntimeException("Failed to set SendQueue world due to reflection failure");
        }
    }
    
    private void setWorldAndPlayer(WorldClient wc, EntityClientPlayerMP player) {
        Minecraft mc = Minecraft.getMinecraft();
        //For logic
        mc.theWorld = wc;
        mc.thePlayer = player;
        setSendQueueWorld(wc);
        
        //For rendering
        TileEntityRenderer.instance.worldObj = wc;
        RenderManager.instance.worldObj = wc;
    }
    
    EntityClientPlayerMP real_player = null;
    WorldClient real_world = null;
    EntityClientPlayerMP fake_player = null;
    
    @Override
    public void setClientWorld(World w) {
        Minecraft mc = Minecraft.getMinecraft();
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
