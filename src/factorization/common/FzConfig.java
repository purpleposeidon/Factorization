package factorization.common;

import java.io.File;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.minecraftforge.common.Configuration;
import net.minecraftforge.common.Property;
import cpw.mods.fml.common.registry.EntityRegistry;
import factorization.shared.Core;

public class FzConfig {

    // I'd totally unstatic all these, but bluh
    public static Configuration config;
    public static int factory_block_id = 1000;
    public static int lightair_id = 1001;
    public static int resource_id = 1002;
    public static int fractured_bedrock_id = 1003;
    public static int dark_iron_ore_id = 1004;
    public static Pattern routerBan;
    public static boolean render_barrel_item = true;
    public static boolean render_barrel_text = true;
    public static boolean render_barrel_close = false;
    public static int entity_relight_task_id = -1;
    public static boolean gen_silver_ore = true;
    public static boolean gen_dark_iron_ore = true;
    public static boolean gen_broken_bedrock = true;
    public static int silver_ore_node_new_size = 18;
    public static boolean enable_dimension_slice = Core.dev_environ;
    public static int dimension_slice_dimid = -7;
    public static int force_max_entity_radius = -1;
    public static boolean spread_wrathfire = true;
    public static boolean pocket_craft_anywhere = true;
    public static boolean bag_swap_anywhere = true;
    public static String pocketActions = "xcbf";
    public static boolean renderTEs = true;
    public static boolean renderAO = true;
    public static boolean add_branding = false;
    public static boolean debug_light_air = false;
    public static boolean dimension_slice_allow_smooth = true;
    public static boolean serverside_translate = true;
    public static boolean boilers_suck_water = true;
    public static double steam_output_adjust = 1.0;
    public static boolean enable_cheat_commands = Core.dev_environ;
    public static boolean enable_sketchy_client_commands = true;
    public static int tps_reporting_interval = 20;
    public static boolean use_tps_reports = true;
    public static float lowest_dilation = 0.6F;
    public static boolean lagssie_watcher = false;
    public static double lagssie_interval = 0.25;
    public static int max_rocket_base_size = 20*20;
    public static int max_rocket_height = 64;
    public static boolean stretchy_clay = true;
    public static boolean equal_opportunities_for_mobs = true;
    public static boolean fix_nether_fog = true;
    public static boolean fix_middle_click = true;
    public static boolean embarken_wood = true;
    public static boolean invasiveCharge = false;
    public static boolean enable_solar_steam = true;
    public static boolean large_servo_instructions = false;
    
    public static boolean enable_retrogen = false;
    public static String retrogen_key = "DEFAULT";
    public static boolean retrogen_silver = false;
    public static boolean retrogen_dark_iron = false;

    

    private int getBlockConfig(String name, int defaultId, String comment) {
        Property prop = null;
        if (Core.dev_environ) {
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
    
    public void loadConfig(File configFile) {
        config = new Configuration(configFile);
        try {
            config.load();
        } catch (Exception e) {
            Core.logWarning("Error loading config: %s", e.toString());
            e.printStackTrace();
        }
        factory_block_id = getBlockConfig("factoryBlockId", factory_block_id, "Factorization TileEntities");
        lightair_id = getBlockConfig("lightAirBlockId", lightair_id, "WrathFire and invisible lamp-air made by WrathLamps");
        resource_id = getBlockConfig("resourceBlockId", resource_id, "Ores and metal blocks mostly");
        dark_iron_ore_id = getBlockConfig("darkIronOre", dark_iron_ore_id, null);
        fractured_bedrock_id = getBlockConfig("fracturedBedrockBlockId", fractured_bedrock_id, null);

        {
            debug_light_air = getBoolConfig("debugLightAir", "client", debug_light_air, "Show invisible lamp-air");
            pocket_craft_anywhere = getBoolConfig("anywherePocketCraft", "client", pocket_craft_anywhere, "Lets you open the pocket crafting table from GUIs");
            bag_swap_anywhere = getBoolConfig("anywhereBagSwap", "client", bag_swap_anywhere, "Lets you use the bag from GUIs");
            render_barrel_item = getBoolConfig("renderBarrelItem", "client", render_barrel_item, null);
            render_barrel_item = getBoolConfig("renderBarrelText", "client", render_barrel_text, null);
            render_barrel_close = getBoolConfig("renderBarrelClose", "client", render_barrel_close, "If true, render barrel info only when nearby");
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
            fix_nether_fog = getBoolConfig("fixNetherFog", "client", fix_nether_fog, "Remove nether fog at short render distances");
            fix_middle_click = getBoolConfig("fixPickBlock", "client", fix_middle_click, "Make middle clicking more useful");
            large_servo_instructions = getBoolConfig("largeServoInstructions", "client", large_servo_instructions, "Render servo instructions extra-large. This can also be toggled on and off using '/f servoInstructionSize'.");
        }

        
        add_branding = getBoolConfig("addBranding", "general", add_branding, null); //For our Tekkit friends
        
        gen_silver_ore = getBoolConfig("generateSilverOre", "general", gen_silver_ore, "Set to false to disable silver ore generation");
        int config_silver_size = getIntConfig("silverOreNodeNewSize", "general", silver_ore_node_new_size, "The size of silver ore nodes. Between 5 & 35. Default is " + silver_ore_node_new_size);
        silver_ore_node_new_size = Math.max(5, Math.min(config_silver_size, 35));
        gen_dark_iron_ore = getBoolConfig("generateDarkIronOre", "general", gen_dark_iron_ore, "Set to false to disable dark iron ore generation");
        gen_broken_bedrock = getBoolConfig("generateBrokenBedrock", "general", gen_broken_bedrock, "Set to false to disable broken bedrock spawning around dark iron ore");
        if (fractured_bedrock_id <= 0) {
            gen_broken_bedrock = false;
        }
        
        {
            enable_retrogen = getBoolConfig("enableRetrogen", "retrogen", enable_retrogen, null);
            retrogen_key = getStringConfig("retrogenKey", "retrogen", retrogen_key, null);
            retrogen_silver = getBoolConfig("retrogenSilver", "retrogen", retrogen_silver, null);
            retrogen_dark_iron = getBoolConfig("retrogenDarkIron", "retrogen", retrogen_dark_iron, null);
        }
        
        enable_dimension_slice = getBoolConfig("enableDimensionSlices", "dimensionSlices", enable_dimension_slice, "work in progress; may be unstable");
        spread_wrathfire = getBoolConfig("spreadWrathFire", "server", spread_wrathfire, null);
        String p = getStringConfig("bannedRouterInventoriesRegex", "server", "", "This is a Java Regex to blacklist router access");
        if (p != null && p.length() != 0) {
            try {
                routerBan = Pattern.compile(p);
            } catch (PatternSyntaxException e) {
                e.printStackTrace();
                Core.logWarning("Factorization: config has invalid Java Regex for banned_router_inventories: " + p);
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
        tps_reporting_interval = getIntConfig("tpsReportInterval", "server", tps_reporting_interval, "How many ticks the server will wait before sending out TPS reports. 20 ticks = 1 second, unless it's lagging.");
        equal_opportunities_for_mobs = getBoolConfig("equalOpportunitiesForMobs", "server", equal_opportunities_for_mobs, "Causes some mobs to rarely spawn wearing your armor");
        embarken_wood = getBoolConfig("barkRecipes", "server", embarken_wood, "Adds recipes for bark variants of logs");
        //invasiveCharge = getBoolConfig("invasiveCharge", "server", invasiveCharge, "Set to true to prevent charge from connecting over metal blocks.");
        //Broken. Doesn't work.
        enable_solar_steam = getBoolConfig("enableSolarSteam", "server", enable_solar_steam, "Set to false to disable the crafting recipe for solar2steam machines");
        config.save();
    }
}
