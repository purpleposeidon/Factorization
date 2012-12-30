package factorization.fzds;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.NetLoginHandler;
import net.minecraft.network.packet.NetHandler;
import net.minecraft.network.packet.Packet1Login;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.IScheduledTickHandler;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.PreInit;
import cpw.mods.fml.common.Mod.ServerStarting;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.TickType;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.IConnectionHandler;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.Player;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;
import factorization.api.Coord;
import factorization.common.Core;

@Mod(modid = Hammer.modId, name = Hammer.name, version = Core.version, dependencies = "required-after: " + Core.modId)
@NetworkMod(clientSideRequired = true)
public class Hammer {
    static final String lore = "Anvil's Hammer";
    
    
    public static final String modId = Core.modId + ".dimensionalSlice";
    public static final String name = "FZDS";
    public static Hammer instance; //@Instance seems to give the parent?
    @SidedProxy(clientSide = "factorization.fzds.HammerClientProxy", serverSide = "factorization.fzds.HammerProxy")
    public static HammerProxy proxy;
    public static boolean enabled;
    public static int dimensionID;
    public static World worldClient = null; //This is actually a WorldClient
    public static double DSE_ChunkUpdateRangeSquared = Math.pow(16*8, 2); //This is actually set when the server starts
    //Two important things:
    //1) Use a synchronized block while iterating
    //2) This will be shared with the client & integrated server.
    static Set<DimensionSliceEntity> slices = Collections.synchronizedSet(new HashSet());
    
    public Hammer() {
        Hammer.instance = this;
    }
    
    public static World getClientShadowWorld() {
        return worldClient;
    }
    
    public static World getServerShadowWorld() {
        return DimensionManager.getWorld(dimensionID);
    }
    
    public static World getClientRealWorld() {
        return proxy.getClientRealWorld();
    }
    
    public static World getWorld() {
        return FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT ? getClientShadowWorld() : getServerShadowWorld();
    }
    
    public static Coord getCellLookout(int cellId) {
        int cellSize = (cellWidth + wallWidth)*16;
        return new Coord(getWorld(), cellSize*cellId - 2, 0 /*wallHeight*/, (1 + cellWidth)*16/2);
    }
    
    public static Coord getCellCorner(int cellId) {
        //return new Coord(getWorld(), 0, 0, 0);
        //return getCellLookout(cellId);
        int cellSize = (cellWidth + wallWidth)*16;
        return new Coord(getWorld(), cellSize*cellId, 0, 0);
    }
    
    public static Coord getCellCenter(int cellId) {
        return getCellCorner(cellId).add(cellWidth*16/2, wallHeight/2, cellWidth*16/2);
    }
    
    public static Coord getCellOppositeCorner(int cellId) {
        return getCellCorner(cellId).add(cellWidth*16, wallHeight, cellWidth*16);
    }
    
    public static int getIdFromCoord(Coord c) {
        if (c.x < 0 || c.z < 0 || c.z > cellWidth*16) {
            return -1;
        }
        int depth_per_cell = (cellWidth + wallWidth)*16;
        return c.x / depth_per_cell;
    }
    
    public static Chunk[] getChunks(int cellId) {
        Coord corner = getCellCorner(cellId);
        int i = 0;
        for (int dx = 0; dx < cellWidth; dx++) {
            for (int dz = 0; dz < cellWidth; dz++) {
                hChunks[i++] = corner.w.getChunkFromBlockCoords(corner.x + 16*dx, corner.z + 16*dz);
            }
        }
        return hChunks;
    }
    
    public static DimensionSliceEntity allocateSlice(World spawnWorld) {
        return spawnSlice(spawnWorld, hammerInfo.takeCellId());
    }
    
    public static DimensionSliceEntity spawnSlice(World spawnWorld, int cellId) {
        return new DimensionSliceEntity(spawnWorld, cellId);
    }
    
    private final static EnumSet<TickType> serverTicks = EnumSet.of(TickType.SERVER);
    private final static HammerInfo hammerInfo = new HammerInfo();
    //each cell is a few chunks wide, with chunks of bedrock between.
    static final int cellWidth = 4;
    static final int wallWidth = 16;
    static final int wallHeight = 16*8; //16*8 is the minimum or something... I'd rather go with 16*4. Meh.
    
    private static Chunk[] hChunks = new Chunk[cellWidth*cellWidth];
    
    @PreInit
    public void setup(FMLPreInitializationEvent event) {
        event.getModMetadata().parent = Core.modId;
        enabled = Core.enable_dimension_slice;
        if (!enabled) {
            return;
        }
        
        //Create the hammer dimension
        dimensionID = Core.dimension_slice_dimid;
        DimensionManager.registerProviderType(dimensionID, HammerWorldProvider.class, true);
        DimensionManager.registerDimension(dimensionID, dimensionID);
        
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
                public void tickStart(EnumSet<TickType> type, Object... tickData) { }
                
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
    }
    
    @ServerStarting
    public void setMainServerThread(FMLServerStartingEvent event) {
        if (!enabled) {
            return;
        }
        event.registerServerCommand(new FZDSCommand());
        DimensionManager.initDimension(dimensionID);
        assert DimensionManager.shouldLoadSpawn(dimensionID);
        int view_distance = MinecraftServer.getServer().getConfigurationManager().getViewDistance();
        //the undeobfed method comes after "isPlayerWatchingChunk", also in uses of ServerConfigurationManager.getViewDistance()
        //It returns how many blocks are visible.
        DSE_ChunkUpdateRangeSquared = Math.pow(PlayerManager.func_72686_a(view_distance) + 16*cellWidth, 2);
    }
    
    public static DimensionSliceEntity findClosest(Entity target, int cellId) {
        if (target == null) {
            return null;
        }
        DimensionSliceEntity closest = null;
        double dist = Double.POSITIVE_INFINITY;
        World real_world = getClientRealWorld();
        
        synchronized (Hammer.slices) {
            for (DimensionSliceEntity here : Hammer.slices) {
                if (here.worldObj != real_world && here.cell != cellId) {
                    continue;
                }
                if (closest == null) {
                    closest = here;
                    continue;
                }
                double here_dist = target.getDistanceSqToEntity(here);
                if (here_dist < dist) {
                    dist = here_dist;
                    closest = here;
                }
            }
        }
        return closest;
    }
    
    public static Vec3 shadow2nearestReal(double x, double y, double z) {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        int correct_cell_id = Hammer.getIdFromCoord(Coord.of(x, y, z));
        DimensionSliceEntity closest = Hammer.findClosest(player, correct_cell_id);
        if (closest == null) {
            return null;
        }
        return closest.shadow2real(Vec3.createVectorHelper(x, y, z));
    }
    
    public static Vec3 ent2vec(Entity ent) {
        return Vec3.createVectorHelper(ent.posX, ent.posY, ent.posZ);
    }
}
