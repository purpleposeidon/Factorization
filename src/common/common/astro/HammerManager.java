package factorization.common.astro;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import net.minecraft.server.MinecraftServer;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.INetworkManager;
import net.minecraft.src.NetHandler;
import net.minecraft.src.NetLoginHandler;
import net.minecraft.src.Packet1Login;
import net.minecraft.src.World;
import net.minecraft.src.WorldManager;
import net.minecraft.src.WorldServer;
import net.minecraft.src.WorldServerMulti;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.world.WorldEvent;
import cpw.mods.fml.common.IScheduledTickHandler;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.Side;
import cpw.mods.fml.common.TickType;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.IConnectionHandler;
import cpw.mods.fml.common.network.Player;
import cpw.mods.fml.common.registry.TickRegistry;
import factorization.api.Coord;
import factorization.common.Core;

public class HammerManager implements IConnectionHandler, IScheduledTickHandler {
    public static int dimensionID;
    public static World hammerWorldClient = null;
    
    public void setup() {
        if (!Core.enable_dimension_slice) {
            return;
        }
        TickRegistry.registerScheduledTickHandler(Core.hammerManager, Side.SERVER);
        dimensionID = Core.dimension_slice_dimid;
        DimensionManager.registerProviderType(dimensionID, HammerWorldProvider.class, true);
        DimensionManager.registerDimension(dimensionID, dimensionID);
    }
    
    public void serverStarting(FMLServerStartingEvent event) {
    if (!Core.enable_dimension_slice) {
      return;
    }
        DimensionManager.initDimension(dimensionID);
    }
    
    DimensionSliceEntity allocateSlice(Coord spawnCoords) {
        World sliceWorld = DimensionManager.getWorld(HammerManager.dimensionID);
        DimensionSliceEntity dse = new DimensionSliceEntity(spawnCoords.w, takeCellId());
        spawnCoords.setAsEntityLocation(dse);
        spawnCoords.w.spawnEntityInWorld(dse);
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
    
    private File getInfoFile() {
        World hammerWorld = DimensionManager.getWorld(dimensionID);
        File saveDir = new File("saves", hammerWorld.getSaveHandler().getSaveDirectoryName());
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
            dos.write(allocated_cells);
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
        return "FZDS_saveinfo";
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
    
    public class NetworkDimensionStateTicker implements ITickHandler {
        ArrayList<PacketProxyingNetworkPlayer> proxiedPlayers = new ArrayList();
        @Override
        public void tickStart(EnumSet<TickType> type, Object... tickData) {
            World world = (World) tickData[0];
            if (!(world.getChunkProvider() instanceof HammerChunkProvider)) {
                //not the best check, but should be efficient.
                return;
            }
            proxiedPlayers.clear();
            for (EntityPlayer ep : (List<EntityPlayer>) world.playerEntities) {
                if (ep instanceof PacketProxyingNetworkPlayer) {
                    PacketProxyingNetworkPlayer ppnp = (PacketProxyingNetworkPlayer) ep; 
                    proxiedPlayers.add(ppnp);
                    ppnp.enterTick();
                }
            }
        }

        @Override
        public void tickEnd(EnumSet<TickType> type, Object... tickData) {
            World world = (World) tickData[0];
            if (!(world.getChunkProvider() instanceof HammerChunkProvider)) {
                //not the best check, but should be efficient.
                return;
            }
            for (int i = 0; i < proxiedPlayers.size(); i++) {
                PacketProxyingNetworkPlayer ppnp = proxiedPlayers.get(i);
                ppnp.leaveTick();
            }
        }

        EnumSet<TickType> worldTicks = EnumSet.of(TickType.WORLD);
        
        @Override
        public EnumSet<TickType> ticks() {
            return worldTicks;
        }

        @Override
        public String getLabel() {
            return "FZDS network";
        }
        
    }
}
