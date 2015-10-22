package factorization.truth;

import com.google.common.io.Closeables;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.event.FMLInterModComms;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.common.FzConfig;
import factorization.coremodhooks.HookTargetsClient;
import factorization.coremodhooks.UnhandledGuiKeyEvent;
import factorization.shared.Core;
import factorization.truth.api.DocReg;
import factorization.truth.api.IDocBook;
import factorization.truth.api.IManwich;
import factorization.truth.api.ITypesetCommand;
import factorization.truth.cmd.*;
import factorization.truth.export.ExportHtml;
import factorization.truth.gen.*;
import factorization.truth.gen.recipe.RecipeViewer;
import factorization.util.DataUtil;
import factorization.util.FzUtil;
import factorization.util.PlayerUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.base64.Base64;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Mod(
        modid = DocumentationModule.modid,
        name = "Truth",
        version = Core.version
)
public class DocumentationModule implements factorization.truth.api.IDocModule {
    public static final String modid = "factorization.truth";
    public static DocumentationModule instance;

    public DocumentationModule() {
        DocReg.module = instance = this;
    }

    @Mod.EventHandler
    public void init(FMLPreInitializationEvent event) {
        FzUtil.setCoreParent(event);
        DocReg.registerGenerator("items", new ItemListViewer());
        DocReg.registerGenerator("recipes", new RecipeViewer());
        DocReg.registerGenerator("enchants", new EnchantViewer());
        DocReg.registerGenerator("treasure", new TreasureViewer());
        DocReg.registerGenerator("biomes", new BiomeViewer());
        DocReg.registerGenerator("fluids", new FluidViewer());
        DocReg.registerGenerator("oredictionary", new OreDictionaryViewer());
        DocReg.registerGenerator("mods", new ModDependViewer());
        DocReg.registerGenerator("worldgen", new WorldgenViewer());
        DocReg.registerGenerator("eventbus", new EventbusViewer());
        DocReg.registerGenerator("tesrs", new TesrViewer());
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            Core.loadBus(new DocKeyListener());
        }

        for (ModContainer mod : Loader.instance().getActiveModList()) {
            DocReg.setVariable("mod:" + mod.getModId(), mod.getName());
        }

        registerCommands();
    }

    private static void reg(String name, ITypesetCommand cmd) {
        DocReg.registerCommand("\\" + name, cmd);
    }

    private static void registerCommands() {
        reg("include", new CmdInclude());
        reg("lmp", new CmdMacro("\\link{lmp}{LMP}"));
        reg("p", new CmdP());
        reg("-", new CmdDash());
        reg("", new CmdSpace());
        reg(" ", new CmdSpace());
        reg("\\", new CmdSlash());
        reg("nl", new CmdNl());
        reg("newpage", new CmdNewpage());
        reg("leftpage", new CmdLeftpage());
        reg("b", new CmdStyle(EnumChatFormatting.BOLD, "b"));
        reg("i", new CmdStyle(EnumChatFormatting.ITALIC, "i"));
        reg("u", new CmdStyle(EnumChatFormatting.UNDERLINE, "u"));
        reg("title", new CmdTitle());
        reg("h1", new CmdHeader());
        reg("link", new CmdLink(false));
        reg("index", new CmdLink(true));
        reg("#", new CmdItem());
        reg("img", new CmdImg());
        reg("figure", new CmdFigure());
        reg("generate", new CmdGenerate());
        reg("seg", new CmdSegStart());
        reg("endseg", new CmdSegEnd());
        reg("topic", new CmdTopic());
        reg("checkmods", new CmdCheckMods());
        reg("vpad", new CmdVpad());
        reg("ifhtml", new CmdIfHtml());
        reg("url", new CmdUrl());
        reg("local", new CmdLocal());
        reg("for", new CmdFor());
    }

    static HashMap<String, ArrayList<ItemStack>> nameCache = null;
    
    public static ArrayList<ItemStack> lookup(String name) {
        return getNameItemCache().get(name);
    }
    
    public static HashMap<String, ArrayList<ItemStack>> getNameItemCache() {
        if (nameCache == null) {
            loadCache();
        }
        return nameCache;
    }

    private static void loadCache() {
        ArrayList<ItemStack> items = new ArrayList<ItemStack>();
        for (Item it : (Iterable<Item>) Item.itemRegistry) {
            if (it == null) continue;
            try {
                it.getSubItems(it, it.getCreativeTab(), items);
            } catch (Throwable t) {
                Core.logWarning("Error getting sub-items from item: " + it + " " + DataUtil.getName(it));
                t.printStackTrace();
            }
        }
        nameCache = new HashMap<String, ArrayList<ItemStack>>(items.size());
        for (ItemStack is : items) {
            if (is == null) continue;
            try {
                String itemName = is.getUnlocalizedName();
                ArrayList<ItemStack> list = nameCache.get(itemName);
                if (list == null) {
                    list = new ArrayList<ItemStack>();
                    nameCache.put(itemName, list);
                }
                list.add(is);
            } catch (Throwable t) {
                Core.logSevere("Error getting names from item: " + is.getItem() + " " + DataUtil.getName(is.getItem()));
                t.printStackTrace();
            }
        }
    }

    @Mod.EventHandler
    public void serverStarts(FMLServerStartingEvent event) {
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            if (Core.dev_environ || Boolean.getBoolean("fz.registerDocCommands")) {
                event.registerServerCommand(new FzdocSerialize());
                event.registerServerCommand(new ExportHtml());
            }
        }
    }
    
    //NBT write -> compress -> base64 encode
    //base64 decode -> decompress -> NBT load
    
    static String encodeNBT(NBTTagCompound tag) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(new GZIPOutputStream(baos));
        CompressedStreamTools.write(tag, dos);
        dos.close();
        byte[] compressedData = baos.toByteArray();
        ByteBuf enc = Base64.encode(Unpooled.copiedBuffer(compressedData));
        return new String(enc.array());
    }
    
    static NBTTagCompound decodeNBT(String contents) throws IOException {
        ByteBuf decoded = Base64.decode(Unpooled.copiedBuffer(contents.getBytes()));
        ByteBufInputStream bais = new ByteBufInputStream(decoded);
        return CompressedStreamTools.read(new DataInputStream(new GZIPInputStream(bais)));
    }
    
    public static DocWorld loadWorld(String text) {
        try {
            HookTargetsClient.abort.set(Boolean.TRUE);
            NBTTagCompound tag = decodeNBT(text);
            return new DocWorld(tag);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            HookTargetsClient.abort.remove();
        }
    }
    
    public static ResourceLocation getResourceForName(String domain, String name) {
        return new ResourceLocation(domain, "doc/" + name + ".txt");
    }
    
    public static IResourceManager overrideResourceManager = null;
    
    public static InputStream getDocumentResource(String domain, String name) {
        try {
            IResourceManager irm = overrideResourceManager != null ? overrideResourceManager : Minecraft.getMinecraft().getResourceManager();
            IResource src = irm.getResource(getResourceForName(domain, name));
            return src.getInputStream();
        } catch (Throwable e) {
            // FIXME: Compiler disagrees with eclipse!
            if (e instanceof IOException) {
                return null;
            }
            e.printStackTrace();
            return null;
        }
    }
    
    public static String readDocument(String domain, String name) {
        try {
            return dispatchDocument(domain, name);
        } catch (Throwable e) {
            e.printStackTrace();
            String txt = e.getMessage();
            for (StackTraceElement ste : e.getStackTrace()) {
                txt += "\n\n    at " + ste.getFileName() + "(" + ste.getFileName() + ":" + ste.getLineNumber() + ")";
            }
            return "\\5*5*5*2*2 Internal Server Error\n\nAn error was encountered while trying to execute your request.\n\n" + txt;
        }
    }
    
    private static String readContents(String name, InputStream is) throws IOException {
        if (is == null) {
            return "\\101*2*2 Not Found: " + name;
        }
        StringBuilder build = new StringBuilder();
        byte[] buf = new byte[1024];
        int length;
        while ((length = is.read(buf)) != -1) {
            build.append(new String(buf, 0, length));
        }
        return build.toString();
    }
    
    private static String dispatchDocument(String domain, String name) throws IOException {
        //NORELEASE: Okay. The document *really* needs to be cached. Things are getting expensive...
        if (name.startsWith("cgi/")) {
            return "\\generate{" + name.replace("cgi/", "") + "}";
        } else {
            InputStream is = null;
            try {
                is = DocumentationModule.getDocumentResource(domain, name);
                return DocumentationModule.readContents(name, is);
            } finally {
                Closeables.close(is, false);
            }
        }
    }

    public static String textifyItem(ItemStack is) {
        String name = is.getUnlocalizedName();
        return "\\#{" + name + "} " + is.getDisplayName();
    }

    public static Slot getSlotUnderMouse() {
        Minecraft mc = Minecraft.getMinecraft();
        if (!(mc.currentScreen instanceof GuiContainer)) return null;
        GuiContainer screen = (GuiContainer) mc.currentScreen;
        //Copied from GuiScreen.handleMouseInput
        int mouseX = Mouse.getEventX() * screen.width / mc.displayWidth;
        int mouseY = screen.height - Mouse.getEventY() * screen.height / mc.displayHeight - 1;
        return screen.getSlotAtPosition(mouseX, mouseY);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void openPageForHilightedItem() {
        Slot slot = getSlotUnderMouse();
        ItemStack stack = slot == null ? null : slot.getStack();
        openBookForItem(stack, false);
    }


    @Override
    @SideOnly(Side.CLIENT)
    public boolean openBookForItem(ItemStack is, boolean forceOpen) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        if (player == null) return false;
        String found_domain = DocReg.default_lookup_domain;
        boolean found = forceOpen
                || PlayerUtil.isPlayerCreative(player)
                || !FzConfig.require_book_for_manual;
        if (!found) {
            for (ItemStack manual : player.inventory.mainInventory) {
                if (manual == null) continue;
                if (manual.getItem() instanceof IDocBook) {
                    found_domain = ((IDocBook) (manual.getItem())).getDocumentationDomain();
                    found = true;
                    break;
                }
            }
        }
        if (!found) {
            IManwich first = null;
            for (IManwich manwich : DocReg.manwiches) {
                if (first == null) first = manwich;
                if (manwich.hasManual(player) > 0) {
                    found = true;
                    found_domain = manwich.getManwichDomain(player);
                    break;
                }
            }
            if (!found) {
                if (first != null) {
                    first.recommendManwich(player);
                }
                return false;
            }
        }
        if (is == null) {
            mc.displayGuiScreen(new DocViewer(found_domain));
            return true;
        }
        String name = is.getUnlocalizedName();
        for (String domain : DocReg.indexed_domains) {
            InputStream topic_index = getDocumentResource(domain, "topic_index");
            if (topic_index == null) return false;
            BufferedReader br = new BufferedReader(new InputStreamReader(topic_index));
            try {
                while (true) {
                    String line = br.readLine();
                    if (line == null) {
                        break;
                    }
                    String[] bits = line.split("\t");
                    if (bits.length >= 2) {
                        if (bits[0].equalsIgnoreCase(name)) {
                            String filename = bits[1];
                            mc.displayGuiScreen(new DocViewer(domain, filename));
                            return true;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                FzUtil.closeNoisily("closing topic_index", topic_index);
            }
        }
        mc.displayGuiScreen(new DocViewer(DocReg.default_recipe_domain, "cgi/recipes/" + name));
        return true;
    }

    public static class DocKeyListener {
        boolean hasNei = Loader.isModLoaded("NotEnoughItems");

        @SubscribeEvent
        public void keyPress(UnhandledGuiKeyEvent event) {
            // Except NEI's keys might be configurable.
            // TODO: Make these keys configurable tho
            if (event.chr == '?') {
                DocReg.module.openPageForHilightedItem();
            } else if ((event.chr == 'r' || event.chr == 'R') && !hasNei) {
                DocReg.module.openPageForHilightedItem();
            }
        }
    }

    @Mod.EventHandler
    public void handleImc(FMLInterModComms.IMCEvent event) {
        for (FMLInterModComms.IMCMessage message : event.getMessages()) {
            try {
                RecipeViewer.handleImc(message);
                handleImc(message);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    private void handleImc(FMLInterModComms.IMCMessage message) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        if (!message.key.equals("DocVar")) return;
        String[] parts = message.key.split("=", 1);
        String key = parts[0];
        String val = parts[1];
        if (key.endsWith("+")) {
            key = key.replace("+", "");
            DocReg.appendVariable(key, val);
        } else {
            DocReg.setVariable(key, val);
        }
    }
}
