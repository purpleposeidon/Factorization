package factorization.common;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.minecraft.client.Minecraft;
import net.minecraft.src.AchievementList;
import net.minecraft.src.Block;
import net.minecraft.src.CreativeTabs;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.NBTBase;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.SaveHandler;
import net.minecraft.src.StatFileWriter;
import net.minecraft.src.WorldInfo;
import net.minecraftforge.common.Configuration;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.Property;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.WorldAccessContainer;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.Mod.PostInit;
import cpw.mods.fml.common.Mod.PreInit;
import cpw.mods.fml.common.Mod.ServerStarting;
import cpw.mods.fml.common.Side;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.TickRegistry;
import factorization.api.Coord;
import factorization.client.gui.FactorizationNotify;
import factorization.common.astro.FZDSCommand;
import factorization.common.astro.HammerManager;

@Mod(modid = "factorization", name = "Factorization", version = Core.version)
@NetworkMod(
        clientSideRequired = true,
        tinyPacketHandler = NetworkFactorization.class
        )
public class Core {
    //The comment below is a marker used by the build script.
    public static final String version = "0.7.4"; //@VERSION@
    public Core() {
        registry = new Registry();
        exoCore = new ExoCore();
        foph = new FactorizationOreProcessingHandler(); //We don't register foph yet.
        hammerManager = new HammerManager();
        MinecraftForge.EVENT_BUS.register(registry);
        MinecraftForge.EVENT_BUS.register(exoCore);
        MinecraftForge.EVENT_BUS.register(hammerManager);
        NetworkRegistry.instance().registerConnectionHandler(hammerManager);
    }
    
    // runtime storage
    @Instance("factorization")
    public static Core instance;
    public static Registry registry;
    public static ExoCore exoCore;
    public static HammerManager hammerManager;
    public static FactorizationOreProcessingHandler foph;
    @SidedProxy(clientSide = "factorization.client.FactorizationClientProxy", serverSide = "factorization.common.FactorizationServerProxy")
    public static FactorizationProxy proxy;
    public static NetworkFactorization network;
    public static int factory_rendertype = -1;

    // Configuration
    public static Configuration config;
    static int factory_block_id = 254;
    static int lightair_id = 253;
    static int resource_id = 252;
    public static Pattern routerBan;
    public static boolean render_barrel_item = true;
    public static boolean render_barrel_text = true;
    public static boolean render_barrel_close = false;
    public static boolean notify_in_chat = false;
    public static int entity_relight_task_id = -1;
    public static boolean gen_silver_ore = true;
    public static boolean enable_dimension_slice = false;
    public static int dimension_slice_dimid = -7;
    public static boolean spread_wrathfire = true;
    public static boolean pocket_craft_anywhere = true;
    public static boolean bag_swap_anywhere = true;
    public static String pocketActions = "xcb";
    public static boolean renderTEs = true;
    public static boolean renderAO = true;
    public static boolean add_branding = false;
    public static boolean cheat = false;
    public static boolean debug_light_air = false;
    public static boolean debug_network = false;
    public static boolean dimension_slice_allow_smooth = true;
    public static boolean show_fine_logging = false;
    
    public static boolean dev_environ = System.getProperty("user.dir", "").startsWith("/home/poseidon/Development/");

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
            logWarning("Error loading config: %s", e.toString());
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
            render_barrel_close = getBoolConfig("renderBarrelClose", "client", render_barrel_close, "If true, render barrel info only when nearby");
            notify_in_chat = getBoolConfig("notifyInChat", "client", notify_in_chat, "If true, notifications are put in the chat log instead in the world");
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

        gen_silver_ore = getBoolConfig("generateSilverOre", "general", gen_silver_ore, "This disables silver ore generation");
        add_branding = getBoolConfig("addBranding", "general", add_branding, null); //For our Tekkit friends
        
        enable_dimension_slice = dev_environ;
        enable_dimension_slice = getBoolConfig("enableDimensionSlices", "general", enable_dimension_slice, "work in progress; may be unstable");
        dimension_slice_dimid = getIntConfig("sliceDimensionID", "general", dimension_slice_dimid, null);
        spread_wrathfire = getBoolConfig("spreadWrathFire", "server", spread_wrathfire, null);
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
            entity_relight_task_id = EntityRegistry.findGlobalUniqueEntityId();
            Property prop = config.get("general", "entityRelightTask", entity_relight_task_id);
            prop.value = "" + entity_relight_task_id;
        }


        config.save();
    }

    @PreInit
    public void load(FMLPreInitializationEvent event) {
        loadConfig(event.getSuggestedConfigurationFile());
        registry.makeBlocks();
        TickRegistry.registerTickHandler(registry, Side.SERVER);
        
        NetworkRegistry.instance().registerGuiHandler(this, proxy);

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
        proxy.addNameDirect("itemGroup.factorizationTab", "Factorization");
        hammerManager.setup();
    }
    
    @ServerStarting
    public void setMainServerThread(FMLServerStartingEvent event) {
        isMainServerThread.set(true);
        hammerManager.serverStarting(event);
    }

    @PostInit
    public void modsLoaded(FMLPostInitializationEvent event) {
        TileEntityWrathFire.setupBurning();
        foph.addDictOres();
        proxy.fixAchievements();
    }
    
    @ServerStarting
    public void registerServerCommands(FMLServerStartingEvent event) {
        event.registerServerCommand(new NameClayCommand());
        if (enable_dimension_slice) {
            event.registerServerCommand(new FZDSCommand());
        }
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
    
    public static Logger FZLogger = Logger.getLogger("FZ");
    static {
        FZLogger.setParent(FMLLog.getLogger());
        logInfo("This is Factorization " + version);
    }
    
    
    
    public static void logWarning(String format, Object... formatParameters) {
        FZLogger.log(Level.WARNING, String.format(format,  formatParameters));
    }
    
    public static void logInfo(String format, Object... formatParameters) {
        FZLogger.log(Level.INFO, String.format(format, formatParameters));
    }
    
    public static void logFine(String format, Object... formatParameters) {
        FZLogger.log(Level.FINE, String.format(format, formatParameters));
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
    
    static ThreadLocal<Boolean> isMainServerThread = new ThreadLocal<Boolean>() {
        protected Boolean initialValue() { return false; }
    };

    public static void profileStart(String section) {
        // :|
        if (isMainClientThread.get()) {
            proxy.getProfiler().startSection(section);
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
        if (dev_environ) {
            list.add("Development!");
        }
    }
    
    public static void notify(EntityPlayer player, Coord where, String format, String ...args) {
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
    
    public static CreativeTabs tabFactorization = new CreativeTabs("factorizationTab") {
        @Override
        public Item getTabIconItem() {
            return registry.pocket_table;
        }
    };
    
    public static Item tab(Item item, TabType tabType) {
        CreativeTabs tab = tabType.type;
        //item.setCreativeTab(tab);
        item.setCreativeTab(tabFactorization);
        return item;
    }
    
    public static Block tab(Block block, TabType tabType) {
        CreativeTabs tab = tabType.type;
        //block.setCreativeTab(tab);
        block.setCreativeTab(tabFactorization);
        return block;
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
