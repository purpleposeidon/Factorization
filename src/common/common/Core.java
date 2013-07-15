package factorization.common;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.command.ICommandSender;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Icon;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraft.util.StringTranslate;
import net.minecraftforge.common.Configuration;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.Property;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.Mod.PostInit;
import cpw.mods.fml.common.Mod.PreInit;
import cpw.mods.fml.common.Mod.ServerStarting;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.ChargeMetalBlockConductance;
import factorization.api.Coord;
import factorization.client.gui.FactorizationNotify;
import factorization.common.compat.CompatManager;

@Mod(modid = Core.modId, name = Core.name, version = Core.version)
@NetworkMod(
        clientSideRequired = true,
        tinyPacketHandler = NetworkFactorization.class
        )
public class Core {
    public static final String modId = "factorization";
    public static final String name = "Factorization";
    //The comment below is a marker used by the build script.
    public static final String version = "0.8.02dev0"; //@VERSION@
    public Core() {
        registry = new Registry();
        foph = new FactorizationOreProcessingHandler(); //We don't register foph yet.
        MinecraftForge.EVENT_BUS.register(registry);
    }
    
    // runtime storage
    @Instance("factorization")
    public static Core instance;
    public static Registry registry;
    public static FactorizationOreProcessingHandler foph;
    @SidedProxy(clientSide = "factorization.client.FactorizationClientProxy", serverSide = "factorization.common.FactorizationServerProxy")
    public static FactorizationProxy proxy;
    public static NetworkFactorization network;
    public static int factory_rendertype = -1;
    public static boolean finished_loading = false;

    // Configuration
    public static Configuration config;
    static int factory_block_id = 1000;
    static int lightair_id = 1001;
    static int resource_id = 1002;
    public static Pattern routerBan;
    public static boolean render_barrel_item = true;
    public static boolean render_barrel_text = true;
    public static boolean render_barrel_close = false;
    public static boolean notify_in_chat = false;
    public static int entity_relight_task_id = -1;
    public static boolean gen_silver_ore = true;
    public static int silver_ore_node_size = 25;
    public static boolean enable_dimension_slice = false;
    public static int dimension_slice_dimid = -7;
    public static int force_max_entity_radius = -1;
    public static boolean spread_wrathfire = true;
    public static boolean pocket_craft_anywhere = true;
    public static boolean bag_swap_anywhere = true;
    public static String pocketActions = "xcbf";
    public static boolean renderTEs = true;
    public static boolean renderAO = true;
    public static boolean add_branding = false;
    public static boolean cheat = false;
    public static boolean cheat_servo_energy = false;
    public static boolean debug_light_air = false;
    public static boolean debug_network = false;
    public static boolean dimension_slice_allow_smooth = true;
    public static boolean show_fine_logging = false;
    public static boolean serverside_translate = true;
    public static boolean dev_environ = (Boolean) ReflectionHelper.getPrivateValue(cpw.mods.fml.relauncher.CoreModManager.class, null, "deobfuscatedEnvironment");
    public static boolean boilers_suck_water = true;
    public static double steam_output_adjust = 1.0;
    public static boolean enable_sketchy_client_commands = true, enable_cheat_commands = dev_environ;
    public static int tps_reporting_interval = 20;
    public static boolean use_tps_reports = true;
    public static float lowest_dilation = 0.9F;
    public static boolean lagssie_watcher = false;
    public static double lagssie_interval = 0.25;
    public static int max_rocket_base_size = 20*20;
    public static int max_rocket_height = 64;
    public static boolean stretchy_clay = true;
    public static boolean equal_opportunities_for_mobs = true;
    public static boolean invasiveCharge = false;
    public static boolean enable_solar_steam = true;
    public static boolean servos_enabled = version.contains("dev");
    public static String language_file = "/mods/factorization/en_US.lang";
    
    static {
        if (!dev_environ) {
            cheat = false;
            cheat_servo_energy = false;
        }
    }


    private int getBlockConfig(String name, int defaultId, String comment) {
        Property prop = null;
        if (dev_environ) {
            return defaultId;
        } else {
            prop = config.getBlock(name, defaultId);
        }
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
    
    private double getDoubleConfig(String name, String category, double defaultValue, String comment) {
        Property prop = config.get(category, name, defaultValue);
        if (comment != null && comment.length() != 0) {
            prop.comment = comment;
        }
        return prop.getDouble(defaultValue);
    }

    private String getStringConfig(String name, String category, String defaultValue, String comment) {
        Property prop = config.get(category, name, defaultValue);
        if (comment != null && comment.length() != 0) {
            prop.comment = comment;
        }
        return prop.getString();
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
            if (attempt.length() == pocketActions.length()) {
                pocketActions = attempt;
            } else {
                Property p = config.get("pocketCraftingActionKeys", "client", pocketActions);
                p.set(pocketActions);
                p.comment = pocketActions.length() + " keys for: removing (x), cycling (c), balancing (b), filling (f)";
            }
            enable_sketchy_client_commands = getBoolConfig("allowUnpureCommands", "client", enable_sketchy_client_commands, null);
            use_tps_reports = getBoolConfig("useTimeDilation", "client", use_tps_reports, "If this is enabled, the client will run as slowly as the server does. This avoids visual artifacts on laggy servers.");
            lowest_dilation = (float) getDoubleConfig("lowestTimeDilation", "client", lowest_dilation, "Sets a lower bound on time dilation. Between 0 and 1.");
            lowest_dilation = Math.max(1, Math.min(0, lowest_dilation));
            lagssie_watcher = getBoolConfig("enableLagWatchDog", "client", lagssie_watcher, "If true, enables a thread that dumps a stack trace of Minecraft if it is paused for longer than lagWatchDogInterval");
            lagssie_interval = getDoubleConfig("lagWatchDogInterval", "client", lagssie_interval, "If the game is stuck for longer than this amount of time (in seconds), dump a stacktrace of what it is doing.");
        }

        gen_silver_ore = getBoolConfig("generateSilverOre", "general", gen_silver_ore, "This disables silver ore generation");
        int config_silver_size = getIntConfig("silverOreNodeSize", "general", silver_ore_node_size, "The size of silver ore nodes. Between 5 & 35. Default is 25");
        silver_ore_node_size = Math.max(5, Math.min(config_silver_size, 35));
        add_branding = getBoolConfig("addBranding", "general", add_branding, null); //For our Tekkit friends
        language_file = getStringConfig("languageFile", "general", language_file, "Filename on the Java classpath with the localizations");
        
        enable_dimension_slice = dev_environ;
        enable_dimension_slice = getBoolConfig("enableDimensionSlices", "dimensionSlices", enable_dimension_slice, "work in progress; may be unstable");
        spread_wrathfire = getBoolConfig("spreadWrathFire", "server", spread_wrathfire, null);
        String p = getStringConfig("bannedRouterInventoriesRegex", "server", "", "This is a Java Regex to blacklist router access");
        if (p != null && p.length() != 0) {
            try {
                routerBan = Pattern.compile(p);
            } catch (PatternSyntaxException e) {
                e.printStackTrace();
                logWarning("Factorization: config has invalid Java Regex for banned_router_inventories: " + p);
            }
        }
        entity_relight_task_id = config.get("general", "entityRelightTask", -1).getInt();
        if (entity_relight_task_id == -1) {
            entity_relight_task_id = EntityRegistry.findGlobalUniqueEntityId();
            Property prop = config.get("general", "entityRelightTask", entity_relight_task_id);
            prop.set(entity_relight_task_id);
        }
        serverside_translate = getBoolConfig("serversideTranslate", "server", serverside_translate, "If false, notifications will be translated by the client");
        boilers_suck_water = getBoolConfig("boilersSuckWater", "server", boilers_suck_water, "If false, water must be piped in");
        steam_output_adjust = getDoubleConfig("steamOutputAdjustment", "server", steam_output_adjust, "Scale how much steam is produced by the solar boiler");
        stretchy_clay = getBoolConfig("stretchyClay", "server", stretchy_clay, "If true, maximum clay lump volume is 1 m³ instead of (1 m³)/4");
        tps_reporting_interval = getIntConfig("tpsReportInterval", "server", tps_reporting_interval, "How many ticks the server will wait before sending out TPS reports. 20 ticks = 1 second.");
        equal_opportunities_for_mobs = getBoolConfig("equalOpportunitiesForMobs", "server", equal_opportunities_for_mobs, null);
        //invasiveCharge = getBoolConfig("invasiveCharge", "server", invasiveCharge, "Set to true to prevent charge from connecting over metal blocks.");
        //Broken. Doesn't work.
        enable_solar_steam = getBoolConfig("enableSolarSteam", "server", enable_solar_steam, "Set to false to disable the crafting recipe for solar2steam machines");
        config.save();
    }

    @PreInit
    public void load(FMLPreInitializationEvent event) {
        loadConfig(event.getSuggestedConfigurationFile());
        registry.makeBlocks();
        TickRegistry.registerTickHandler(registry, Side.SERVER);
        
        NetworkRegistry.instance().registerGuiHandler(this, proxy);

        registry.registerSimpleTileEntities();
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
        registry.loadLanguages();
        TileEntityWrathFire.setupBurning();
        TileEntitySolarBoiler.setupSteam();
        foph.addDictOres();
        registry.sendIMC();
        (new CompatManager()).loadCompat();
        ChargeMetalBlockConductance.setup();
        finished_loading = true;
    }
    
    @ServerStarting
    public void registerServerCommands(FMLServerStartingEvent event) {
        isMainServerThread.set(true);
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
        FZLogger.log(dev_environ ? Level.INFO : Level.FINE, String.format(format, formatParameters));
    }
    
    public static void logSevere(String format, Object... formatParameters) {
        FZLogger.log(Level.SEVERE, String.format(format, formatParameters));
    }

    public static void addBlockToCreativeList(List tab, Block block) {
        ArrayList a = new ArrayList<Object>();
        block.addCreativeItems(a);
        for (Object o : a) {
            if (o != null) {
                tab.add(o);
            }
        }
    }
    
    static ThreadLocal<Boolean> isMainClientThread = new ThreadLocal<Boolean>() {
        protected Boolean initialValue() { return false; }
    };
    
    static ThreadLocal<Boolean> isMainServerThread = new ThreadLocal<Boolean>() {
        protected Boolean initialValue() { return false; }
    };

    //TODO: Pass World to these? They've got profiler fields.
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
    
    private static void addTranslationHints(String hint_key, List list) {
        StringTranslate st = StringTranslate.getInstance();
        if (st.containsTranslateKey(hint_key) /* func_94520_b = containsTranslateKey */ ) {
            String hint = st.translateKey(hint_key);
            if (hint != null) {
                hint = hint.trim();
                if (hint.length() > 0) {
                    for (String s : hint.split("\\\\n") /* whee */) {
                        list.add(s);
                    }
                }
            }
        }
    }
    
    public static void brand(ItemStack is, List list) {
        String name = is.getItem().getUnlocalizedName(is);
        addTranslationHints(name + ".hint", list);
        if (proxy.isClientHoldingShift()) {
            addTranslationHints(name + ".shift", list);
        }
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
        if (player != null && player.worldObj.isRemote) {
            FactorizationNotify.addMessage(where, format, args);
        } else {
            network.broadcastPacket(player, where, network.notifyPacket(where, format, args));
        }
    }
    
    public static void notify(EntityPlayer player, Coord where, NotifyStyle style, String format, String ...args) {
        if (style == NotifyStyle.FORCE || style == NotifyStyle.FORCELONG) {
            format = "\b" + format;
        }
        if (style == NotifyStyle.LONG || style == NotifyStyle.FORCELONG) {
            format = "\t" + format;
        }
        notify(player, where, format, args);
    }
    
    public static void clearNotifications(EntityPlayer player) {
        if (player != null) {
            //We aren't willing to do global clears
            notify(player, new Coord(player), "!clear");
        }
    }
    
    public static enum NotifyStyle {
        FORCE, LONG, FORCELONG
    }
    
    public static enum TabType {
        REDSTONE(CreativeTabs.tabRedstone), TOOLS(CreativeTabs.tabTools), MISC(CreativeTabs.tabMisc), MATERIALS(CreativeTabs.tabMaterials), ART(CreativeTabs.tabMisc), SERVOS(CreativeTabs.tabTools);
        CreativeTabs type;
        TabType(CreativeTabs type) {
            this.type = type;
        }
    }
    
    public static CreativeTabs tabFactorization = new CreativeTabs("factorizationTab") {
        @Override
        public Item getTabIconItem() {
            if (servos_enabled) {
                return registry.logicMatrixProgrammer;
            }
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
    
    public static String getProperKey(ItemStack is) {
        String n = is.getItem().getUnlocalizedName(is);
        if (n == null || n.length() == 0) {
            n = is.getItem().getUnlocalizedName();
        }
        if (n == null || n.length() == 0) {
            n = is.getItemName();
        }
        if (n == null || n.length() == 0) {
            n = "???";
        }
        return n;
    }
    
    public static String getTranslationKey(ItemStack is) {
        //Get the key for translating is.
        if (is == null) {
            return "<null itemstack; bug?>";
        }
        try {
            String s = is.getDisplayName();
            if (s != null && s.length() > 0) {
                return s;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        String key = getProperKey(is);
        if (canTranslate(key + ".name")) {
            return key + ".name";
        }
        if (canTranslate(key)) {
            return key;
        }
        return key + ".name";
    }
    
    static boolean canTranslate(String str) {
        String ret = StatCollector.translateToLocal(str);
        if (ret == null || ret.length() == 0) {
            return false;
        }
        return !ret.equals(str);
    }
    
    public static String getTranslationKey(Item i) {
        if (i == null) {
            return "<null item; bug?>";
        }
        return i.getUnlocalizedName() + ".name";
    }
    
    public static void sendChatMessage(boolean raw, ICommandSender sender, String msg) {
        sender.sendChatToPlayer(chatmessagecomponent);
    }
    
    public static void sendChatMessage(boolean raw, ICommandSender sender, String format, Object... params) {
        sender.sendChatToPlayer(chatmessagecomponent);
    }
    
    @SideOnly(Side.CLIENT)
    public static Icon texture(IconRegister reg, String name) {
        name = name.replace('.', '/');
        return reg.registerIcon(texture_dir + name);
    }
    
    public final static String texture_dir = "factorization:";
    public final static String model_dir = "/mods/factorization/models/";
    public final static String real_texture_dir = "/mods/factorization/textures/";
    public final static String gui_dir = "/mods/factorization/textures/gui/";
    public final static String texture_file_block = "/terrain.png";
    public final static String texture_file_item = "/gui/items.png";
    
    public static ResourceLocation getResource(String name) {
        return new ResourceLocation("factorization", name);
    }
    
    @SideOnly(Side.CLIENT)
    public static void bindGuiTexture(String name) {
        //bad design; should have a GuiFz. meh.
        TextureManager tex = Minecraft.getMinecraft().renderEngine;
        tex.bindResourceTexture(getResource("gui/" + name + ".png"));
    }
}
