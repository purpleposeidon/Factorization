package factorization.docs;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.base64.Base64;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.input.Mouse;

import com.google.common.io.Closeables;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Coord;
import factorization.api.DeltaCoord;
import factorization.api.ICoordFunction;
import factorization.notify.Notify;
import factorization.shared.Core;
import factorization.shared.FzUtil;

public class DocumentationModule implements ICommand {
    public static final DocumentationModule instance = new DocumentationModule();
    static HashMap<String, IDocGenerator> generators = new HashMap();
    
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
        ArrayList<ItemStack> items = new ArrayList();
        for (Item it : (Iterable<Item>) Item.itemRegistry) {
            if (it == null) continue;
            try {
                it.getSubItems(it, null, items);
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
                list = new ArrayList();
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
            event.registerServerCommand(this);
        }
    }

    @Override
    public int compareTo(Object arg0) {
        if (arg0 instanceof ICommand) {
            ICommand other = (ICommand) arg0;
            return getCommandName().compareTo(other.getCommandName());
        }
        return 0;
    }

    @Override
    public String getCommandName() {
        return "fzdoc-figure";
    }

    @Override
    public String getCommandUsage(ICommandSender icommandsender) {
        return "/fzdoc-serialize generates an FZDoc \\figure command";
    }

    @Override
    public List getCommandAliases() { return null; }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender icommandsender) {
        return icommandsender instanceof EntityPlayer && MinecraftServer.getServer().getConfigurationManager().isPlayerOpped(icommandsender.getCommandSenderName());
    }

    @Override
    public List addTabCompletionOptions(ICommandSender icommandsender, String[] astring) { return null; }

    @Override
    public boolean isUsernameIndex(String[] astring, int i) { return false; }
    
    void msg(ICommandSender player, String msg) {
        player.addChatMessage(new ChatComponentText(msg));
    }
    
    int measure(Coord bottom, ForgeDirection east, ForgeDirection west, Block gold) {
        Coord at = bottom.copy();
        ForgeDirection d = bottom.add(east).is(gold) ? east : west;
        int size = 0;
        while (at.is(gold)) {
            at.adjust(d);
            size++;
        }
        return size*(d.offsetX + d.offsetY + d.offsetZ);
    }
    
    @Override
    public void processCommand(ICommandSender icommandsender, String[] astring) {
        if (!(icommandsender instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) icommandsender;
        Coord peak = new Coord(player).add(ForgeDirection.DOWN);
        Block gold = Blocks.gold_block;
        if (!peak.is(gold)) {
            msg(player, "Not on a gold block");
            return;
        }
        int ySize = 0;
        Coord at = peak.copy();
        while (at.is(gold)) {
            at.adjust(ForgeDirection.DOWN);
            ySize--;
        }
        at.adjust(ForgeDirection.UP);
        Coord bottom = at.copy();
        int xSize = measure(bottom, ForgeDirection.EAST, ForgeDirection.WEST, gold);
        int zSize = measure(bottom, ForgeDirection.SOUTH, ForgeDirection.NORTH, gold);
        
        if (xSize*ySize*zSize == 0) {
            msg(player, "Invalid dimensions");
            return;
        }
        if (Math.abs(xSize) > 0xF) {
            msg(player, "X axis is too large");
            return;
        }
        if (Math.abs(ySize) > 0xF) {
            msg(player, "Y ayis is too large");
            return;
        }
        if (Math.abs(zSize) > 0xF) {
            msg(player, "Z azis is too large");
            return;
        }
        
        xSize -= 2*Math.signum(xSize);
        zSize -= 2*Math.signum(zSize);
        ySize += 1;
        peak.x += Math.signum(xSize);
        peak.z += Math.signum(zSize);
        Coord max = peak.copy();
        Coord min = peak.add(xSize, ySize, zSize);
        Coord.sort(min, max);
        Notify.send(max, "max");
        Notify.send(min, "min");
        DocWorld dw = copyChunkToWorld(min, max);
        NBTTagCompound worldTag = new NBTTagCompound();
        dw.writeToTag(worldTag);
        try {
            String encoded = encodeNBT(worldTag);
            String cmd = "\\figure{\n" + encoded + "}";
            cmd = cmd.replace("\0", "");
            System.out.println(cmd);
            FzUtil.copyStringToClipboard(cmd);
            msg(player, "\\figure command copied to the clipboard");
        } catch (Throwable t) {
            msg(player, "An error occured");
            t.printStackTrace();
        }
    }
    
    DocWorld copyChunkToWorld(final Coord min, final Coord max) {
        final DocWorld w = new DocWorld();
        DeltaCoord size = max.difference(min);
        DeltaCoord maxSize = new DeltaCoord(0xF, 0xF, 0xF);
        final DeltaCoord start = new DeltaCoord(0, 0, 0); //size.add(maxSize.scale(-1)).scale(0.5);
        Coord.iterateCube(min, max, new ICoordFunction() { @Override public void handle(Coord here) {
            if (here.isAir()) return;
            DeltaCoord dc = here.difference(min).add(start);
            w.setIdMdTe(dc, here.getId(), here.getMd(), here.getTE());
        }});
        DeltaCoord d = max.difference(min);
        d.y /= 2; // The top always points up, so it can be pretty tall
        w.diagonal = (int) (d.magnitude() + 1);
        copyEntities(w, min, max);
        w.orig.set(min);
        return w;
    }
    
    void copyEntities(DocWorld dw20, Coord min, Coord max) {
        AxisAlignedBB ab = Coord.aabbFromRange(min, max);
        List<Entity> ents = min.w.getEntitiesWithinAABBExcludingEntity(null, ab);
        for (Entity ent : ents) {
            if (ent instanceof EntityPlayer) {
                continue; //??? We probably could get away with it...
            }
            dw20.addEntity(ent);
        }
    }
    
    static void debugBytes(String header, byte[] d) {
        System.out.println(header + " #" + d.length);
        for (byte b : d) {
            System.out.print(" " + Integer.toString(b));
        }
        System.out.println();
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
            NBTTagCompound tag = decodeNBT(text);
            return new DocWorld(tag);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static ResourceLocation getResourceForName(String name) {
        return Core.getResource("doc/" + name + ".txt");
    }
    
    public static IResourceManager overrideResourceManager = null;
    
    public static InputStream getDocumentResource(String name) {
        try {
            IResourceManager irm = overrideResourceManager != null ? overrideResourceManager : Minecraft.getMinecraft().getResourceManager();
            IResource src = irm.getResource(getResourceForName(name));
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
    
    public static String readDocument(String name) {
        try {
            return dispatchDocument(name);
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
    
    private static String dispatchDocument(String name) throws IOException {
        //NORELEASE: Okay. The document *really* needs to be cached. Things are getting expensive...
        if (name.startsWith("cgi/")) {
            return "\\generate{" + name.replace("cgi/", "") + "}";
        } else {
            InputStream is = null;
            try {
                is = DocumentationModule.getDocumentResource(name);
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
    
    @SideOnly(Side.CLIENT)
    public static boolean tryOpenBookForItem(ItemStack is) {
        if (is == null) return false;
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        if (!player.capabilities.isCreativeMode) {
            ItemStack manual = player.getHeldItem();
            if (manual == null) return false;
            if (manual.getItem() != Core.registry.docbook) return false;
        }
        String name = is.getUnlocalizedName();
        InputStream topic_index = getDocumentResource("topic_index");
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
                        mc.displayGuiScreen(new DocViewer(filename));
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            FzUtil.closeNoisily("closing topic_index", topic_index);
        }
        if (Loader.isModLoaded("NotEnoughItems") || Loader.isModLoaded("craftguide")) return false;
        mc.displayGuiScreen(new DocViewer("cgi/recipes/for/" + name));
        return true;
    }
    
    static void registerGenerators() {
        registerGenerator("items", new ItemListViewer());
        registerGenerator("recipes", new RecipeViewer());
    }
}
