package factorization.truth;

import com.google.common.io.Closeables;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.common.FzConfig;
import factorization.coremodhooks.HookTargetsClient;
import factorization.shared.Core;
import factorization.truth.export.ExportHtml;
import factorization.truth.gen.*;
import factorization.util.FzUtil;
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
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class DocumentationModule {
    public static final DocumentationModule instance = new DocumentationModule();
    public static final HashMap<String, IDocGenerator> generators = new HashMap<String, IDocGenerator>();
    
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
                Core.logWarning("Error getting sub-items from item: " + it);
                t.printStackTrace();
            }
        }
        nameCache = new HashMap<String, ArrayList<ItemStack>>(items.size());
        for (ItemStack is : items) {
            if (is == null) continue;
            String itemName = is.getUnlocalizedName();
            ArrayList<ItemStack> list = nameCache.get(itemName);
            if (list == null) {
                list = new ArrayList<ItemStack>();
                nameCache.put(itemName, list);
            }
            list.add(is);
        }
    }
    
    public static void registerGenerator(String name, IDocGenerator gen) {
        generators.put(name, gen);
    }
    
    public void serverStarts(FMLServerStartingEvent event) {
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            event.registerServerCommand(new FzdocSerialize());
            if (Core.dev_environ) {
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
            //FIXME: Compiler disagrees with eclipse!
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
            return "\\obf{5*5*5*2*2 Internal Server Error\n\nAn error was encountered while trying to execute your request.}\n\n" + txt;
        }
    }
    
    private static String readContents(String name, InputStream is) throws IOException {
        if (is == null) {
            return "\\obf{101*2*2 Not Found:} " + name;
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
    
    @SideOnly(Side.CLIENT)
    public static void openPageForHilightedItem() {
        Minecraft mc = Minecraft.getMinecraft();
        if (!(mc.currentScreen instanceof GuiContainer)) return;
        GuiContainer screen = (GuiContainer) mc.currentScreen;
        //Copied from GuiScreen.handleMouseInput
        int mouseX = Mouse.getEventX() * screen.width / mc.displayWidth;
        int mouseY = screen.height - Mouse.getEventY() * screen.height / mc.displayHeight - 1;
        Slot slot = screen.getSlotAtPosition(mouseX, mouseY);
        if (slot == null) return;
        tryOpenBookForItem(slot.getStack());
    }

    public static String default_lookup_domain = "factorization";
    public static String default_recipe_domain = "factorization";
    public static final ArrayList<String> indexed_domains = new ArrayList<String>();

    @SideOnly(Side.CLIENT)
    public static boolean tryOpenBookForItem(ItemStack is) {
        Minecraft mc = Minecraft.getMinecraft();
        if (is == null) {
            mc.displayGuiScreen(new DocViewer(default_lookup_domain));
            return true;
        }
        EntityPlayer player = mc.thePlayer;
        if (player != null && !player.capabilities.isCreativeMode && FzConfig.require_book_for_manual) {
            // TODO: Add manwiches
            boolean found = false;
            for (ItemStack manual : player.inventory.mainInventory) {
                if (manual == null) continue;
                if (manual.getItem() != Core.registry.docbook) continue;
                found = true;
                break;
            }
            if (!found) {
                return true;
            }
        }
        String name = is.getUnlocalizedName();
        for (String domain : indexed_domains) {
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
        // if (Loader.isModLoaded("NotEnoughItems") || Loader.isModLoaded("craftguide")) return false;
        mc.displayGuiScreen(new DocViewer(default_recipe_domain, "cgi/recipes/" + name));
        return true;
    }
    
    static {
        registerGenerator("items", new ItemListViewer());
        registerGenerator("recipes", new RecipeViewer());
        registerGenerator("enchants", new EnchantViewer());
        registerGenerator("treasure", new TreasureViewer());
        registerGenerator("biomes", new BiomeViewer());
        registerGenerator("fluids", new FluidViewer());
        registerGenerator("oredictionary", new OreDictionaryViewer());
        registerGenerator("mods", new ModDependViewer());
        registerGenerator("worldgen", new WorldgenViewer());
        registerGenerator("eventbus", new EventbusViewer());
    }
}
