package factorization.common;

import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.Init;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.Mod.PostInit;
import cpw.mods.fml.common.Mod.PreInit;
import cpw.mods.fml.common.Mod.ServerStarted;
import cpw.mods.fml.common.Mod.ServerStarting;
import cpw.mods.fml.common.Side;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.IGuiHandler;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.NetworkMod.SidedPacketHandler;
import cpw.mods.fml.common.registry.TickRegistry;

import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.src.AchievementList;
import net.minecraft.src.Block;
import net.minecraft.src.CommandHandler;
import net.minecraft.src.CreativeTabs;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ICommand;
import net.minecraft.src.ICommandSender;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.ModLoader;
import net.minecraft.src.NetHandler;
import net.minecraft.src.Packet;
import net.minecraft.src.Profiler;
import net.minecraft.src.ServerCommandManager;
import net.minecraft.src.StatCollector;
import net.minecraft.src.StatFileWriter;
import net.minecraft.src.TileEntityChest;
import net.minecraft.src.World;
import net.minecraft.src.WorldGenMinable;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.Property;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.common.Configuration;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.ForgeSubscribe;
import factorization.api.Coord;
import factorization.client.gui.FactorizationNotify;

@Mod(modid = "factorization", name = "Factorization", version = Core.version)
@NetworkMod(
        clientSideRequired = true,
        packetHandler = NetworkFactorization.class,
        channels = { NetworkFactorization.factorizeTEChannel, NetworkFactorization.factorizeMsgChannel, NetworkFactorization.factorizeCmdChannel, NetworkFactorization.factorizeNtfyChannel })
public class Core {
    //The comment below is a marker used by the build script.
    public static final String version = "0.6.3"; //@VERSION@
    // runtime storage
    @Instance("factorization")
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
    public static String pocketActions = "xcb";
    public static boolean add_branding = false;
    public static boolean cheat = false;
    public static boolean renderTEs = true;
    public static boolean renderAO = true;
    public static boolean spawnDemons = true;
    public static boolean debug_network = false;

    // universal constant config
    public final static String texture_dir = "/factorization/texture/";
    public final static String texture_file_block = texture_dir + "blocks.png";
    public final static String texture_file_item = texture_dir + "items.png";

    private int getBlockConfig(String name, int defaultId, String comment) {
        Property prop = config.getBlock(name, defaultId);
        if (comment != null && comment.length() != 0) {
            prop.comment = comment;
        }
        return prop.getInt(defaultId);
    }

    private int getIntConfig(String name, String category, int defaultValue, String comment) {
        Property prop = config.get(category, name, defaultValue);
        if (comment != null && comment.length() != 0) {
            prop.comment = comment;
        }
        return prop.getInt(defaultValue);
    }

    private boolean getBoolConfig(String name, String category, boolean defaultValue, String comment) {
        Property prop = config.get(category, name, defaultValue);
        if (comment != null && comment.length() != 0) {
            prop.comment = comment;
        }
        return prop.getBoolean(defaultValue);
    }

    private String getStringConfig(String name, String category, String defaultValue, String comment) {
        Property prop = config.get(category, name, defaultValue);
        if (comment != null && comment.length() != 0) {
            prop.comment = comment;
        }
        return prop.value;
    }
    
    private void loadConfig(File configFile) {
        config = new Configuration(configFile);
        try {
            config.load();
        } catch (Exception e) {
            FMLLog.severe("Error loading config: %s", e.toString());
            e.printStackTrace();
        }
        factory_block_id = getBlockConfig("factoryBlockId", factory_block_id, "Factorization Machines.");
        lightair_id = getBlockConfig("lightAirBlockId", lightair_id, "WrathFire and invisible lamp-air made by WrathLamps");
        resource_id = getBlockConfig("resourceBlockId", resource_id, "Ores and metal blocks mostly");

        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            debug_light_air = getBoolConfig("debugLightAir", "client", debug_light_air, "Show invisible lamp-air");
            pocket_craft_anywhere = getBoolConfig("anywherePocketCraft", "client", pocket_craft_anywhere, "Lets you open the pocket crafting table from GUIs");
            bag_swap_anywhere = getBoolConfig("anywhereBagSwap", "client", bag_swap_anywhere, "Lets you use the bag from GUIs");
            render_barrel_item = getBoolConfig("renderBarrelItem", "client", render_barrel_item, null);
            render_barrel_item = getBoolConfig("renderBarrelText", "client", render_barrel_text, null);
            renderTEs = getBoolConfig("renderOtherTileEntities", "client", renderTEs, "If false, most TEs won't draw, making everything look broken but possibly improving FPS");
            renderAO = getBoolConfig("renderAmbientOcclusion", "client", renderAO, "If false, never use smooth lighting for drawing sculptures");
            String attempt = getStringConfig("pocketCraftingActionKeys", "client", pocketActions, "3 keys for: removing (x), cycling (c), balancing (b)");
            if (attempt.length() == 3) {
                pocketActions = attempt;
            } else {
                Property p = config.get("pocketCraftingActionKeys", "client", pocketActions);
                p.value = pocketActions;
                p.comment = "3 keys for: removing (x), cycling (c), balancing (b)";
            }
        }

        gen_silver_ore = getBoolConfig("generateSilverOre", "general", gen_silver_ore, null);
        block_item_id_offset = getIntConfig("blockItemIdOffset", "general", block_item_id_offset, null);
        add_branding = getBoolConfig("addBranding", "general", add_branding, null); //For our Tekkit friends
        
        spread_wrathfire = getBoolConfig("spreadWrathFire", "server", spread_wrathfire, null);
        spawnDemons = getBoolConfig("spawnDemons", "general", spawnDemons, null);
        String p = getStringConfig("bannedRouterInventoriesRegex", "server", "", "This is a Java Regex to blacklist router access");
        if (p != null && p.length() != 0) {
            try {
                routerBan = Pattern.compile(p);
            } catch (PatternSyntaxException e) {
                e.printStackTrace();
                System.err.println("Factorization: config has invalid Java Regex for banned_router_inventories: " + p);
            }
        }
        entity_relight_task_id = config.get("general", "entityRelightTask", -1).getInt();
        if (entity_relight_task_id == -1) {
            entity_relight_task_id = ModLoader.getUniqueEntityId();
            Property prop = config.get("general", "entityRelightTask", entity_relight_task_id);
            prop.value = "" + entity_relight_task_id;
        }


        config.save();
    }

    @PreInit
    public void load(FMLPreInitializationEvent event) {
        loadConfig(event.getSuggestedConfigurationFile());
        registry = new Registry();
        registry.makeBlocks();
        
        NetworkRegistry.instance().registerGuiHandler(this, proxy);
        MinecraftForge.EVENT_BUS.register(registry);
        MinecraftForge.EVENT_BUS.register(this);
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
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            isMainClientThread.set(true);
        }
    }

    @PostInit
    public void modsLoaded(FMLPostInitializationEvent event) {
        TileEntityWrathFire.setupBurning();
        registry.addDictOres();
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            //give the first achievement, because it is stupid and nobody cares.
            //If you're using this mod, you've probably opened your inventory before anyways.
            StatFileWriter sfw = Minecraft.getMinecraft().statFileWriter;
            if (sfw != null && !sfw.hasAchievementUnlocked(AchievementList.openInventory)) {
                sfw.readStat(AchievementList.openInventory, 1);
                logInfo("Achievement Get! You've opened your inventory hundreds of times already! Yes! You're welcome!");
            }
        }
    }
    
    @ServerStarting
    public void registerServerCommands(FMLServerStartingEvent event) {
        event.registerServerCommand(new NameClayCommand());
    }

    ItemStack getExternalItem(String className, String classField, String description) {
        try {
            Class c = Class.forName(className);
            return (ItemStack) c.getField(classField).get(null);
        } catch (Exception err) {
            logWarning("Could not get %s (from %s.%s)", description, className, classField);
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

    static Logger factorizationLog = Logger.getLogger("Factorization");
    static {
        factorizationLog.setParent(Logger.getLogger("ForgeModLoader"));
    }
    
    public static void logWarning(String format, Object... data) {
        factorizationLog.log(Level.WARNING, format, data);
    }
    
    public static void logInfo(String format, Object... data) {
        factorizationLog.log(Level.INFO, format, data);
    }

    public static void addBlockToCreativeList(List tab, Block block) {
        ArrayList a = new ArrayList<Object>();
        block.addCreativeItems(a);
        for (Object o : a) {
            tab.add(o);
        }
    }
    
    static ThreadLocal<Boolean> isMainClientThread = new ThreadLocal<Boolean>() {
        protected Boolean initialValue() { return false; }
    };

    public static void profileStart(String section) {
        // :|
        if (isMainClientThread.get()) {
            Core.proxy.getProfiler().startSection(section);
        }
    }

    public static void profileEnd() {
        if (isMainClientThread.get()) {
            Core.proxy.getProfiler().endSection();
        }
    }
    
    public static void profileStartRender(String section) {
        profileStart("factorization");
        profileStart(section);
    }
    
    public static void profileEndRender() {
        profileEnd();
        profileEnd();
    }

    public static void brand(List list) {
        if (add_branding) {
            list.add("Factorization");
        }
        if (cheat) {
            list.add("Cheat mode!");
        }
    }
    
    public static void notify(EntityPlayer player, Coord where, String format, String ...args) {
        //TODO: Have the client draw the notification somewhere in the world instead of using a chat message! It'll be awesome!
        if (player.worldObj.isRemote) {
            FactorizationNotify.addMessage(where, format, args);
        } else {
            proxy.addPacket(player, network.notifyPacket(where, format, args));
        }
    }
    
    enum TabType {
        REDSTONE(CreativeTabs.tabRedstone), TOOLS(CreativeTabs.tabTools), MISC(CreativeTabs.tabMisc), MATERIALS(CreativeTabs.tabMaterials);
        CreativeTabs type;
        TabType(CreativeTabs type) {
            this.type = type;
        }
    }
    
    public static Item tab(Item item, TabType tabType) {
        CreativeTabs tab = tabType.type;
        item.setCreativeTab(tab);
        return item;
    }
    
    public static String getTranslationKey(ItemStack is) {
        if (is == null) {
            return "<null itemstack; bug?>";
        }
        String n = is.getItem().getItemNameIS(is);
        if (n != null && n.length() != 0) {
            n += ".name";
        }
        if (n == null || n.length() == 0) {
            n = is.getItem().getItemName() + ".name";
        }
        if (n == null || n.length() == 0) {
            n = is.getItemName() + ".name";
        }
        if (n == null || n.length() == 0) {
            n = "???";
        }
        return n;
    }
    
    public static String getTranslationKey(Item i) {
        if (i == null) {
            return "<null item; bug?>";
        }
        return i.getItemName() + ".name";
    }
}
