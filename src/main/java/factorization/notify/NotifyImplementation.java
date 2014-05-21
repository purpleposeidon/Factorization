package factorization.notify;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.MinecraftForge;

import com.google.common.base.Joiner;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import cpw.mods.fml.relauncher.Side;

@Mod(modid = NotifyImplementation.modId, name = NotifyImplementation.name, version = NotifyImplementation.version)
public class NotifyImplementation extends Notify {
    public static final String modId = "factorization.notify";
    public static final String name = "Factorization Notification System";
    public static final String version = "1.0";
    
    @SidedProxy(clientSide = "factorization.notify.RenderMessages", serverSide = "factorization.notify.RenderMessagesProxy")
    public static RenderMessagesProxy proxy;
    public static NotifyNetwork net = new NotifyNetwork();
    
    
    {
        Notify.instance = this;
        loadBus(this);
    }
    
    static void loadBus(Object obj) {
        // A copy of Core.loadBus(), for the sake of independence.
        FMLCommonHandler.instance().bus().register(obj);
        MinecraftForge.EVENT_BUS.register(obj);
    }
    
    @EventHandler
    public void setParent(FMLPreInitializationEvent event) {
        final String FZ = "factorization";
        if (Loader.isModLoaded(FZ)) {
            event.getModMetadata().parent = FZ;
        }
    }
    
    @EventHandler
    public void registerServerCommands(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandBase() {
            @Override
            public void processCommand(ICommandSender sender, String[] args) {
                if (!(sender instanceof TileEntity || sender instanceof Entity)) {
                    return;
                }
                EnumSet theStyle = EnumSet.noneOf(Style.class);
                ItemStack heldItem = null;
                if (sender instanceof EntityLivingBase) {
                    heldItem = ((EntityLivingBase) sender).getHeldItem();
                }
                ItemStack sendItem = null;
                for (int i = 0; i < args.length; i++) {
                    String s = args[i];
                    if (s.equalsIgnoreCase("--long")) {
                        theStyle.add(Style.LONG);
                    } else if (s.equalsIgnoreCase("--show-item") && heldItem != null) {
                        theStyle.add(Style.DRAWITEM);
                        sendItem = heldItem;
                    } else {
                        break;
                    }
                    args[i] = null;
                }
                String msg = Joiner.on(" ").skipNulls().join(args);
                msg = msg.replace("\\n", "\n");
                Notify.send(null, sender, theStyle, sendItem, "%s", msg);
            }
            
            @Override
            public String getCommandUsage(ICommandSender icommandsender) {
                return "/mutter [--long] [--show-item] some text. Clears if empty";
            }
            
            @Override
            public String getCommandName() {
                return "mutter";
            }
            
            @Override
            public boolean canCommandSenderUseCommand(ICommandSender sender) {
                return sender instanceof Entity;
            }
            
            @Override
            public int getRequiredPermissionLevel() {
                return 0;
            }
            
            // o_ó eclipse has no trouble compiling without these two methods...
            public int compareTo(ICommand otherCmd) {
                return this.getCommandName().compareTo(otherCmd.getCommandName());
            }

            public int compareTo(Object obj) {
                return this.compareTo((ICommand)obj);
            }
        });
    }
    
    @Override
    protected void doSend(EntityPlayer player, Object where, EnumSet<Style> style, ItemStack item, String format, String[] args) {
        if (where == null) {
            return;
        }
        format = styleMessage(style, format);
        if ((player != null && player.worldObj.isRemote) || FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
            proxy.addMessage(where, item, format, args);
        } else {
            FMLProxyPacket packet = NotifyNetwork.notifyPacket(where, item, format, args);
            NotifyNetwork.broadcast(packet, player);
        }
    }
    
    public static void recieve(EntityPlayer player, Object where, ItemStack item, String styledFormat, String[] args) {
        if (where == null) {
            return;
        }
        proxy.addMessage(where, item, styledFormat, args);
    }
    
    String styleMessage(EnumSet<Style> style, String format) {
        if (style == null) {
            return "\n" + format;
        }
        String prefix = "";
        String sep = "";
        for (Style s : style) {
            prefix += sep + s.toString();
            sep = " ";
        }
        return prefix + "\n" + format;
    }
    
    static EnumSet<Style> loadStyle(String firstLine) {
        EnumSet<Style> ret = EnumSet.noneOf(Style.class);
        for (String s : firstLine.split(" ")) {
            try {
                ret.add(Style.valueOf(s));
            } catch (IllegalArgumentException e) {}
        }
        return ret;
    }
    
    static ArrayList<RecuringNotification> recuring_notifications = new ArrayList();
    @SubscribeEvent
    public void updateRecuringNotifications(ServerTickEvent event) {
        if (event.phase != Phase.END) return;
        for (Iterator<RecuringNotification> iterator = recuring_notifications.iterator(); iterator.hasNext();) {
            RecuringNotification rn = iterator.next();
            if (rn.isInvalid()) {
                iterator.remove();
                continue;
            }
            if (rn.updater.update(rn.first) == false) {
                iterator.remove();
            }
            rn.first = false;
        }
    }
    
    @Override
    protected void addRecuringNotification(RecuringNotification newRN) {
        for (Iterator<RecuringNotification> iterator = recuring_notifications.iterator(); iterator.hasNext();) {
            RecuringNotification rn = iterator.next();
            if (rn.where.equals(newRN.where) && (newRN.player == null || newRN.player == rn.player)) {
                iterator.remove();
            }
        }
        recuring_notifications.add(newRN);
    }
    
    @Override
    protected void doSendOnscreenMessage(EntityPlayer player, String message, String[] formatArgs) {
        if (player.worldObj.isRemote) {
            proxy.onscreen(message, formatArgs);
        } else {
            FMLProxyPacket packet = NotifyNetwork.onscreenPacket(message, formatArgs);
            NotifyNetwork.broadcast(packet, player);
        }
    }
}
