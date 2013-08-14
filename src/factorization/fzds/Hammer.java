package factorization.fzds;

import java.io.File;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.NetLoginHandler;
import net.minecraft.network.packet.NetHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.Packet1Login;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.util.Vec3;
import net.minecraft.world.IWorldAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.IScheduledTickHandler;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.TickType;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.network.IConnectionHandler;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.Player;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;
import factorization.api.Coord;
import factorization.common.Core;
import factorization.common.FzConfig;
import factorization.common.WeakSet;
import factorization.fzds.api.IDeltaChunk;

@Mod(modid = Hammer.modId, name = Hammer.name, version = Core.version, dependencies = "required-after: " + Core.modId)
@NetworkMod(clientSideRequired = true, tinyPacketHandler = HammerNet.class)
public class Hammer {
    final String[] Lore = new String[] {
            "At twilight's end, the shadow's crossed,",
            "A new world birthed, the elder lost.",
            "Yet on the morn we wake to find",
            "That mem'ry left so far behind.",
            "To deafened ears we ask, unseen,",
            "“Which is life and which the dream?”"
    };
    
    
    public static final String modId = Core.modId + ".dimensionalSlice";
    public static final String name = "Factorization Dimensional Slices";
    public static Hammer instance; //@Instance seems to give the parent?
    @SidedProxy(clientSide = "factorization.fzds.HammerClientProxy", serverSide = "factorization.fzds.HammerProxy")
    public static HammerProxy proxy;
    public static boolean enabled;
    public static int dimensionID;
    public static World worldClient = null; //This is actually a WorldClient that is actually HammerClientProxy.HammerWorldClient
    public static double DSE_ChunkUpdateRangeSquared = Math.pow(16*8, 2); //This is actually set when the server starts
    public static int fzds_command_channel = 0;
    public static int max_fzds_grab_area = 16*16*80*4;
    
    static Set<IDeltaChunk> serverSlices = new WeakSet(), clientSlices = new WeakSet();
    
    public Hammer() {
        Hammer.instance = this;
    }
    
    private final static EnumSet<TickType> serverTicks = EnumSet.of(TickType.SERVER);
    final static HammerInfo hammerInfo = new HammerInfo();
    static final int channelWidth = 16*50;
    
    @EventHandler
    public void setup(FMLPreInitializationEvent event) {
        event.getModMetadata().parent = Core.modId;
        enabled = FzConfig.enable_dimension_slice;
        if (!enabled) {
            return;
        }
        
        EntityRegistry.registerModEntity(DimensionSliceEntity.class, "fzds", 1, this, 64, 1, true);
        EntityRegistry.registerModEntity(DseCollider.class, "fzdsC", 2, this, 64, 80000, false);
        
        //Create the hammer dimension
        dimensionID = FzConfig.dimension_slice_dimid;
        DimensionManager.registerProviderType(dimensionID, HammerWorldProvider.class, true);
        DimensionManager.registerDimension(dimensionID, dimensionID);
        File base = event.getSuggestedConfigurationFile().getParentFile();
        hammerInfo.setConfigFile(new File(base, "hammerChannels.cfg"));
        fzds_command_channel = hammerInfo.makeChannelFor(this, "cmd", fzds_command_channel, -1, "This channel is used for Slices created using the /fzds command");
        Packet.addIdClassMapping(220, true /* client side */, true /* server side */, Packet220FzdsWrap.class);
        
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            //When the client logs in or out, we need to do something to the shadow world
            proxy.clientInit();
            NetworkRegistry.instance().registerConnectionHandler(new IConnectionHandler() {
                @Override
                public void playerLoggedIn(Player player, NetHandler netHandler, INetworkManager manager) { }

                @Override
                public String connectionReceived(NetLoginHandler netHandler, INetworkManager manager) { return null; }
                
                @Override
                public void connectionOpened(NetHandler netClientHandler, MinecraftServer server, INetworkManager manager) { }
                
                @Override
                public void connectionOpened(NetHandler netClientHandler, String server, int port, INetworkManager manager) { }
                
                @Override
                public void connectionClosed(INetworkManager manager) {
                    Hammer.proxy.clientLogout(manager);
                }
                
                @Override
                public void clientLoggedIn(NetHandler clientHandler, INetworkManager manager, Packet1Login login) {
                    Hammer.proxy.clientLogin(clientHandler, manager, login);
                }
            });
            
            TickRegistry.registerTickHandler(new ITickHandler() {
                EnumSet<TickType> clientTick = EnumSet.of(TickType.CLIENT);
                @Override
                public EnumSet<TickType> ticks() {
                    return clientTick;
                }
                
                @Override
                public void tickStart(EnumSet<TickType> type, Object... tickData) {
                    proxy.checkForWorldChange();
                }
                
                @Override
                public void tickEnd(EnumSet<TickType> type, Object... tickData) {
                    proxy.runShadowTick();
                }
                
                @Override
                public String getLabel() {
                    return "FZ Hammer client tick";
                }
            }, Side.CLIENT);
        }
        
        //This sets up saving how many IDs we've used
        TickRegistry.registerScheduledTickHandler(new IScheduledTickHandler() {
            @Override
            public EnumSet<TickType> ticks() { return serverTicks; }
            
            @Override
            public String getLabel() {
                return "FZDS save info";
            }
            
            @Override
            public int nextTickSpacing() {
                return 5*20;
            }
            
            @Override
            public void tickStart(EnumSet<TickType> type, Object... tickData) { }
            
            @Override
            public void tickEnd(EnumSet<TickType> type, Object... tickData) {
                hammerInfo.saveCellAllocations();
            }
        }, Side.SERVER);
        MinecraftForge.EVENT_BUS.register(hammerInfo);
        MinecraftForge.EVENT_BUS.register(proxy);
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            MinecraftForge.EVENT_BUS.register(new DseRayTarget.ClickHandler());
        }
    }
    
    @EventHandler
    public void setMainServerThread(FMLServerStartingEvent event) {
        if (!enabled) {
            return;
        }
        event.registerServerCommand(new FZDSCommand());
        DimensionManager.initDimension(dimensionID);
        assert DimensionManager.shouldLoadSpawn(dimensionID);
        World hammerWorld = DimensionManager.getWorld(dimensionID);
        hammerWorld.addWorldAccess(new IWorldAccess() {
            //TODO: Move to file; mix with Client Proxy's version
            //Should lets DSEs know that they need to update their area when a block is changed 
            @Override public void spawnParticle(String var1, double var2, double var4, double var6, double var8, double var10, double var12) { }
            @Override public void onEntityCreate(Entity entity) { }
            @Override public void onEntityDestroy(Entity entity) { }
            @Override public void playSound(String var1, double var2, double var4, double var6, float var8, float var9) { }
            @Override public void playRecord(String var1, int var2, int var3, int var4) { }
            @Override public void playAuxSFX(EntityPlayer var1, int var2, int var3, int var4, int var5, int var6) { }
            
            @Override
            public void markBlockRangeForRenderUpdate(int lx, int ly, int lz, int hx, int hy, int hz) {
                markBlocksForUpdate(lx, ly, lz, hx, hy, hz);
            }
            
            @Override
            public void markBlockForUpdate(int x, int y, int z) {
                markBlocksForUpdate(x, y, z, x, y, z);
            }
            
            @Override
            public void markBlockForRenderUpdate(int x, int y, int z) {
                markBlocksForUpdate(x, y, z, x, y, z);
            }
            
            Coord center = new Coord(DeltaChunk.getClientShadowWorld(), 0, 0, 0);
            void markBlocksForUpdate(int lx, int ly, int lz, int hx, int hy, int hz) {
                //Sorry, it could probably be a bit more efficient.
                Coord lower = new Coord(null, lx, ly, lz);
                Coord upper = new Coord(null, hx, hy, hz);
                World realClientWorld = DeltaChunk.getClientRealWorld();
                Iterator<IDeltaChunk> it = DeltaChunk.getSlices(realClientWorld).iterator();
                while (it.hasNext()) {
                    DimensionSliceEntity dse = (DimensionSliceEntity) it.next();
                    if (dse.isDead) {
                        it.remove(); //shouldn't happen. Keeping it anyways.
                        continue;
                    }
                    if (dse.getCorner().inside(lower, upper) || dse.getFarCorner().inside(lower, upper)) {
                        dse.blocksChanged(lx, ly, lz);
                        dse.blocksChanged(hx, hy, hz);
                    }
                }
            }
            
            
            @Override
            public void playSoundToNearExcept(EntityPlayer entityplayer, String s, double d0, double d1, double d2, float f, float f1) { }
            @Override public void destroyBlockPartially(int var1, int var2, int var3, int var4, int var5) { }
            @Override public void broadcastSound(int var1, int var2, int var3, int var4, int var5) { }
        });
        int view_distance = MinecraftServer.getServer().getConfigurationManager().getViewDistance();
        //the undeobfed method comes after "isPlayerWatchingChunk", also in uses of ServerConfigurationManager.getViewDistance()
        //It returns how many blocks are visible.
        DSE_ChunkUpdateRangeSquared = Math.pow(PlayerManager.getFurthestViewableBlock(view_distance) + 16*2, 2);
    }
    
    @EventHandler
    public void saveInfo(FMLServerStoppingEvent event) {
        hammerInfo.saveCellAllocations();
        serverSlices.clear();
        clientSlices.clear();
    }
    
    public static Vec3 ent2vec(Entity ent) {
        return ent.worldObj.getWorldVec3Pool().getVecFromPool(ent.posX, ent.posY, ent.posZ);
    }
    
    @EventHandler
    public void modsLoaded(FMLPostInitializationEvent event) {
        double desired_radius = 16/2;
        if (FzConfig.force_max_entity_radius >= 0 && FzConfig.force_max_entity_radius < desired_radius) {
            desired_radius = FzConfig.force_max_entity_radius;
            Core.logFine("Using %f as FZDS's maximum entity radius; this could cause failure to collide with FZDS entities");
        }
        if (World.MAX_ENTITY_RADIUS < desired_radius) {
            Core.logFine("Enlarging World.MAX_ENTITY_RADIUS from %f to %f", World.MAX_ENTITY_RADIUS, desired_radius);
            Core.logFine("Please let the author know if this causes problems.");
            World.MAX_ENTITY_RADIUS = desired_radius;
        } else {
            Core.logFine("World.MAX_ENTITY_RADIUS was already set to %f, which is large enough for our purposes (%f)", World.MAX_ENTITY_RADIUS, desired_radius);
        }
    }
}
