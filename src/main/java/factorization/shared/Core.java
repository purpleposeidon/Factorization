package factorization.shared;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.event.FMLMissingMappingsEvent.Action;
import cpw.mods.fml.common.event.FMLMissingMappingsEvent.MissingMapping;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.GameRegistry.Type;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.artifact.InspirationManager;
import factorization.beauty.EntityLeafBomb;
import factorization.charge.TileEntitySolarBoiler;
import factorization.citizen.EntityCitizen;
import factorization.colossi.BuildColossusCommand;
import factorization.colossi.ColossusController;
import factorization.colossi.ColossusFeature;
import factorization.common.FactorizationProxy;
import factorization.common.FactoryType;
import factorization.common.FzConfig;
import factorization.common.Registry;
import factorization.compat.CompatModuleLoader;
import factorization.coremod.AtVerifier;
import factorization.coremod.LoadingPlugin;
import factorization.darkiron.BlockDarkIronOre;
import factorization.fzds.Hammer;
import factorization.fzds.HammerEnabled;
import factorization.mechanics.MechanismsFeature;
import factorization.oreprocessing.FactorizationOreProcessingHandler;
import factorization.servo.ServoMotor;
import factorization.servo.stepper.EntityGrabController;
import factorization.servo.stepper.StepperEngine;
import factorization.truth.minecraft.DistributeDocs;
import factorization.util.DataUtil;
import factorization.util.FzUtil;
import factorization.weird.EntityMinecartDayBarrel;
import factorization.weird.poster.EntityPoster;
import factorization.wrath.TileEntityWrathLamp;
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
import net.minecraft.util.*;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.URL;
import java.util.*;

@Mod(
        modid = Core.modId,
        name = Core.name,
        version = Core.version,
        acceptedMinecraftVersions = "1.7.10",
        dependencies = "required-after:" + Hammer.modId,
        guiFactory = "factorization.common.FzConfigGuiFactory"
)
public class Core {
    public static final String modId = "factorization";
    public static final String name = "Factorization";
    public static final String version = "@FZVERSION@"; // Modified by build script, but we have to live with this in dev environ.

    public Core() {
        instance = this;
        fzconfig = new FzConfig();
        registry = new Registry();
        foph = new FactorizationOreProcessingHandler();
        network = new NetworkFactorization();
        netevent = new FzNetEventHandler();
        compatLoader = new CompatModuleLoader();
    }
    
    // runtime storage
    public static Core instance;
    public static FzConfig fzconfig;
    public static Registry registry;
    public static FactorizationOreProcessingHandler foph;
    @SidedProxy(clientSide = "factorization.common.FactorizationClientProxy", serverSide = "factorization.common.FactorizationProxy")
    public static FactorizationProxy proxy;
    public static NetworkFactorization network;
    public static FzNetEventHandler netevent;
    public static CompatModuleLoader compatLoader;
    public static int factory_rendertype = -1, nonte_rendertype = -1;
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

    private static boolean checked = false;
    public static void checkJar() {
        if (checked) return;
        checked = true;
        // Apparently some people somehow manage to get "Factorization.jar.zip", which somehow breaks the coremod.
        if (Core.dev_environ) {
            Core.logSevere("Dev environ; skipping jar check.");
            return;
        }
        if (Boolean.parseBoolean(System.getProperty("fz.dontCheckJar"))) {
            Core.logSevere("checkJar disabled by system property");
            return;
        }
        ModContainer mod = FMLCommonHandler.instance().findContainerFor(modId);
        if (mod == null) {
            Core.logSevere("I don't have a mod container? Wat?");
            return;
        }
        final File src = mod.getSource();
        if (src == null) {
            Core.logSevere("mod has null source!");
            return;
        }
        if (src.isDirectory()) {
            throw new RuntimeException("Factorization jar has been unpacked into a directory. Don't do that. Just put Factorization.jar in the mods/ folder");
        }
        final String path = src.getPath();
        if (!isBadName(path)) {
            Core.logSevere("Mod jar seems to have a valid filename");
            return;
        }
        String correctName = path.replaceAll("\\.zip$", ".jar");
        if (isBadName(correctName)) {
            // Carefully ensure we don't make a loop
            Core.logSevere("Failed to fix filename? " + path + " didn't work when changed to " + correctName);
            return;
        }

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

    @EventHandler
    public void load(FMLPreInitializationEvent event) {
        initializeLogging(event.getModLog());
        checkJar();
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

        String truth = "factorization.truth";
        FMLInterModComms.sendMessage(truth, "AddRecipeCategory", "Lacerator|factorization.oreprocessing.TileEntityGrinder|recipes");
        FMLInterModComms.sendMessage(truth, "AddRecipeCategory", "Crystallizer|factorization.oreprocessing.TileEntityCrystallizer|recipes");
        FMLInterModComms.sendMessage(truth, "AddRecipeCategory", "Slag Furnace|factorization.oreprocessing.TileEntitySlagFurnace$SlagRecipes|smeltingResults");
        FMLInterModComms.sendMessage(truth, "DocVar", "fzverion=" + Core.version);
        compatLoader.loadCompat();
        compatLoader.preinit(event);
    }
    
    void registerSimpleTileEntities() {
        FactoryType.registerTileEntities();
        GameRegistry.registerTileEntity(TileEntityFzNull.class, TileEntityFzNull.mapName);
        GameRegistry.registerTileEntity(BlockDarkIronOre.Glint.class, "fz.glint");
        //TileEntity renderers are registered in the client proxy

        // See EntityTracker.addEntityToTracker for reference on what the three last values should be
        EntityRegistry.registerModEntity(TileEntityWrathLamp.RelightTask.class, "fz_relight_task", 0, Core.instance, 1, 10, false);
        EntityRegistry.registerModEntity(ServoMotor.class, "factory_servo", 1, Core.instance, 100, 1, true);
        EntityRegistry.registerModEntity(ColossusController.class, "fz_colossal_controller", 2, Core.instance, 256, 20, false);
        EntityRegistry.registerModEntity(EntityPoster.class, "fz_entity_poster", 3, Core.instance, 160, Integer.MAX_VALUE, false);
        EntityRegistry.registerModEntity(EntityCitizen.class, "fz_entity_citizen", 4, Core.instance, 100, 1, true);
        EntityRegistry.registerModEntity(EntityMinecartDayBarrel.class, "fz_minecart_barrel", 5, this, 80, 3, true);
        EntityRegistry.registerModEntity(EntityLeafBomb.class, "fz_leaf_bomb", 6, this, 64, 10, true);
        EntityRegistry.registerModEntity(StepperEngine.class, "fz_stepper_engine", 7, Core.instance, 100, 1, true);
        EntityRegistry.registerModEntity(EntityGrabController.class, "fz_grab_controller", 8, Core.instance, 100, 1, true);
        // The "fz_" prefix isn't necessary these days; FML adds a prefix.
    }
    
    @EventHandler
    public void handleInteractions(FMLInitializationEvent event) {
        registry.sendIMC();
        ColossusFeature.init();
        PatreonRewards.init();
        InspirationManager.init();
        compatLoader.init(event);
    }

    @EventHandler
    public void modsLoaded(FMLPostInitializationEvent event) {
        TileEntitySolarBoiler.setupSteam();
        foph.addDictOres();

        registry.addOtherRecipes();
        for (FactoryType ft : FactoryType.values()) ft.getRepresentative(); // Make sure everyone's registered to the EVENT_BUS
        proxy.afterLoad();
        compatLoader.postinit(event);
        finished_loading = true;
        Blocks.diamond_block.setHardness(5.0F).setResistance(10.0F);
        validateEnvironment();
    }
    
    @EventHandler
    public void registerServerCommands(FMLServerStartingEvent event) {
        isMainServerThread.set(true);
        serverStarted = true;
        if (HammerEnabled.ENABLED) {
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
            HashSet<String> found = new HashSet<String>();
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
            return new HashSet<String>();
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
        // NORELEASE: Can remove in 1.8
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
        ArrayList<String> untranslated = new ArrayList<String>();
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
    
    
    public enum TabType {
        ART, CHARGE, OREP, SERVOS, ROCKETRY, TOOLS, BLOCKS, MATERIALS, COLOSSAL, ARTIFACT;
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
}
