package factorization.common;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.command.ICommandSender;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatMessageComponent;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.Icon;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraft.util.StringTranslate;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.ChargeMetalBlockConductance;
import factorization.common.compat.CompatManager;
import factorization.common.servo.ServoMotor;

@Mod(modid = Core.modId, name = Core.name, version = Core.version, dependencies = "after:Forestry")
@NetworkMod(
        clientSideRequired = true,
        tinyPacketHandler = NetworkFactorization.class
        )
public class Core {
    //NORELEASE: GUIs need text and crafting GUIs need fixed textures
    //NORELEASE: We should repackage stuff. And rename the API package possibly.
    public static final String modId = "factorization";
    public static final String name = "Factorization";
    //The comment below is a marker used by the build script.
    public static final String version = "0.8.03dev1"; //@VERSION@
    public Core() {
        instance = this;
        fzconfig = new FzConfig();
        registry = new Registry();
        foph = new FactorizationOreProcessingHandler(); //We don't register foph yet.
        MinecraftForge.EVENT_BUS.register(registry);
    }
    
    // runtime storage
    public static Core instance;
    public static FzConfig fzconfig;
    public static Registry registry;
    public static FactorizationOreProcessingHandler foph;
    @SidedProxy(clientSide = "factorization.client.FactorizationClientProxy", serverSide = "factorization.common.FactorizationServerProxy")
    public static FactorizationProxy proxy;
    public static NetworkFactorization network;
    public static int factory_rendertype = -1;
    public static boolean finished_loading = false;

    public static boolean cheat = false;
    public static boolean cheat_servo_energy = false;
    public static boolean debug_network = false;
    public static boolean show_fine_logging = false;
    public static boolean dev_environ = (Boolean) ReflectionHelper.getPrivateValue(cpw.mods.fml.relauncher.CoreModManager.class, null, "deobfuscatedEnvironment");
    public static boolean servos_enabled = version.contains("dev");
    static {
        if (!dev_environ) {
            cheat = false;
            cheat_servo_energy = false;
        }
    }
    static public boolean serverStarted = false;



    @EventHandler
    public void load(FMLPreInitializationEvent event) {
        fzconfig.loadConfig(event.getSuggestedConfigurationFile());
        registry.makeBlocks();
        TickRegistry.registerTickHandler(registry, Side.SERVER);
        
        NetworkRegistry.instance().registerGuiHandler(this, proxy);

        registerSimpleTileEntities();
        registry.makeItems();
        FzConfig.config.save();
        registry.makeOther();
        registry.makeRecipes();
        registry.setToolEffectiveness();
        proxy.registerKeys();
        proxy.registerRenderers();
        
        FzConfig.config.save();
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            isMainClientThread.set(true);
        }
    }
    
    void registerSimpleTileEntities() {
        FactoryType.registerTileEntities();
        GameRegistry.registerTileEntity(TileEntityFzNull.class, "fz.null");
        //TileEntity renderers are registered in the client proxy
        
        EntityRegistry.registerModEntity(TileEntityWrathLamp.RelightTask.class, "factory_relight_task", 0, Core.instance, 1, 10, false);
        EntityRegistry.registerModEntity(ServoMotor.class, "factory_servo", 1, Core.instance, 100, 1, true);
    }

    @EventHandler
    public void modsLoaded(FMLPostInitializationEvent event) {
        TileEntityWrathFire.setupBurning();
        TileEntitySolarBoiler.setupSteam();
        foph.addDictOres();
        registry.sendIMC();
        registry.addOtherRecipes();
        (new CompatManager()).loadCompat();
        ChargeMetalBlockConductance.setup();
        finished_loading = true;
    }
    
    @EventHandler
    public void registerServerCommands(FMLServerStartingEvent event) {
        isMainServerThread.set(true);
        serverStarted = true;
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
        @Override
        protected Boolean initialValue() { return false; }
    };
    
    static ThreadLocal<Boolean> isMainServerThread = new ThreadLocal<Boolean>() {
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
        StringTranslate st = StringTranslate.getInstance();
        if (st.containsTranslateKey(hint_key) /* containsTranslateKey = containsTranslateKey */ ) {
            String hint = st.translateKey(hint_key);
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
    
    public static final String hintFormat = "" + EnumChatFormatting.ITALIC;
    public static final String shiftFormat = "" + EnumChatFormatting.DARK_GRAY + EnumChatFormatting.ITALIC;
    
    public static void brand(ItemStack is, EntityPlayer player, List list, boolean verbose) {
        final Item it = is.getItem();
        String name = it.getUnlocalizedName(is);
        addTranslationHints(name + ".hint", list, hintFormat);
        if (player != null && proxy.isClientHoldingShift()) {
            addTranslationHints(name + ".shift", list, shiftFormat);
        }
        if (it instanceof ItemFactorization) {
            ((ItemFactorization) it).addExtraInformation(is, player, list, verbose);
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
            list.add(EnumChatFormatting.BLUE + brand.trim());
        }
    }
    
    
    public static enum TabType {
        ART, CHARGE, OREP, SERVOS, ROCKETRY, TOOLS, BLOCKS, MATERIALS;
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
        item.setCreativeTab(tabFactorization);
        return item;
    }
    
    public static Block tab(Block block, TabType tabType) {
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
    
    public static String translate(String key) {
        return ("" + StatCollector.translateToLocal(key + ".name")).trim();
    }
    
    public static void sendChatMessage(boolean raw, ICommandSender sender, String msg) {
        sender.sendChatToPlayer(raw ? ChatMessageComponent.func_111066_d(msg) : ChatMessageComponent.func_111077_e(msg));
    }
    
    public static void sendUnlocalizedChatMessage(ICommandSender sender, String format, Object... params) {
        sender.sendChatToPlayer(ChatMessageComponent.func_111082_b(format, params));
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
    public final static String gui_nei = "factorization:textures/gui/";
//	public final static String texture_file_block = "/terrain.png";
//	public final static String texture_file_item = "/gui/items.png";
    
    public static final ResourceLocation blockAtlas = new ResourceLocation("textures/atlas/blocks.png");
    public static final ResourceLocation itemAtlas = new ResourceLocation("textures/atlas/items.png");
    public static Icon blockMissingIcon, itemMissingIcon;
    
    public static ResourceLocation getResource(String name) {
        return new ResourceLocation("factorization", name);
    }
    
    @SideOnly(Side.CLIENT)
    public static void bindGuiTexture(String name) {
        //bad design; should have a GuiFz. meh.
        TextureManager tex = Minecraft.getMinecraft().renderEngine;
        tex.func_110577_a(getResource("textures/gui/" + name + ".png"));
    }
}
