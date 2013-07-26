package factorization.fzds;

import java.io.File;
import java.lang.annotation.Annotation;
import java.util.HashMap;

import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ConfigCategory;
import net.minecraftforge.common.Configuration;
import net.minecraftforge.common.Property;
import cpw.mods.fml.common.Mod;
import factorization.api.Coord;
import factorization.api.DeltaCoord;
import factorization.common.Core;

public class HammerInfo {
    static File worldConfigFile = null;
    static Configuration channelConfig;
    static Configuration worldState;
    
    private int unsaved_allocations = 0;
    private boolean channel_config_dirty = false;
    boolean world_loaded = false;
    HashMap<Integer, ConfigCategory> channel2category = new HashMap<Integer, ConfigCategory>();
    
    private static final int defaultPadding = 16*8;
    
    void setConfigFile(File f) {
        channelConfig = new Configuration(f);
    }
    
    void loadGlobalConfig() {
        if (worldState != null) {
            return;
        }
        WorldServer world = (WorldServer) DeltaChunk.getServerShadowWorld();
        world_loaded = true;
        File saveDir = world.getChunkSaveLocation();
        saveDir = saveDir.getAbsoluteFile();
        worldConfigFile = new File(saveDir, "hammer.state");
        worldState = new Configuration(worldConfigFile);
        saveChannelConfig();
    }
    
    private static final String channelsCategory = "channels";
    
    public int makeChannelFor(Object modInstance, String channelId, int default_channel, int padding, String comment) {
        if (padding < 0) {
            padding = defaultPadding;
        }
        if (channelConfig == null) {
            throw new IllegalArgumentException("Tried to register channel too early");
        }
        Core.logFine("Allocating Hammer channel for %s: %s", modInstance, comment);
        
        
        Class<? extends Object> c = modInstance.getClass();
        Annotation a = c.getAnnotation(Mod.class);
        if (a == null) {
            throw new IllegalArgumentException("modInstance is not a mod");
        }
        Mod info = (Mod) c.getAnnotation(Mod.class);
        String modCategory = (info.modid() + "." + channelId).toLowerCase();
        
        int max = default_channel;
        boolean collision = false;
        
        for (String categoryName : channelConfig.getCategoryNames()) {
            ConfigCategory cat = channelConfig.getCategory(categoryName);
            if (cat.equals(modCategory)) {
                continue;
            }
            if (!cat.containsKey("channel")) {
                continue;
            }
            int here_chan = channelConfig.get(categoryName, "channel", -1).getInt();
            max = Math.max(max, here_chan);
            if (here_chan == default_channel) {
                collision = true;
            }
        }
        if (collision) {
            int newDefault = max + 1;
            Core.logFine("Default channel ID for %s (%s) was already taken, using %s", modCategory, default_channel, newDefault);
            default_channel = newDefault;
        }
        
        channelConfig.addCustomCategoryComment(modCategory, comment);
        int channelRet = channelConfig.get(modCategory, "channel", default_channel).getInt();
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
        Property prop = cat.get("padding");
        int ret = prop.getInt(defaultPadding);
        return ret;
    }
    
    Coord takeCell(int channel, DeltaCoord size) {
        loadGlobalConfig();
        Property chanAllocs = worldState.get("allocations", "channel" + channel, 0);
        int start = chanAllocs.getInt(0);
        int add = size.x + getPaddingForChannel(channel);
        chanAllocs.set(Integer.toString(start + add));
        Coord ret = new Coord(DeltaChunk.getServerShadowWorld(), start, 16, channel*Hammer.channelWidth);
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
        channelConfig.save();
    }
    
}
