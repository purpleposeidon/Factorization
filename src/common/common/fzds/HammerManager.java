package factorization.common.fzds;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.EnumSet;

import net.minecraft.network.INetworkManager;
import net.minecraft.network.NetLoginHandler;
import net.minecraft.network.packet.NetHandler;
import net.minecraft.network.packet.Packet1Login;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.world.WorldEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.IScheduledTickHandler;
import cpw.mods.fml.common.TickType;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.IConnectionHandler;
import cpw.mods.fml.common.network.Player;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;
import factorization.api.Coord;
import factorization.common.Core;

public class HammerManager implements IConnectionHandler, IScheduledTickHandler {
    public static int dimensionID;
    public static World hammerWorldClient = null; //This is actually a WorldClient
    
    public void setup() {
        if (!Core.enable_dimension_slice) {
            return;
        }
        TickRegistry.registerScheduledTickHandler(Core.hammerManager, Side.SERVER);
        dimensionID = Core.dimension_slice_dimid;
        DimensionManager.registerProviderType(dimensionID, HammerWorldProvider.class, true);
        DimensionManager.registerDimension(dimensionID, dimensionID);
        assert DimensionManager.shouldLoadSpawn(dimensionID);
    }
    
    public void serverStarting(FMLServerStartingEvent event) {
        if (!Core.enable_dimension_slice) {
          return;
        }
        DimensionManager.initDimension(dimensionID);
        assert DimensionManager.shouldLoadSpawn(dimensionID);
    }
    
    public static Coord getCoordForCell(int cellId) {
        return HammerChunkProvider.getCellStart(FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT ? getClientWorld() : getServerWorld(), cellId);
    }
    
    DimensionSliceEntity allocateSlice(World spawnWorld) {
        int cell_id = 0; //takeCellId();
        DimensionSliceEntity dse = new DimensionSliceEntity(spawnWorld, cell_id);
        dse.hammerCell = getCoordForCell(cell_id);
        return dse;
    }
    
    private int allocated_cells = 0;
    private int unsaved_allocations = 0;
    int takeCellId() {
        //save the first time, and every 30 seconds (if there's been a change...)
        if (unsaved_allocations++ == 0) {
            saveCellAllocations();
        }
        return allocated_cells++;
    }
    
    static public World getServerWorld() {
        return DimensionManager.getWorld(dimensionID);
    }
    
    static public World getClientWorld() {
        return hammerWorldClient;
    }
    
    private File getInfoFile() {
        World baseWorld = DimensionManager.getWorld(0);
        File saveDir = new File("saves", baseWorld.getSaveHandler().getSaveDirectoryName());
        saveDir = saveDir.getAbsoluteFile();
        return new File(saveDir, "fzds");
    }
    
    @ForgeSubscribe
    public void handleWorldLoad(WorldEvent.Load event) {
        if (DimensionManager.getWorld(dimensionID) == event.world) {
            loadCellAllocations();
        }
    }
    
    public void loadCellAllocations() {
        File infoFile = getInfoFile();
        if (!infoFile.exists()) {
            Core.logInfo("No FZDS info file");
            return;
        }
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(infoFile);
            DataInputStream ios = new DataInputStream(fis);
            allocated_cells = ios.readInt();
        } catch (Exception e) {
            Core.logWarning("Unable to load FZDS info");
            e.printStackTrace();
        } finally {
            try {
                fis.close();
            } catch (IOException e) {
                e.printStackTrace(); //lern2raii
            }
        }
    }
    
    public void saveCellAllocations() {
        FileOutputStream fos = null;
        try {
            File infoFile = getInfoFile();
            if (!infoFile.exists()) {
                infoFile.createNewFile();
            }
            fos = new FileOutputStream(infoFile);
            
            DataOutputStream dos = new DataOutputStream(fos);
            dos.writeInt(allocated_cells);
            dos.flush();
        } catch (Exception e) {
            Core.logWarning("Unable to save FZDS info");
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace(); //lern2raii
                }
            }
        }
        
    }
    
    
    @Override
    public void connectionClosed(INetworkManager manager) {
        hammerWorldClient = null; //XXX TODO: Stuff needs to happen here!
    }

    /**
     * {@link factorization.client.FactorizationClientProxy.hammerClientLogin}
     */
    @Override
    public void clientLoggedIn(NetHandler clientHandler, INetworkManager manager, Packet1Login login) {
        Core.proxy.hammerClientLogin(clientHandler, manager, login);
    }
    

    @Override
    public void playerLoggedIn(Player player, NetHandler netHandler, INetworkManager manager) {}

    @Override
    public String connectionReceived(NetLoginHandler netHandler, INetworkManager manager) { return null; }

    @Override
    public void connectionOpened(NetHandler netClientHandler, String server, int port, INetworkManager manager) { }

    @Override
    public void connectionOpened(NetHandler netClientHandler, MinecraftServer server, INetworkManager manager) { }
    
    
    EnumSet<TickType> serverTicks = EnumSet.of(TickType.SERVER);
    @Override
    public EnumSet<TickType> ticks() {
        return serverTicks;
    }
    @Override
    public String getLabel() {
        return "FZDS saveinfo";
    }
    
    @Override
    public int nextTickSpacing() {
        return 5*20;
    }
    
    @Override
    public void tickEnd(EnumSet<TickType> type, Object... tickData) {
        if (unsaved_allocations != 0) {
            saveCellAllocations();
            unsaved_allocations = 0;
        }
    }
    @Override
    public void tickStart(EnumSet<TickType> type, Object... tickData) {}
}
