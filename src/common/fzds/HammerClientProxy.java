package factorization.fzds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.client.multiplayer.NetClientHandler;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.NetHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.Packet1Login;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.profiler.Profiler;
import net.minecraft.world.IWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.chunk.IChunkProvider;
import factorization.api.Coord;
import factorization.common.Core;

public class HammerClientProxy extends HammerProxy {
    
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
    }
    
    @Override
    public World getClientRealWorld() {
        if (real_world != null) {
            return real_world;
        }
        return Minecraft.getMinecraft().theWorld;
    }
    
    static class HammerRenderGlobal implements IWorldAccess {
        private World realWorld;
        public HammerRenderGlobal(World realWorld) {
            this.realWorld = realWorld;
        }
        //The coord arguments are always in shadowspace.
        
        @Override
        public void markBlockForUpdate(int var1, int var2, int var3) {
            // TODO: *definitely* need something here...
            realWorld.markBlockForRenderUpdate(var1, var2, var3);
        }

        @Override
        public void markBlockForRenderUpdate(int var1, int var2, int var3) {
            // TODO: *definitely* need something here...
            realWorld.markBlockForRenderUpdate(var1, var2, var3);
        }

        @Override
        public void markBlockRangeForRenderUpdate(int var1, int var2, int var3,
                int var4, int var5, int var6) {
            // TODO: *definitely* need something here...
            realWorld.markBlockRangeForRenderUpdate(var1, var2, var3, var4, var5, var6);
        }

        @Override
        public void playSound(String sound, double x, double y, double z, float volume, float pitch) {
            synchronized (Hammer.slices) {
                World real_world = Hammer.getClientRealWorld();
                EntityPlayer player = Minecraft.getMinecraft().thePlayer;
                if (player == null || real_world == null) {
                    return;
                }
                DimensionSliceEntity closest = null;
                int correct_id = Hammer.getIdFromCoord(Coord.of(x, y, z));
                for (DimensionSliceEntity here : Hammer.slices) {
                    if (here.worldObj != real_world) {
                        continue;
                    }
                    
                }
            }
        }

        @Override
        public void func_85102_a(EntityPlayer var1, String var2, double var3,
                double var5, double var7, float var9, float var10) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void spawnParticle(String var1, double var2, double var4,
                double var6, double var8, double var10, double var12) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void obtainEntitySkin(Entity var1) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void releaseEntitySkin(Entity var1) {
            // TODO Auto-generated method stub
            
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
    public void clientLogin(NetHandler clientHandler, INetworkManager manager, Packet1Login login) {
        if (Core.enable_dimension_slice) {
            NetClientHandler nch = (NetClientHandler) clientHandler;
            Hammer.worldClient = new HammerWorldClient(nch,
                    new WorldSettings(0L, login.gameType, false, login.hardcoreMode, login.terrainType),
                    Hammer.dimensionID,
                    login.difficultySetting,
                    Core.proxy.getProfiler());
        }
    }
    
    @Override
    public void clientLogout(INetworkManager manager) {
        //TODO: what else we can do here to cleanup?
        Hammer.worldClient = null;
    }
    
    private void setWorldAndPlayer(WorldClient wc, EntityClientPlayerMP player) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.theWorld = wc;
        mc.thePlayer = player;
        TileEntityRenderer.instance.worldObj = wc;
        real_world.sendQueue.worldClient = wc; //NOTE: This will require an AT
        //((NetClientHandler)mc.thePlayer.sendQueue.netManager).worldClient = wc;
    }
    
    EntityClientPlayerMP real_player = null;
    WorldClient real_world = null;
    
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
        EntityClientPlayerMP fake_player = new EntityClientPlayerMP(mc, w, mc.session, real_player.sendQueue /* not sure about this one. */);
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
