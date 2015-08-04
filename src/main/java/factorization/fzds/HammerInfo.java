package factorization.fzds;

import java.io.File;
import java.lang.annotation.Annotation;
import java.util.HashMap;

import factorization.shared.NORELEASE;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;
import factorization.api.Coord;
import factorization.api.DeltaCoord;
import factorization.shared.Core;

public class HammerInfo {
    static File worldConfigFile = null;
    static Configuration channelConfig;
    static Configuration worldState;
    static int dimension_slice_dimid = -7;
    
    private int unsaved_allocations = 0;
    private boolean channel_config_dirty = false;
    boolean world_loaded = false;
    HashMap<Integer, ConfigCategory> channel2category = new HashMap<Integer, ConfigCategory>();
    
    private static final int defaultPadding = 16 * 4;
    
    void setConfigFile(File f) {
        channelConfig = new Configuration(f);
        final String rem = "The dimension used for FZDS/Hammer/Colossi/rotated/moving blocks, etc.\n" +
                "If things go really south with those features, as a last resort you can try deleting this dimension.\n" +
                "But first see if you can use the /fzds to fix it.";
        dimension_slice_dimid = channelConfig.getInt("FzdsDimension", "Hammer", dimension_slice_dimid, Integer.MIN_VALUE, Integer.MAX_VALUE, rem);
        saveChannelConfig();
    }
    
    void loadGlobalConfig() {
        if (worldState != null) {
            return;
        }
        DeltaChunk.assertEnabled();
        WorldServer world = (WorldServer) DeltaChunk.getServerShadowWorld();
        world_loaded = true;
        File saveDir = world.getChunkSaveLocation();
        saveDir = saveDir.getAbsoluteFile();
        worldConfigFile = new File(saveDir, "hammer.state");
        worldState = new Configuration(worldConfigFile);
        saveChannelConfig();
    }

    boolean isFree(String name, int val) {
        for (String categoryName : channelConfig.getCategoryNames()) {
            ConfigCategory cat = channelConfig.getCategory(categoryName);
            if (cat.getQualifiedName().equals(name)) {
                if (channelConfig.get(categoryName, "channel", val).getInt() == val) {
                    return true;
                }
                // Uhm.
                continue;
            }
            if (!cat.containsKey("channel")) {
                continue;
            }
            int here_chan = channelConfig.get(categoryName, "channel", -1).getInt();
            if (here_chan == val) {
                return false;
            }
        }
        return true;
    }
    
    public int makeChannelFor(String modName, String channelName, int default_channel_id, int padding, String comment) {
        DeltaChunk.assertEnabled();
        NORELEASE.fixme("Uh, totally broken ID registration");
        if (padding < 0) {
            padding = defaultPadding;
        }
        if (channelConfig == null) {
            throw new IllegalArgumentException("Tried to register channel too early");
        }
        Core.logFine("Allocating Hammer channel for %s: %s", modName, channelName);
        
        String modCategory = ("hammerChannels." + modName + "." + channelName).toLowerCase();

        boolean collision = false;
        int channel_id = default_channel_id;
        while (!isFree(modCategory, channel_id)) {
            channel_id++;
            collision = true;
        }
        if (collision) {
            Core.logFine("Default channel ID for %s (%s) was already taken, using %s", modCategory, default_channel_id, channel_id);
            default_channel_id = channel_id;
        }
        
        channelConfig.addCustomCategoryComment(modCategory, comment);
        int channelRet = channelConfig.get(modCategory, "channel", default_channel_id).getInt();
        if (!isFree(modCategory, channelRet)) {
            int fixedChannel = channelRet;
            do {
                fixedChannel++;
            } while (!isFree(modCategory, fixedChannel));
            Core.logWarning("Saved channel ID for %s (%s) conflicted, changing to %s", modCategory, channelRet, fixedChannel);
            channelRet = fixedChannel;
            channelConfig.get(modCategory, "channel", channelRet).set(channelRet);
        }
        padding = channelConfig.get(modCategory, "padding", padding).getInt();
        
        if (world_loaded) {
            saveChannelConfig();
        } else {
            channel_config_dirty = true;
        }
        channel2category.put(channelRet, channelConfig.getCategory(modCategory));
        return channelRet;
    }
    
    public int getPaddingForChannel(int channel) {
        ConfigCategory cat = channel2category.get(channel);
        if (cat == null) {
            return defaultPadding;
        }
        Property prop = cat.get("padding");
        return prop.getInt(defaultPadding);
    }
    
    int roundToChunk(int n) {
        n++;
        if (n % 16 == 0) return n;
        return ((n/16) + 1) * 16;
    }
    
    Coord takeCell(int channel, DeltaCoord size) {
        loadGlobalConfig();
        Property chanAllocs = worldState.get("allocations", "channel" + channel, 0);
        int start = roundToChunk(chanAllocs.getInt(0));
        int add = size.x + getPaddingForChannel(channel);
        chanAllocs.set(Integer.toString(start + add));
        Coord ret = new Coord(DeltaChunk.getServerShadowWorld(), start, 16, roundToChunk(channel*Hammer.channelWidth));
        dirtyCellAllocations();
        return ret;
    }
    
    public void setAllocationCount(int channel, int count) {
        loadGlobalConfig();
        ConfigCategory cat = channel2category.get(channel);
        cat.get("allocated").set(count);
        saveCellAllocations();
    }
    
    File getWorldSaveFile() {
        World hammerWorld = DeltaChunk.getServerShadowWorld();
        File base = new File(hammerWorld.getSaveHandler().getWorldDirectoryName());
        return new File(base, "deltaChunk.cfg");
    }
    
    public void dirtyCellAllocations() {
        if (unsaved_allocations == 0) {
            saveCellAllocations();
        }
        unsaved_allocations++;
    }
    
    
    private int ticks = 0;
    @EventHandler
    public void tickCellSaving(ServerTickEvent event) {
        if (event.phase != Phase.END) return;
        if (ticks++ < 5*20) return;
        ticks = 0;
        saveCellAllocations();
    }
    
    public void saveCellAllocations() {
        if (channel_config_dirty) {
            channelConfig.save();
            channel_config_dirty = false;
        }
        if (worldState == null) {
            return;
        }
        worldState.save();
        unsaved_allocations = 0;
    }
    
    public void saveChannelConfig() {
        if (channelConfig.hasChanged()) {
            channelConfig.save();
        }
    }
    
}
