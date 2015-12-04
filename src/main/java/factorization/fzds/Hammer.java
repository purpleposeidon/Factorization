package factorization.fzds;

import factorization.fzds.network.FzdsPacketRegistry;
import factorization.fzds.network.HammerNet;
import factorization.fzds.network.PPPChunkLoader;
import factorization.fzds.network.WrappedPacket;
import factorization.shared.Core;
import factorization.util.FzUtil;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

@Mod(
        modid = Hammer.modId,
        name = Hammer.name,
        version = Core.version
)
public class Hammer {
    final String[] Lore = new String[] {
            "At twilight's end, the shadow's crossed,",
            "A new world birthed, the elder lost.",
            "Yet on the morn we wake to find",
            "That mem'ry left so far behind.",
            "To deafened ears we ask, unseen,",
            "“Which is life and which the dream?”" // -- Aaron Diaz, http://dresdencodak.com/2006/03/02/zhuangzi/
    };
    
    
    public static final String modId = Core.modId + ".dimensionalSlice";
    public static final String name = "Factorization Dimensional Slices";
    public static Hammer instance; //@Instance seems to give the parent?
    public static HammerNet net;
    @SidedProxy(clientSide = "factorization.fzds.HammerClientProxy", serverSide = "factorization.fzds.HammerProxy")
    public static HammerProxy proxy;
    public static World worldClient = null; //This is actually a WorldClient that is actually HammerClientProxy.HammerWorldClient
    public static double DSE_ChunkUpdateRangeSquared = Math.pow(16*8, 2); //This is actually set when the server starts
    public static int fzds_command_channel = 0;
    public static int max_fzds_grab_area = 16*16*80*4;
    public static int max_dse_collidable_chunk_area = 9*9*9;
    public static boolean log_client_chunking = Boolean.parseBoolean(System.getProperty("fzds.logClientChunks", "false"));
    
    static DeltaChunkMap serverSlices = new DeltaChunkMap(), clientSlices = new DeltaChunkMap();

    public Hammer() {
        Core.checkJar();
        Hammer.instance = this;
        if (!DeltaChunk.enabled()) return;
        Hammer.net = new HammerNet();
        Core.loadBus(this);
    }
    
    public static final HammerInfo hammerInfo = new HammerInfo();
    static final int channelWidth = 16*50;
    
    @EventHandler
    public void setup(FMLPreInitializationEvent event) {
        FzUtil.setCoreParent(event);
        if (!DeltaChunk.enabled()) return;
        final File configFile = event.getSuggestedConfigurationFile();
        File base = configFile.getParentFile();
        hammerInfo.setConfigFile(new File(base, "hammerChannels.cfg"));
        
        int client_despawn_distance = 16*10; //NORELEASE: This wants for a config setting. "How far away the a client must be from a DSE before the server will tell the client to forget about it (eg, client side despawn)."
        EntityRegistry.registerModEntity(DimensionSliceEntity.class, "fzds", 1, this, client_despawn_distance, 1, true);
        // Sigh. Would be nice to kill the velocity update packet noise. :/
        // EntityRegistry.registerModEntity(PacketProxyingPlayer.class, "fzds", 2, this, 1, Integer.MAX_VALUE, false);
        
        //Create the hammer dimension
        DimensionManager.registerProviderType(DeltaChunk.getDimensionId(), HammerWorldProvider.class, true);
        DimensionManager.registerDimension(DeltaChunk.getDimensionId(), DeltaChunk.getDimensionId());
        fzds_command_channel = hammerInfo.makeChannelFor(Core.modId, "fzdscmd", fzds_command_channel, -1, "This channel is used for Slices created using the /fzds command");
        FzdsPacketRegistry.init();
        
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            //When the client logs in or out, we need to do something to the shadow world
            proxy.clientInit();
        }
        
        //This sets up saving how many IDs we've used
        Core.loadBus(hammerInfo);
        Core.loadBus(proxy);
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            Core.loadBus(new ClickHandler());
        }
        WrappedPacket.registerPacket();
        ForgeChunkManager.setForcedChunkLoadingCallback(this, new PPPChunkLoader());
        if (log_client_chunking && FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            Core.loadBus(new ChunkLogger());
        }
    }

    @EventHandler
    public void finishLoad(FMLPostInitializationEvent event) {
        if (!DeltaChunk.enabled()) return;
        hammerInfo.saveChannelConfig();
    }
    
    @EventHandler
    public void serverStart(FMLServerStartingEvent event) {
        if (!DeltaChunk.enabled()) return;
        event.registerServerCommand(new FZDSCommand());
        DimensionManager.initDimension(DeltaChunk.getDimensionId());
        if (!DimensionManager.shouldLoadSpawn(DeltaChunk.getDimensionId())) {
            throw new RuntimeException("hammerWorld is not loaded");
        }
        if (!DimensionManager.isDimensionRegistered(DeltaChunk.getDimensionId())) {
            throw new RuntimeException("Hammer dimension was not registered!?");
        }
        World hammerWorld = DimensionManager.getWorld(DeltaChunk.getDimensionId());
        if (!(hammerWorld.provider instanceof HammerWorldProvider)) {
            throw new RuntimeException("Expected HammerWorldProvider for HammerWorld; is there a dimension ID conflict? Actual provider: " + hammerWorld.provider.getClass());
        }
        hammerWorld.addWorldAccess(new ServerShadowWorldAccess());
        int view_distance = MinecraftServer.getServer().getConfigurationManager().getViewDistance();
        //the undeobfed method comes after "isPlayerWatchingChunk", also in uses of ServerConfigurationManager.getViewDistance()
        //It returns how many blocks are visible.
        DSE_ChunkUpdateRangeSquared = Math.pow(PlayerManager.getFurthestViewableBlock(view_distance) + 16*2, 2);
    }
    
    @EventHandler
    public void saveInfo(FMLServerStoppingEvent event) {
        if (!DeltaChunk.enabled()) return;
        hammerInfo.saveCellAllocations();
        serverSlices.clear();
        clientSlices.clear();
    }

    @SubscribeEvent
    public void clearSlicesBeforeConnect(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        if (!DeltaChunk.enabled()) return;
        proxy.cleanupClientWorld();
    }

    @SubscribeEvent
    public void boostStepHeight(EntityJoinWorldEvent event) {
        Entity ent = event.entity;
        if (ent == null) return;
        if (ent.stepHeight <= 0.0F) return;
        ent.stepHeight += 1F / 128F;
    }

    private static Logger hammerLogger = LogManager.getLogger("FZ-Hammer-init");
    private void initializeLogging(Logger logger) {
        Hammer.hammerLogger = logger;
    }

    public static void logSevere(String format, Object... formatParameters) {
        hammerLogger.error(String.format(format, formatParameters));
    }

    public static void logWarning(String format, Object... formatParameters) {
        hammerLogger.warn(String.format(format, formatParameters));
    }

    public static void logInfo(String format, Object... formatParameters) {
        hammerLogger.info(String.format(format, formatParameters));
    }

    public static void logFine(String format, Object... formatParameters) {
        if (Core.dev_environ) {
            hammerLogger.info(String.format(format, formatParameters));
        }
    }

}
