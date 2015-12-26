package factorization.common;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.EntityRegistry;
import factorization.fzds.DeltaChunk;
import factorization.shared.Core;
import factorization.shared.Graylist;
import net.minecraft.block.Block;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import java.io.File;
import java.util.ArrayList;

public class FzConfig {

    // I'd totally unstatic all these, but bluh
    public static Configuration config;
    public static boolean render_barrel_item = true;
    public static boolean render_barrel_text = true;
    public static boolean render_barrel_use_displaylists = true;
    public static boolean render_barrel_force_entity_render = false;
    public static boolean render_barrel_force_no_intercept = false;
    public static boolean render_barrel_close = false;
    public static int entity_relight_task_id = -1;
    public static boolean gen_silver_ore = true;
    public static boolean gen_dark_iron_ore = true;
    public static int silver_ore_node_new_size = 18;
    public static boolean gen_colossi = true;
    public static int colossus_spacing = 48;
    public static boolean pocket_craft_anywhere = true;
    public static String pocketActions = "xcbf";
    public static boolean renderTEs = true;
    public static boolean renderAO = true;
    public static boolean add_branding = false;
    public static boolean debug_light_air = false;
    public static boolean dimension_slice_allow_smooth = true;
    public static boolean boilers_suck_water = true;
    public static double steam_output_adjust = 1.0;
    public static boolean enable_cheat_commands = Core.dev_environ;
    public static boolean enable_sketchy_client_commands = true;
    public static int tps_reporting_interval = 20;
    public static boolean use_tps_reports = true;
    public static float lowest_dilation = 0.6F;
    public static boolean lagssie_watcher = false;
    public static boolean limit_integrated_server = false;
    public static double lagssie_interval = 0.25;
    public static int max_rocket_base_size = 20*20;
    public static int max_rocket_height = 64;
    public static boolean stretchy_clay = true;
    public static boolean equal_opportunities_for_mobs = true;
    public static boolean fix_middle_click = false;
    public static boolean embarken_wood = true;
    public static boolean mushroomalize = true;
    public static boolean proper_projectile_physics = true;
    public static boolean buffed_nametags = true;
    public static boolean enable_solar_steam = true;
    public static boolean fanturpeller_works_on_players = true;
    public static boolean large_servo_instructions = false;
    public static boolean players_discover_colossus_guides = true;
    public static boolean disable_endermen_griefing = false;
    public static boolean blockundo = true;
    public static boolean blockundo_grab = true;
    public static boolean debug_fzds_collisions = false;
    public static boolean enable_rocketry = /* Hey! If you're turning this on, remove stuff from factorization_dead_items */ Core.dev_environ;
    public static boolean sockets_ignore_front_redstone = true;
    public static boolean show_time_on_fullscreen = true;
    public static boolean require_book_for_manual = true;
    public static boolean infinite_guide_usage = false;
    public static boolean mirror_sunbeams = true;
    public static boolean ic2_kinetic_compat = true;
    public static Graylist<Block> lacerator_block_graylist;
    public static int legendarium_queue_size = 7;
    public static int legendarium_delay_hours = 24 * 5;
    public static String f = "f";
    
    public static boolean enable_retrogen = false;
    public static String retrogen_key = "DEFAULT";
    public static boolean retrogen_silver = false;
    public static boolean retrogen_dark_iron = false;
    public static boolean sort_renderers = !Loader.isModLoaded("optifine"); // Just as a guess.
    public static boolean enableRecipeReflection = true;

    public ArrayList<Property> editable_main = new ArrayList<Property>();
    public ArrayList<Property> editable_runtime = new ArrayList<Property>();

    private boolean can_edit_main = false, can_edit_runtime = false;

    FzConfig editMain() {
        can_edit_main = true;
        return this;
    }

    FzConfig editRun() {
        can_edit_runtime = true;
        can_edit_main = true;
        return this;
    }

    Property putProp(Property prop, String comment) {
        if (can_edit_main) {
            editable_main.add(prop);
            can_edit_main = false;
        }
        if (can_edit_runtime) {
            editable_runtime.add(prop);
            can_edit_runtime = false;
        }
        if (comment != null && !comment.isEmpty()) {
            prop.comment = comment;
        }
        return prop;
    }
    
    private int getIntConfig(String name, String category, int defaultValue, String comment) {
        Property prop = putProp(config.get(category, name, defaultValue), comment);
        return putProp(prop, comment).getInt(defaultValue);
    }

    private boolean getBoolConfig(String name, String category, boolean defaultValue, String comment) {
        Property prop = putProp(config.get(category, name, defaultValue), comment);
        return putProp(prop, comment).getBoolean(defaultValue);
    }
    
    private double getDoubleConfig(String name, String category, double defaultValue, String comment) {
        Property prop = putProp(config.get(category, name, defaultValue), comment);
        return putProp(prop, comment).getDouble(defaultValue);
    }

    private String getStringConfig(String name, String category, String defaultValue, String comment) {
        Property prop = putProp(config.get(category, name, defaultValue), comment);
        return putProp(prop, comment).getString();
    }


    public static boolean loaded = false;
    public void load() {
        if (loaded) return;
        loaded = true;
        loadConfig(null);
    }

    public void reload() {
        loaded = false;
        load();
    }
    
    public void loadConfig(File configFile) {
        config = new Configuration(configFile);
        try {
            config.load();
        } catch (Exception e) {
            Core.logWarning("Error loading config: %s", e.toString());
            e.printStackTrace();
        }

        readConfigSettings();
        saveConfigSettings();
    }

    public void saveConfigSettings() {
        if (config.hasChanged()) {
            config.save();
        }
    }

    public void readConfigSettings() {
        {
            debug_light_air = getBoolConfig("debugLightAir", "client", debug_light_air, "Show invisible lamp-air");
            pocket_craft_anywhere = editRun().getBoolConfig("anywherePocketCraft", "client", pocket_craft_anywhere, "Lets you open the pocket crafting table from GUIs");
            render_barrel_item = editMain().getBoolConfig("renderBarrelItem", "client", render_barrel_item, null);
            render_barrel_text = editMain().getBoolConfig("renderBarrelText", "client", render_barrel_text, null);
            render_barrel_close = editMain().getBoolConfig("renderBarrelClose", "client", render_barrel_close, "If true, render barrel info only when nearby");
            render_barrel_use_displaylists = editMain().getBoolConfig("renderBarrelUseDisplayLists", "client", render_barrel_use_displaylists, "If true, use OpenGL display lists for rendering barrels. Setting to false may fix some render issues, at the cost of making barrels render less efficiently");
            render_barrel_force_no_intercept = editMain().getBoolConfig("renderBarrelForceNoIntercept", "client", render_barrel_force_no_intercept, "If true, don't use hacks to avoid enchantment effect rendering issues. Setting to false may fix some render issues, at the cost of making the enchantment effect from, eg, enchanted books visible through walls.");
            render_barrel_force_entity_render = editMain().getBoolConfig("renderBarrelForceItemFrameStyle", "client", render_barrel_force_entity_render, "If true, render barrels the same way as item frames. Setting to false may fix some render issues, at the cost of looking lame.");
            renderTEs = editMain().getBoolConfig("renderOtherTileEntities", "client", renderTEs, "If false, most TEs won't draw, making everything look broken but possibly improving FPS");
            renderAO = editMain().getBoolConfig("renderAmbientOcclusion", "client", renderAO, "If false, never use smooth lighting for drawing sculptures");
            String attempt = getStringConfig("pocketCraftingActionKeys", "client", pocketActions, "4 keys for: removing (x), cycling (c), balancing (b), filling (f)");
            if (attempt.length() == pocketActions.length()) {
                pocketActions = attempt;
            } else {
                Property p = config.get("pocketCraftingActionKeys", "client", pocketActions);
                p.set(pocketActions);
                p.comment = pocketActions.length() + " keys for: removing (x), cycling (c), balancing (b), filling (f)";
            }
            enable_sketchy_client_commands = editRun().getBoolConfig("allowUnpureCommands", "client", enable_sketchy_client_commands, null);
            use_tps_reports = editRun().getBoolConfig("useTimeDilation", "client", use_tps_reports, "If this is enabled, the client will run as slowly as the server does. This avoids visual artifacts on laggy servers.");
            lowest_dilation = (float) editRun().getDoubleConfig("lowestTimeDilation", "client", lowest_dilation, "Sets a lower bound on time dilation. Between 0 and 1.");
            lowest_dilation = Math.max(1, Math.min(0, lowest_dilation));
            lagssie_watcher = getBoolConfig("enableLagWatchDog", "client", lagssie_watcher, "If true, enables a thread that dumps a stack trace of Minecraft if it is paused for longer than lagWatchDogInterval");
            lagssie_interval = getDoubleConfig("lagWatchDogInterval", "client", lagssie_interval, "If the game is stuck for longer than this amount of time (in seconds), dump a stacktrace of what it is doing.");
            limit_integrated_server = getBoolConfig("limitIntegratedServer", "client", limit_integrated_server, /*"Prevent the integrated server from ticking faster than the client. Probably won't cause a deadlocks."*/ "(Broken; don't use this. Attempts to limit integrated server tick speed to match the client's, but can cause deadlocks.)");
            //fix_middle_click = getBoolConfig("fixPickBlock", "client", fix_middle_click, "Make middle clicking more useful");
            large_servo_instructions = editMain().getBoolConfig("largeServoInstructions", "client", large_servo_instructions, "Render servo instructions extra-large. This can also be toggled on and off using '/f servoInstructionSize'.");
            show_time_on_fullscreen = editRun().getBoolConfig("showTimeOnFullscreen", "client", show_time_on_fullscreen, "If true, show the time every half hour");
            mirror_sunbeams = editRun().getBoolConfig("drawMirrorSunbeams", "client", mirror_sunbeams, "If false, mirrors won't draw sunbeams");
            sort_renderers = getBoolConfig("sortRenderers", "client", sort_renderers, "Use advanced Entity & TileEntity sorting techniques to optimize rendering, particularly for FZ entities.");
            enableRecipeReflection = getBoolConfig("enableRecipeReflection", "client", enableRecipeReflection, "If true, java reflection will be used as an (ugly) fallback for getting recipe information from unknown recipes");
        }


        add_branding = getBoolConfig("addBranding", "general", add_branding, null);

        gen_silver_ore = getBoolConfig("generateSilverOre", "general", gen_silver_ore, "Set to false to disable silver ore generation");
        int config_silver_size = getIntConfig("silverOreNodeNewSize", "general", silver_ore_node_new_size, "The size of silver ore nodes. Between 5 & 35. Default is " + silver_ore_node_new_size);
        silver_ore_node_new_size = Math.max(5, Math.min(config_silver_size, 35));
        gen_dark_iron_ore = getBoolConfig("generateDarkIronOre", "general", gen_dark_iron_ore, "Set to false to disable dark iron ore generation");
        gen_colossi = getBoolConfig("generateColossi", "general", gen_colossi, "If true, Colossi will generate in the world. If false, the player will be given an LMP instead of a lost map.");
        colossus_spacing = getIntConfig("colossusSpacing", "general", colossus_spacing, "Distance between colossi in chunks");

        {
            enable_retrogen = getBoolConfig("enableRetrogen", "retrogen", enable_retrogen, null);
            retrogen_key = getStringConfig("retrogenKey", "retrogen", retrogen_key, null);
            retrogen_silver = getBoolConfig("retrogenSilver", "retrogen", retrogen_silver, null);
            retrogen_dark_iron = getBoolConfig("retrogenDarkIron", "retrogen", retrogen_dark_iron, null);
        }

        entity_relight_task_id = config.get("general", "entityRelightTask", -1).getInt();
        if (entity_relight_task_id == -1) {
            entity_relight_task_id = EntityRegistry.findGlobalUniqueEntityId();
            Property prop = config.get("general", "entityRelightTask", entity_relight_task_id);
            prop.set(entity_relight_task_id);
        }
        boilers_suck_water = getBoolConfig("boilersSuckWater", "server", boilers_suck_water, "If false, water must be piped in");
        disable_endermen_griefing = getBoolConfig("disableEndermenGriefing", "server", disable_endermen_griefing, "If set to true, then endermen will not pick up blocks.");
        steam_output_adjust = getDoubleConfig("steamOutputAdjustment", "server", steam_output_adjust, "Scale how much steam is produced by the solar boiler");
        stretchy_clay = getBoolConfig("stretchyClay", "server", stretchy_clay, "If true, maximum clay lump volume is 1 m³ instead of (1 m³)/4");
        tps_reporting_interval = getIntConfig("tpsReportInterval", "server", tps_reporting_interval, "How many ticks the server will wait before sending out TPS reports. 20 ticks = 1 second, unless it's lagging.");
        equal_opportunities_for_mobs = getBoolConfig("equalOpportunitiesForMobs", "server", equal_opportunities_for_mobs, "Causes some mobs to rarely spawn wearing your armor");
        embarken_wood = getBoolConfig("barkRecipes", "server", embarken_wood, "Adds recipes for bark variants of logs");
        mushroomalize = getBoolConfig("mushroomNormalize", "server", mushroomalize, "Textures giant mushroom blocks when placed");
        blockundo = getBoolConfig("blockUndo", "server", blockundo, "If true, then recently placed blocks break easily");
        blockundo_grab = getBoolConfig("blockUndoGrab", "server", blockundo_grab, "If true, then blocks broken with blockundo will be instantly picked up");
        config.getCategory("server").remove("hotBlocks");
        config.getCategory("server").remove("hotBlocksGrab");
        proper_projectile_physics = getBoolConfig("properProjectilePhysics", "server", proper_projectile_physics, "Makes projectiles start with the velocity of the thrower");
        buffed_nametags = getBoolConfig("buffedNametags", "server", buffed_nametags, "Naming entities gives them +5 hearts");
        players_discover_colossus_guides = getBoolConfig("playersDiscoverColossusGuides", "server", players_discover_colossus_guides, "If set to true, players will find a lost map after getting the diamonds achievement. If colossi are disabled, they'll get an LMP instead.");
        infinite_guide_usage = getBoolConfig("infiniteGuideUse", "server", infinite_guide_usage, "If set to true, the Lost Map won't take damage");
        //invasiveCharge = getBoolConfig("invasiveCharge", "server", invasiveCharge, "Set to true to prevent charge from connecting over metal blocks.");
        //Broken. Doesn't work.
        enable_solar_steam = getBoolConfig("enableSolarSteam", "server", enable_solar_steam, "Set to false to disable the crafting recipe for solar2steam machines");
        fanturpeller_works_on_players = getBoolConfig("fanturpellerWorksOnPlayers", "server", fanturpeller_works_on_players, "If set to false, fanturpellers will not move players.");
        sockets_ignore_front_redstone = getBoolConfig("socketsIgnoreFacePower", "server", sockets_ignore_front_redstone, "Set to false to let socket blocks detect redstone from their front; provided for legacy worlds.");
        require_book_for_manual = getBoolConfig("requireBookForManual", "general", require_book_for_manual, "If set to true, then you must have a manual in your inventory to look up items");
        lacerator_block_graylist = Graylist.ofBlocks(getStringConfig("laceratorBlockGraylist", "server", "-minecraft:bedrock,minecraft:end_portal", "Comma-separated list of block names. In front of the list must be either a +, for white-listing, or a -, for black-listing."));
        f = getStringConfig("miscClientCommand", "client", f, "Use this to change the /f command to avoid conflict with the Factions bukkit plugin");
        ic2_kinetic_compat = getBoolConfig("ic2KineticCompat", "server", ic2_kinetic_compat, "Compatability with IC2's IKineticSource");
        getStringConfig("README", "fzds", "See hammerChannels.cfg for FZDS-related configuration", "");
        legendarium_delay_hours = getIntConfig("legendariumDelayHours", "server", legendarium_delay_hours, "How many hours must pass before the legendarium can be used again");
        legendarium_queue_size = getIntConfig("legendariumQueueSize", "server", legendarium_queue_size, "The legendarium must have this many items before an artifact can be reforged.");

        if (!DeltaChunk.enabled()) {
            gen_colossi = false;
            enable_rocketry = false;
        }
    }
}
