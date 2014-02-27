package factorization.docs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import net.minecraft.block.Block;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatMessageComponent;
import net.minecraftforge.common.ForgeDirection;

import org.bouncycastle.util.encoders.Base64;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.relauncher.Side;
import factorization.api.Coord;
import factorization.api.DeltaCoord;
import factorization.api.ICoordFunction;
import factorization.fzds.TransferLib;
import factorization.notify.Notify;
import factorization.shared.Core;
import factorization.shared.FzUtil;

public class DocumentationModule implements ICommand {
    public static final DocumentationModule instance = new DocumentationModule(); 
    
    static HashMap<String, ItemStack> nameCache = null;
    
    public static ItemStack lookup(String name) {
        if (nameCache == null) {
            loadCache();
        }
        return nameCache.get(name);
    }

    private static void loadCache() {
        ArrayList<ItemStack> items = new ArrayList();
        for (Item it : Item.itemsList) {
            if (it == null) continue;
            try {
                it.getSubItems(it.itemID, null, items);
            } catch (Throwable t) {
                Core.logWarning("Error getting sub-items from item: " + it);
                t.printStackTrace();
            }
        }
        nameCache = new HashMap<String, ItemStack>(items.size());
        for (ItemStack is : items) {
            if (is == null) continue;
            nameCache.put(is.getUnlocalizedName(), is);
        }
    }
    
    
    //@EventHandler TODO?
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
        return "/fzdoc-serialize while standing on a golden axis";
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
        player.sendChatToPlayer(ChatMessageComponent.createFromText(msg));
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
        Block gold = Block.blockGold;
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
            ByteArrayInputStream bais = new ByteArrayInputStream(encoded.getBytes());
            NBTTagCompound it = decodeNBT(encoded);
            String cmd = "\\figure{" + encoded + "}";
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
        return w;
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
        tag.writeNamedTag(tag, dos);
        dos.close();
        byte[] compressedData = baos.toByteArray();
        byte[] enc = Base64.encode(compressedData);
        return new String(enc);
    }
    
    static NBTTagCompound decodeNBT(String contents) throws IOException {
        byte[] decoded = Base64.decode(contents);
        ByteArrayInputStream bais = new ByteArrayInputStream(decoded);
        return (NBTTagCompound) NBTTagCompound.readNamedTag(new DataInputStream(new GZIPInputStream(bais)));
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
    
}
