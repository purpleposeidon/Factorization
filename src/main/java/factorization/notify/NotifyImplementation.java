package factorization.notify;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
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
import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import cpw.mods.fml.relauncher.Side;

@Mod(modid = NotifyImplementation.modId, name = NotifyImplementation.name, version = NotifyImplementation.version)
public class NotifyImplementation {
    public static final String modId = "factorization.notify";
    public static final String name = "Factorization Notification System";
    public static final String version = "1.0";
    
    @SidedProxy(clientSide = "factorization.notify.RenderMessages", serverSide = "factorization.notify.RenderMessagesProxy")
    public static RenderMessagesProxy proxy;
    public static NotifyNetwork net = new NotifyNetwork();
    
    public static NotifyImplementation instance;
    
    {
        NotifyImplementation.instance = this;
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
        event.registerServerCommand(new MutterCommand());
    }
    
    void doSend(EntityPlayer player, Object where, World world, EnumSet<Style> style, ItemStack item, String format, String[] args) {
        if (where == null) {
            return;
        }
        format = styleMessage(style, format);
        if ((player != null && player.worldObj.isRemote) || FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
            proxy.addMessage(where, item, format, args);
        } else {
            TargetPoint target = null;
            if (player == null) {
                final int range = style.contains(Style.DRAWFAR) ? 128 : 32;
                int x = 0, y = 0, z = 0;
                boolean failed = false;
                if (where instanceof ISaneCoord) {
                    ISaneCoord c = (ISaneCoord) where;
                    world = c.w();
                    x = c.x();
                    y = c.y();
                    z = c.z();
                } else if (where instanceof TileEntity) {
                    TileEntity te = (TileEntity) where;
                    world = te.getWorldObj();
                    x = te.xCoord;
                    y = te.yCoord;
                    z = te.zCoord;
                } else if (where instanceof Entity) {
                    Entity ent = (Entity) where;
                    world = ent.worldObj;
                    x = (int) ent.posX;
                    y = (int) ent.posY;
                    z = (int) ent.posZ;
                } else if (where instanceof Vec3) {
                    Vec3 vec = (Vec3) where;
                    x = (int) vec.xCoord;
                    y = (int) vec.yCoord;
                    z = (int) vec.zCoord;
                } else {
                    failed = true;
                }
                if (world != null && !failed) {
                    int dimension = world.getWorldInfo().getVanillaDimension();
                    target = new TargetPoint(dimension, x, y, z, range);
                }
            }
            FMLProxyPacket packet = NotifyNetwork.notifyPacket(where, item, format, args);
            NotifyNetwork.broadcast(packet, player, target);
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
    
    private static ArrayList<Notice> recuring_notifications = new ArrayList();
    
    @SubscribeEvent
    public void updateRecuringNotifications(ServerTickEvent event) {
        if (event.phase != Phase.END) return;
        synchronized (recuring_notifications) {
            Iterator<Notice> iterator = recuring_notifications.iterator();
            while (iterator.hasNext()) {
                Notice rn = iterator.next();
                if (rn.isInvalid() || !rn.updateNotice()) {
                    iterator.remove();
                }
            }
        }
    }
    
    void addRecuringNotification(Notice newRN) {
        synchronized (recuring_notifications) {
            Iterator<Notice> iterator = recuring_notifications.iterator();
            while (iterator.hasNext()) {
                Notice rn = iterator.next();
                if (rn.where.equals(newRN.where) && (newRN.targetPlayer == null || newRN.targetPlayer == rn.targetPlayer)) {
                    iterator.remove();
                }
            }
            recuring_notifications.add(newRN);
        }
    }
    
    void doSendOnscreenMessage(EntityPlayer player, String message, String[] formatArgs) {
        if (player.worldObj.isRemote) {
            proxy.onscreen(message, formatArgs);
        } else {
            FMLProxyPacket packet = NotifyNetwork.onscreenPacket(message, formatArgs);
            NotifyNetwork.broadcast(packet, player, null);
        }
    }
}
