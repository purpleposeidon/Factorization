package factorization.common;

import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.Init;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.Mod.PostInit;
import cpw.mods.fml.common.Mod.PreInit;
import cpw.mods.fml.common.Side;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.IGuiHandler;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.NetworkMod.SidedPacketHandler;
import cpw.mods.fml.common.registry.TickRegistry;

import net.minecraft.src.Block;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ItemStack;
import net.minecraft.src.ModLoader;
import net.minecraft.src.NetHandler;
import net.minecraft.src.Packet;
import net.minecraft.src.Profiler;
import net.minecraft.src.TileEntityChest;
import net.minecraft.src.World;
import net.minecraft.src.WorldGenMinable;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.Property;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.common.Configuration;
import factorization.api.Coord;

@Mod(modid = "factorization", name = "Factorization", version = Core.version)
@NetworkMod(
        clientSideRequired = true,
        packetHandler = NetworkFactorization.class,
        channels = {NetworkFactorization.factorizeTEChannel, NetworkFactorization.factorizeMsgChannel, NetworkFactorization.factorizeCmdChannel},
        clientPacketHandlerSpec = @SidedPacketHandler(
                packetHandler = NetworkFactorization.class,
                channels = {NetworkFactorization.factorizeTEChannel, NetworkFactorization.factorizeMsgChannel, NetworkFactorization.factorizeCmdChannel}
        ),
        serverPacketHandlerSpec = @SidedPacketHandler(
                packetHandler = NetworkFactorization.class,
                channels = {NetworkFactorization.factorizeTEChannel, NetworkFactorization.factorizeMsgChannel, NetworkFactorization.factorizeCmdChannel}
        )
)
public class Core {
    public static final String version = "0.5.0";
    // runtime storage
    @Instance
    public static Core instance;
    public static Registry registry;
    @SidedProxy(clientSide = "factorization.client.FactorizationClientProxy", serverSide = "factorization.common.FactorizationServerProxy")
    public static FactorizationProxy proxy;
    public static NetworkFactorization network;
    public static int factory_rendertype;

    // Configuration
    public static Configuration config;
    public static int factory_block_id = 254;
    public static int lightair_id = 253;
    public static int resource_id = 252;
    public static Pattern routerBan;
    public static int block_item_id_offset = -256;
    public static boolean render_barrel_item = true;
    public static boolean render_barrel_text = true;
    public static boolean debug_light_air = false;
    public static int watch_demon_chunk_range = 3;
    public static int entity_relight_task_id = -1;
    public static boolean gen_silver_ore = true;
    public static boolean spread_wrathfire = true;
    public static boolean pocket_craft_anywhere = true;
    public static boolean bag_swap_anywhere = true;
    
    // universal constant config
    public final static String texture_dir = "/factorization/texture/";
    public final static String texture_file_block = texture_dir + "blocks.png";
    public final static String texture_file_item = texture_dir + "items.png";


    private int getBlockConfig(String name, int defaultId, String comment) {
        Property prop = config.getOrCreateBlockIdProperty(name, defaultId);
        if (comment != null && comment.length() != 0) {
            prop.comment = comment;
        }
        return prop.getInt(defaultId);
    }

    private int getIntConfig(String category, String name, int defaultValue, String comment) {
        Property prop = config.getOrCreateIntProperty(category, name, defaultValue);
        if (comment != null && comment.length() != 0) {
            prop.comment = comment;
        }
        return prop.getInt(defaultValue);
    }

    private boolean getBoolConfig(String category, String name, boolean defaultValue, String comment) {
        Property prop = config.getOrCreateBooleanProperty(category, name, defaultValue);
        if (comment != null && comment.length() != 0) {
            prop.comment = comment;
        }
        return prop.getBoolean(defaultValue);
    }

    private String getStringConfig(String category, String name, String defaultValue, String comment) {
        Property prop = config.getOrCreateProperty(category, name, defaultValue);
        if (comment != null && comment.length() != 0) {
            prop.comment = comment;
        }
        return prop.value;
    }


    @PreInit
    public void loadConfig(FMLPreInitializationEvent event) {
        config = new Configuration(event.getSuggestedConfigurationFile());
        try {
            config.load();
        } catch (Exception e) {
            FMLLog.severe("Error loading config: %s", e.toString());
            e.printStackTrace();
        }
        factory_block_id = getBlockConfig("factoryBlockId", factory_block_id, "Factorization Machines.");
        lightair_id = getBlockConfig("lightAirBlockId", lightair_id, "WrathFire and invisible lamp-air made by WrathLamps");
        resource_id = getBlockConfig("resourceBlockId", resource_id, "Ores and metal blocks mostly");

        debug_light_air = getBoolConfig("debugLightAir", "general", debug_light_air, "Render invisible lamp-air");
        gen_silver_ore = getBoolConfig("generateSilverOre", "general", gen_silver_ore, null);
        pocket_craft_anywhere = getBoolConfig("anywherePocketCraft", "general", pocket_craft_anywhere, "Lets you open the pocket crafting table from GUIs");
        bag_swap_anywhere = getBoolConfig("anywhereBagSwap", "general", bag_swap_anywhere, "Lets you use the bag from GUIs");

        block_item_id_offset = getIntConfig("blockItemIdOffset", "misc", block_item_id_offset, "Hopefully you'll never need to change these.");
        render_barrel_item = getBoolConfig("renderBarrelItem", "misc", render_barrel_item, null);
        render_barrel_item = getBoolConfig("renderBarrelText", "misc", render_barrel_text, null);

        watch_demon_chunk_range = getIntConfig("watchDemonChunkRange", "smpAdmin", watch_demon_chunk_range, "chunk radius to keep loaded");
        spread_wrathfire = getBoolConfig("spreadWrathFire", "smpAdmin", spread_wrathfire, null);
        String p = getStringConfig("bannedRouterInventoriesRegex", "smpAdmin", "", null);
        if (p != null && p.length() != 0) {
            try {
                routerBan = Pattern.compile(p);
            } catch (PatternSyntaxException e) {
                e.printStackTrace();
                System.err.println("Factorization: config has invalid Java Regex for banned_router_inventories: " + p);
            }
        }
        entity_relight_task_id = config.getOrCreateIntProperty("entityRelightTask", "general", -1).getInt();
        if (entity_relight_task_id == -1) {
            entity_relight_task_id = ModLoader.getUniqueEntityId();
            Property prop = config.getOrCreateIntProperty("entityRelightTask", "general", entity_relight_task_id);
            prop.value = "" + entity_relight_task_id;
            prop.comment = "This is a Java Regex to blacklist access to TE";
        }
        config.save();
        
        registry = new Registry();
        registry.makeBlocks();
    }
    
    @Init
    public void load(FMLInitializationEvent event) {
        
        NetworkRegistry.instance().registerGuiHandler(this, proxy);
        MinecraftForge.EVENT_BUS.register(registry);
        //MinecraftForge.EVENT_BUS.register(TileEntityWatchDemon.loadHandler);
        TickRegistry.registerTickHandler(registry, Side.CLIENT);
        TickRegistry.registerTickHandler(registry, Side.SERVER);
        
        registry.registerSimpleTileEntities();
        proxy.makeItemsSide();
        registry.makeItems();
        config.save();
        registry.makeOther();
        registry.makeRecipes();
        registry.setToolEffectiveness();
        proxy.registerKeys();
        proxy.registerRenderers();

        config.save();
    }

    @PostInit
    public void modsLoaded(FMLPostInitializationEvent event) {
        TileEntityWrathFire.setupBurning();
        registry.addDictOres();
    }

    ItemStack getExternalItem(String className, String classField, String description) {
        try {
            Class c = Class.forName(className);
            return (ItemStack) c.getField(classField).get(null);
        } catch (Exception err) {
            System.out.println("Could not get " + description);
        }
        return null;

    }

    enum KeyState {
        KEYOFF, KEYSTART, KEYON;

        boolean isPressed() {
            return this != KEYOFF;
        }
    }

    HashMap<EntityPlayer, KeyState[]> keyStateMap = new HashMap();

    void putPlayerKey(EntityPlayer player, int key, boolean state) {
        KeyState pmap[] = keyStateMap.get(player);
        if (pmap == null) {
            pmap = new KeyState[Registry.MechaKeyCount];
            keyStateMap.put(player, pmap);
        }
        if (state) {
            pmap[key] = KeyState.KEYSTART;
        }
        else {
            pmap[key] = KeyState.KEYOFF;
        }
    }

    static int ExtraKey_minimum = 0;

    public enum ExtraKey {
        SNEAK(-1, "sneaking"), INAIR(-2, "in the air"), RUN(-3, "running");

        int id;
        public String text;

        ExtraKey(int id, String text) {
            this.id = id;
            this.text = text;
            ExtraKey_minimum = Math.min(ExtraKey_minimum, id);
        }

        public static ExtraKey fromInt(int i) {
            for (ExtraKey ek : values()) {
                if (ek.id == i) {
                    return ek;
                }
            }
            return INAIR;
        }

        boolean isActive(EntityPlayer player) {
            switch (this) {
            case SNEAK:
                return player.isSneaking();
            case INAIR:
                return player.isAirBorne;
            case RUN:
                return player.isSprinting();
            }
            return false;
        }
    }

    KeyState getPlayerKeyState(EntityPlayer player, int key) {
        if (player == null) {
            return KeyState.KEYOFF;
        }
        if (key < 0) {
            return ExtraKey.fromInt(key).isActive(player) ? KeyState.KEYON : KeyState.KEYOFF;
        }
        KeyState arr[] = keyStateMap.get(player);
        if (arr == null) {
            putPlayerKey(player, 0, false);
            return KeyState.KEYOFF;
        }
        KeyState ret = arr[key];
        if (ret == null) {
            return KeyState.KEYOFF;
        }
        return ret;
    }

    boolean hasPlayerKey(EntityPlayer player, int key) {
        return getPlayerKeyState(player, key).isPressed();
    }

    public void updatePlayerKeys() {
        for (KeyState states[] : keyStateMap.values()) {
            for (int i = 0; i < states.length; i++) {
                if (states[i] == KeyState.KEYSTART) {
                    states[i] = KeyState.KEYON;
                }
                if (states[i] == null) {
                    states[i] = KeyState.KEYOFF;
                }
            }
        }
    }

    
//	public static boolean isCannonical() {
//		if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
//			return false;
//		}
//		return true;
//	}
    
//	public static boolean isServer() {
//		return FMLCommonHandler.instance().getSide() != Side.CLIENT;
//	}
    
    public static void logWarning(String format, Object... data) {
        FMLLog.warning("Factorization: " + format, data);
    }
    
    public static void addBlockToCreativeList(List tab, Block block) {
        ArrayList a = new ArrayList<Object>();
        block.addCreativeItems(a);
        for (Object o : a) {
            tab.add(o);
        }
    }
}
