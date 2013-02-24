package factorization.fzds;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.world.WorldEvent;
import factorization.api.Coord;
import factorization.api.DeltaCoord;
import factorization.common.Core;

public class HammerInfo {
    private int allocated_cells = 0;
    private int unsaved_allocations = 0;
    
    //TODO NORELEASE: Let's use a config file in the save directory!
    
    @ForgeSubscribe
    public void handleWorldLoad(WorldEvent.Load event) {
        if (DimensionManager.getWorld(Hammer.dimensionID) == event.world) {
            loadCellAllocations();
        }
    }
    
    public int makeChannelFor(Object modInstance, String description, int padding) {
        Core.logFine("Allocating Hammer channel for %s %s", modInstance, description); //NORELEASE
        return 0;
    }
    
    public int getPaddingForChannel(int channel) {
        if (channel != 0) {
            throw new IllegalArgumentException("Non-zero channels not yet implemented");
        }
        return 16*8;
    }
    
    Coord takeCell(int channel, DeltaCoord size) {
        if (channel != 0) {
            throw new IllegalArgumentException("Non-zero channels not yet implemented");
        }
        int x = allocated_cells;
        int add = size.x + getPaddingForChannel(channel);
        Coord ret = new Coord(Hammer.getServerShadowWorld(), x, 64, channel*Hammer.channelWidth);
        allocated_cells += add;
        if (unsaved_allocations++ == 0) {
            saveCellAllocations();
        }
        return ret;
    }
    
    void setAllocationCount(int channel, int count) {
        if (channel != 0) {
            throw new IllegalArgumentException("Non-zero channels not yet implemented");
        }
        allocated_cells = count;
        saveCellAllocations();
    }
    
    private File getInfoFile() {
        WorldServer baseWorld = DimensionManager.getWorld(0);
        File saveDir = baseWorld.getChunkSaveLocation();
        saveDir = saveDir.getAbsoluteFile();
        return new File(saveDir, "fzds");
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
            unsaved_allocations = 0;
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
            unsaved_allocations = 0;
        } catch (Exception e) {
            Core.logWarning("Unable to save FZDS info (cell allocation count = " + allocated_cells + ". Might need to restore this with /fzds force_cell_allocation_count)");
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
    
}
