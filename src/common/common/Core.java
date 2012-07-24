package factorization.common;

import java.io.File;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ItemStack;
import net.minecraft.src.ModLoader;
import net.minecraft.src.NetHandler;
import net.minecraft.src.Packet;
import net.minecraft.src.TileEntityChest;
import net.minecraft.src.World;
import net.minecraft.src.WorldGenMinable;
import net.minecraft.src.forge.Configuration;
import net.minecraft.src.forge.IGuiHandler;
import net.minecraft.src.forge.MinecraftForge;
import net.minecraft.src.forge.NetworkMod;
import net.minecraft.src.forge.Property;
import factorization.api.Coord;

public abstract class Core extends NetworkMod implements IGuiHandler {
    /** Wrapper for client */
    public abstract void addName(Object what, String string);

    /** Wrapper for client */
    public abstract String translateItemStack(ItemStack here);

    /** Return false if we're an SMP client */
    public abstract boolean isCannonical(World world);

    public abstract boolean isServer();

    /** Send a chat message to the client to translate */
    public abstract void broadcastTranslate(EntityPlayer who, String... msg);

    /** Get a configuration instance */
    public abstract Configuration getConfig();

    /** Get the save directory for a world */
    public abstract File getWorldSaveDir(World world);

    /** If on SMP, send packet to tell player what he's holding */
    public void updateHeldItem(EntityPlayer player) {
    }

    public abstract void pokeChest(TileEntityChest chest);

    public EntityPlayer getClientPlayer() {
        return null;
    }

    public void randomDisplayTickFor(World w, int x, int y, int z, Random rand) {
    }

    public void pokePocketCrafting() {
    }

    public void playSoundFX(String src, float volume, float pitch) {
    }

    public void updatePlayerInventory(EntityPlayer player) {
    }

    public abstract void make_recipes_side();

    protected abstract EntityPlayer getPlayer(NetHandler handler);

    protected abstract void addPacket(EntityPlayer player, Packet packet);

    public boolean playerListensToCoord(EntityPlayer player, Coord c) {
        return true;
    }

    public abstract boolean isPlayerAdmin(EntityPlayer player);

    // configificable
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

    // universal config
    public static String texture_dir = "/factorization/texture/";
    public static String texture_file_block = texture_dir + "blocks.png";
    public static String texture_file_item = texture_dir + "items.png";

    // runtime storage
    public static Core instance;
    public static Registry registry;
    public static NetworkFactorization network;
    final static Charset utf8 = Charset.forName("UTF-8");
    public static int factory_rendertype;
    WorldGenMinable silverGen;

    Configuration config;

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

    void loadConfig() {
        config = getConfig();
        config.load();
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
    }

    @Override
    public String getVersion() {
        return "0.3.2";
    }

    @Override
    public String getName() {
        return "Factorization";
    }

    boolean is_loaded = false;

    @Override
    public void load() {
        if (is_loaded) {
            return;
        }
        is_loaded = true;
        loadConfig();
        instance = this;
        registry = new Registry();
        network = new NetworkFactorization();
        registry.makeBlocks();
        registry.registerSimpleTileEntities();
        make_recipes_side();
        registry.makeRecipes();
        registry.setToolEffectiveness();

        silverGen = new WorldGenMinable(resource_id, 35);

        MinecraftForge.registerConnectionHandler(network);
        MinecraftForge.setGuiHandler(this, this);
        MinecraftForge.registerCraftingHandler(registry);
        MinecraftForge.registerPickupHandler(registry);
        MinecraftForge.registerOreHandler(registry);
        MinecraftForge.registerChunkLoadHandler(TileEntityWatchDemon.loadHandler);
        MinecraftForge.registerSaveHandler(TileEntityWatchDemon.loadHandler);
        ModLoader.setInGameHook(this, true, true);
        config.save();
    }

    @Override
    public void modsLoaded() {
        TileEntityWrathFire.setupBurning();
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

    @Override
    public boolean clientSideRequired() {
        return true;
    }

    @Override
    public boolean serverSideRequired() {
        return false;
    }

    @Override
    public void generateSurface(World w, Random rand, int cx, int cz) {
        if (!gen_silver_ore) {
            return;
        }
        //only gen in 1/4 of the chunks or something
        if (rand.nextFloat() > 0.35) {
            return;
        }
        if (Math.abs(cx + cz) % 3 == 1) {
            return;
        }
        int x = cx + rand.nextInt(16);
        int z = cz + rand.nextInt(16);
        int y = 5 + rand.nextInt(48);
        silverGen.generate(w, rand, x, y, z);
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

}
