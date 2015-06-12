package factorization.shared;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cpw.mods.fml.common.ModContainer;
import factorization.citizen.EntityCitizen;
import factorization.fzds.DeltaChunk;
import factorization.mechanics.MechanismsFeature;
import factorization.util.DataUtil;
import factorization.util.FzUtil;
import factorization.weird.poster.EntityPoster;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.command.ICommandSender;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.MinecraftForge;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLInterModComms;
import cpw.mods.fml.common.event.FMLInterModComms.IMCMessage;
import cpw.mods.fml.common.event.FMLMissingMappingsEvent;
import cpw.mods.fml.common.event.FMLMissingMappingsEvent.Action;
import cpw.mods.fml.common.event.FMLMissingMappingsEvent.MissingMapping;
import cpw.mods.fml.common.event.FMLModIdMappingEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.GameRegistry.Type;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.charge.TileEntitySolarBoiler;
import factorization.colossi.BuildColossusCommand;
import factorization.colossi.ColossusController;
import factorization.colossi.ColossusFeature;
import factorization.common.FactorizationProxy;
import factorization.common.FactoryType;
import factorization.common.FzConfig;
import factorization.common.Registry;
import factorization.compat.CompatManager;
import factorization.coremod.AtVerifier;
import factorization.coremod.LoadingPlugin;
import factorization.darkiron.BlockDarkIronOre;
import factorization.docs.DistributeDocs;
import factorization.docs.DocumentationModule;
import factorization.docs.RecipeViewer;
import factorization.fzds.Hammer;
import factorization.oreprocessing.FactorizationOreProcessingHandler;
import factorization.servo.ServoMotor;
import factorization.wrath.TileEntityWrathLamp;

@Mod(
        modid = Core.modId,
        name = Core.name,
        version = Core.version,
        acceptedMinecraftVersions = "1.7.10",
        dependencies = "required-after: " + Hammer.modId,
        guiFactory = "factorization.common.FzConfigGuiFactory"
)
public class Core {
    //We should repackage stuff. And rename the API package possibly.
    public static final String modId = "factorization";
    public static final String name = "Factorization";
    //The comment below is a marker used by the build script.
    public static final String version = "@FZVERSION@";

    public Core() {
        instance = this;
        fzconfig = new FzConfig();
        registry = new Registry();
        foph = new FactorizationOreProcessingHandler();
        network = new NetworkFactorization();
        netevent = new FzNetEventHandler();
    }
    
    // runtime storage
    public static Core instance;
    public static FzConfig fzconfig;
    public static Registry registry;
    public static FactorizationOreProcessingHandler foph;
    @SidedProxy(clientSide = "factorization.common.FactorizationClientProxy", serverSide = "factorization.common.FactorizationProxy")
    //TODO: Server proxy should extend clientproxy for sanity.
    //(Have to be sure that the server overrides everything tho!) 
    public static FactorizationProxy proxy;
    public static NetworkFactorization network;
    public static FzNetEventHandler netevent;
    public static int factory_rendertype = -1;
    public static boolean finished_loading = false;

    public static final boolean dev_environ = Launch.blackboard != null ? (Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment") : false;
    public static final boolean cheat = dev_only(false);
    public static final boolean cheat_servo_energy = dev_only(false);
    public static final boolean debug_network = false;
    public static final boolean show_fine_logging = false;
    public static final boolean enable_test_content = dev_environ || Boolean.parseBoolean(System.getProperty("fz.enableTestContent"));

    private static boolean dev_only(boolean a) {
        if (!dev_environ) return false;
        return a;
    }

    static public boolean serverStarted = false;

    public static void checkJar() {
        // Apparently some people somehow manage to get "Factorization.jar.zip", which somehow breaks the coremod.
        ModContainer mod = FMLCommonHandler.instance().findContainerFor(modId);
        if (mod == null) {
            Core.logSevere("I don't have a mod container? Wat?");
            return;
        }
        final File src = mod.getSource();
        if (src == null || src.isDirectory()) return;
        final String path = src.getPath();
        if (!isBadName(path)) return;
        String correctName = path.replaceAll("\\.zip$", ".jar");
        if (isBadName(correctName)) return; // Carefully ensure we don't make a loop

        Core.logSevere("The factorization jar is improperly named! Renaming " + path + "  to " + correctName);
        boolean success = src.renameTo(new File(correctName));
        if (success) {
            throw new RuntimeException("The Factorization jar had an improper file extension. It has been renamed. Please restart Minecraft.");
        } else {
            throw new RuntimeException("The Factorization jar has an improper file extension; it should be a .jar, not a .zip, and not a .jar.zip.");
        }
    }

    private static boolean isBadName(String path) {
        if (path == null) return false;
        return path.endsWith(".zip");
    }

    void checkForge() { }

    @EventHandler
    public void load(FMLPreInitializationEvent event) {
        checkJar();
        initializeLogging(event.getModLog());
        checkForge();
        Core.loadBus(registry);
        fzconfig.loadConfig(event.getSuggestedConfigurationFile());
        registry.makeBlocks();
        
        NetworkRegistry.INSTANCE.registerGuiHandler(this, proxy);

        registerSimpleTileEntities();
        registry.makeItems();
        FzConfig.config.save();
        registry.makeRecipes();
        registry.setToolEffectiveness();
        proxy.registerRenderers();
        
        if (FzConfig.players_discover_colossus_guides) {
            DistributeDocs dd = new DistributeDocs();
            MinecraftForge.EVENT_BUS.register(dd);
            FMLCommonHandler.instance().bus().register(dd);
        }

        MechanismsFeature.initialize();
        
        FzConfig.config.save();
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            isMainClientThread.set(true);
        }
        
        FMLInterModComms.sendMessage(Core.modId, "AddRecipeCategory", "Lacerator|factorization.oreprocessing.TileEntityGrinder|recipes");
        FMLInterModComms.sendMessage(Core.modId, "AddRecipeCategory", "Crystallizer|factorization.oreprocessing.TileEntityCrystallizer|recipes");
        FMLInterModComms.sendMessage(Core.modId, "AddRecipeCategory", "Slag Furnace|factorization.oreprocessing.TileEntitySlagFurnace$SlagRecipes|smeltingResults");
    }
    
    void registerSimpleTileEntities() {
        FactoryType.registerTileEntities();
        GameRegistry.registerTileEntity(TileEntityFzNull.class, "fz.null");
        GameRegistry.registerTileEntity(BlockDarkIronOre.Glint.class, "fz.glint");
        //TileEntity renderers are registered in the client proxy
        
        EntityRegistry.registerModEntity(TileEntityWrathLamp.RelightTask.class, "factory_relight_task", 0, Core.instance, 1, 10, false);
        EntityRegistry.registerModEntity(ServoMotor.class, "factory_servo", 1, Core.instance, 100, 1, true);
        EntityRegistry.registerModEntity(ColossusController.class, "fz_colossal_controller", 2, Core.instance, 256, 20, false);
        EntityRegistry.registerModEntity(EntityPoster.class, "fz_entity_poster", 3, Core.instance, 160, Integer.MAX_VALUE, false);
        EntityRegistry.registerModEntity(EntityCitizen.class, "fz_entity_citizen", 4, Core.instance, 100, 1, true);
    }
    
    @EventHandler
    public void handleInteractions(FMLInitializationEvent event) {
        registry.sendIMC();
        ColossusFeature.init();
        PatreonRewards.init();
    }
    
    @EventHandler
    public void handleIMC(FMLInterModComms.IMCEvent event) {
        for (IMCMessage message : event.getMessages()) {
            try {
                RecipeViewer.handleImc(message);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    @EventHandler
    public void modsLoaded(FMLPostInitializationEvent event) {
        TileEntitySolarBoiler.setupSteam();
        foph.addDictOres();

        registry.addOtherRecipes();
        (new CompatManager()).loadCompat();
        for (FactoryType ft : FactoryType.values()) ft.getRepresentative(); // Make sure everyone's registered to the EVENT_BUS
        proxy.afterLoad();
        finished_loading = true;
        Blocks.diamond_block.setHardness(5.0F).setResistance(10.0F);
        validateEnvironment();
    }
    
    @EventHandler
    public void registerServerCommands(FMLServerStartingEvent event) {
        isMainServerThread.set(true);
        serverStarted = true;
        DocumentationModule.instance.serverStarts(event);
        if (DeltaChunk.enabled()) {
            event.registerServerCommand(new BuildColossusCommand());
        }
    }
    
    @EventHandler
    public void mappingsChanged(FMLModIdMappingEvent event) {
        for (FactoryType ft : FactoryType.values()) {
            TileEntityCommon tec = ft.getRepresentative();
            if (tec != null) tec.mappingsChanged(event);
        }
    }
    
    private Set<String> getDeadItems() {
        InputStream is = null;
        final String dead_list = "/factorization_dead_items";
        try {
            HashSet<String> found = new HashSet();
            URL url = getClass().getResource(dead_list);
            is = url.openStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            while (true) {
                String line = br.readLine();
                if (line == null) {
                    break;
                }
                found.add(line);
            }
            return found;
        } catch (IOException e) {
            Core.logSevere("Failed to load " + dead_list);
            e.printStackTrace();
            return new HashSet();
        } finally {
            FzUtil.closeNoisily("closing " + dead_list, is);
        }
    }
    
    @EventHandler
    public void abandonDeadItems(FMLMissingMappingsEvent event) {
        Set<String> theDead = getDeadItems();
        for (MissingMapping missed : event.get()) {
            if (missed.name.startsWith("factorization:")) {
                if (theDead.contains(missed.name)) {
                    missed.ignore();
                } else if (missed.getAction() != Action.IGNORE) {
                    Core.logSevere("Missing mapping: " + missed.name);
                }
            }
        }
    }
    
    @EventHandler
    public void handleFzPrefixStrip(FMLMissingMappingsEvent event) {
        Map<String, Item> fixups = Registry.nameCleanup;
        for (MissingMapping missed : event.get()) {
            if (missed.type != GameRegistry.Type.ITEM) continue;
            Item target = fixups.get(missed.name);
            if (target != null) {
                missed.remap(target);
            }
        }
    }
    
    @EventHandler
    public void replaceDerpyNames(FMLMissingMappingsEvent event) {
        Object[][] corrections = new Object[][] {
                {"factorization:tile.null", Core.registry.factory_block},
                {"factorization:FZ factory", Core.registry.factory_block},
                {"factorization:tile.factorization.ResourceBlock", Core.registry.resource_block},
                {"factorization:FZ resource", Core.registry.resource_block},
                {"factorization:tile.lightair", Core.registry.lightair_block},
                {"factorization:FZ Lightair", Core.registry.lightair_block},
                {"factorization:tile.factorization:darkIronOre", Core.registry.dark_iron_ore},
                {"factorization:FZ dark iron ore", Core.registry.dark_iron_ore},
                {"factorization:tile.bedrock", Core.registry.fractured_bedrock_block},
                {"factorization:FZ fractured bedrock", Core.registry.fractured_bedrock_block},
                {"factorization:tile.factorization:darkIronOre", Core.registry.dark_iron_ore},
                {"factorization:FZ fractured bedrock", Core.registry.fractured_bedrock_block},
        };
        HashMap<String, Block> corr = new HashMap<String, Block>();
        for (Object[] pair : corrections) {
            corr.put((String) pair[0], (Block) pair[1]);
        }
        for (MissingMapping missed : event.get()) {
            Block value = corr.get(missed.name);
            if (value == null) {
                continue;
            }
            if (missed.type == Type.BLOCK) {
                missed.remap(value);
            } else if (missed.type == Type.ITEM) {
                Item it = DataUtil.getItem(value);
                if (it != null) {
                    missed.remap(it);
                }
            }
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
    
    private static Logger FZLogger = LogManager.getLogger("FZ-init");
    private void initializeLogging(Logger logger) {
        Core.FZLogger = logger;
        logInfo("This is Factorization %s", version);
    }
    
    public static void logSevere(String format, Object... formatParameters) {
        FZLogger.error(String.format(format, formatParameters));
    }
    
    public static void logWarning(String format, Object... formatParameters) {
        FZLogger.warn(String.format(format, formatParameters));
    }
    
    public static void logInfo(String format, Object... formatParameters) {
        FZLogger.info(String.format(format, formatParameters));
    }
    
    public static void logFine(String format, Object... formatParameters) {
        if (dev_environ) {
            FZLogger.info(String.format(format, formatParameters));
        }
    }
    
    static final ThreadLocal<Boolean> isMainClientThread = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() { return false; }
    };
    
    static final ThreadLocal<Boolean> isMainServerThread = new ThreadLocal<Boolean>() {
        @Override
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
    
    private static void addTranslationHints(String hint_key, List list, String prefix) {
        if (StatCollector.canTranslate(hint_key)) {
            //if (st.containsTranslateKey(hint_key) /* containsTranslateKey = containsTranslateKey */ ) {
            String hint = StatCollector.translateToLocal(hint_key);
            if (hint != null) {
                hint = hint.trim();
                if (hint.length() > 0) {
                    for (String s : hint.split("\\\\n") /* whee */) {
                        list.add(prefix + s);
                    }
                }
            }
        }
    }
    
    public static final String hintFormat = "" + EnumChatFormatting.DARK_PURPLE;
    public static final String shiftFormat = "" + EnumChatFormatting.DARK_GRAY + EnumChatFormatting.ITALIC;
    
    public static void brand(ItemStack is, EntityPlayer player, List list, boolean verbose) {
        final Item it = is.getItem();
        String name = it.getUnlocalizedName(is);
        addTranslationHints(name + ".hint", list, hintFormat);
        if (player != null && proxy.isClientHoldingShift()) {
            addTranslationHints(name + ".shift", list, shiftFormat);
        }
        ArrayList<String> untranslated = new ArrayList();
        if (it instanceof ItemFactorization) {
            ((ItemFactorization) it).addExtraInformation(is, player, untranslated, verbose);
        }
        String brand = "";
        if (FzConfig.add_branding) {
            brand += "Factorization";
        }
        if (cheat) {
            brand += " Cheat mode!";
        }
        if (dev_environ) {
            brand += " Development!";
        }
        if (brand.length() > 0) {
            untranslated.add(EnumChatFormatting.BLUE + brand.trim());
        }
        for (String s : untranslated) {
            list.add(StatCollector.translateToLocal(s));
        }
    }
    
    
    public static enum TabType {
        ART, CHARGE, OREP, SERVOS, ROCKETRY, TOOLS, BLOCKS, MATERIALS, COLOSSAL;
    }
    
    public static CreativeTabs tabFactorization = new CreativeTabs("factorizationTab") {
        @Override
        public Item getTabIconItem() {
            return registry.logicMatrixProgrammer;
        }
    };
    
    public static Item tab(Item item, TabType tabType) {
        item.setCreativeTab(tabFactorization);
        return item;
    }
    
    public static Block tab(Block block, TabType tabType) {
        block.setCreativeTab(tabFactorization);
        return block;
    }
    
    public static String getProperKey(ItemStack is) {
        String n = is.getUnlocalizedName();
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
    
    public static String translate(String key) {
        return ("" + StatCollector.translateToLocal(key + ".name")).trim();
    }
    
    public static String translateThis(String key) {
        return ("" + StatCollector.translateToLocal(key)).trim();
    }
    
    public static String translateExact(String key) {
        String ret = StatCollector.translateToLocal(key);
        if (ret == key) {
            return null;
        }
        return ret;
    }
    
    public static String tryTranslate(String key, String fallback) {
        String ret = translateExact(key);
        if (ret == null) {
            return fallback;
        }
        return ret;
    }
    
    public static boolean canTranslateExact(String key) {
        return translateExact(key) != null;
    }
    
    public static String translateWithCorrectableFormat(String key, Object... params) {
        String format = translate(key);
        String ret = String.format(format, params);
        String correctedTranslation = translateExact("factorization.replace:" + ret);
        if (correctedTranslation != null) {
            return correctedTranslation;
        }
        return ret;
    }
    
    public static void sendChatMessage(boolean raw, ICommandSender sender, String msg) {
        sender.addChatMessage(raw ? new ChatComponentText(msg) : new ChatComponentTranslation(msg));
    }
    
    public static void sendUnlocalizedChatMessage(ICommandSender sender, String format, Object... params) {
        sender.addChatMessage(new ChatComponentTranslation(format, params));
    }
    
    @SideOnly(Side.CLIENT)
    public static IIcon texture(IIconRegister reg, String name) {
        name = name.replace('.', '/');
        return reg.registerIcon(texture_dir + name);
    }
    
    public final static String texture_dir = "factorization:";
    public final static String model_dir = "/mods/factorization/models/";
    public final static String real_texture_dir = "/mods/factorization/textures/";
    public final static String gui_dir = "/mods/factorization/textures/gui/";
    public final static String gui_nei = "factorization:textures/gui/";
    public static final ResourceLocation blockAtlas = new ResourceLocation("textures/atlas/blocks.png");
    public static final ResourceLocation itemAtlas = new ResourceLocation("textures/atlas/items.png");
    
    public static ResourceLocation getResource(String name) {
        return new ResourceLocation("factorization", name);
    }
    
    @SideOnly(Side.CLIENT)
    public static void bindGuiTexture(String name) {
        //bad design; should have a GuiFz. meh.
        TextureManager tex = Minecraft.getMinecraft().renderEngine;
        tex.bindTexture(getResource("textures/gui/" + name + ".png"));
    }
    
    public static void loadBus(Object obj) {
        //@SubscribeEvent is the annotation the eventbus, *NOT* @EventHandler; that one is for mod containers.
        FMLCommonHandler.instance().bus().register(obj);
        MinecraftForge.EVENT_BUS.register(obj);
    }
    
    private static void validateEnvironment() {
        if (!LoadingPlugin.pluginInvoked) {
            String fml = "-Dfml.coreMods.load=factorization.coremod.LoadingPlugin";
            String ignore = "-Dfz.ignoreMissingCoremod=true";
            if ("".equals(System.getProperty("fz.ignoreMissingCoremod", ""))) {
                String dev = dev_environ ? "You're in a dev environ, so this is to be expected.\n" : "";
                throw new IllegalStateException("Coremod didn't load! Is your installation broken?\n" +
                        "Weird. It really is supposed to load, y'know...\n" +
                        "Anyways, try adding this flag to the JVM command line: " + fml + "\n" +
                        dev +
                        "(If the '-Dfml.coreMods.load' property is already being passed with another coremod, instead add the FZ coremod class with a comma.)\n" +
                        "If you want to force loading to continue anyways, without the coremod, " +
                        "pass the following flag, but many things (including blowing up diamond blocks) will be broken: " + ignore);
            } else {
                System.err.println("Coremod did not load! But continuing anyways; as per VM flag " + ignore);
            }
        }
        if (!dev_environ) {
            if ("".equals(System.getProperty("fz.dontVerifyAt", ""))) {
                AtVerifier.verify();
            }
        }
    }
}
