package factorization.notify;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;

import factorization.util.FzUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.relauncher.Side;

@Mod(
        modid = NotifyImplementation.modId,
        name = NotifyImplementation.name,
        version = NotifyImplementation.version
)
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
        PointNetworkHandler.INSTANCE.initialize();
    }
    
    static void loadBus(Object obj) {
        // A copy of Core.loadBus(), for the sake of independence.
        FMLCommonHandler.instance().bus().register(obj);
        MinecraftForge.EVENT_BUS.register(obj);
    }
    
    @EventHandler
    public void setParent(FMLPreInitializationEvent event) {
        FzUtil.setCoreParent(event);
    }
    
    @EventHandler
    public void registerServerCommands(FMLServerStartingEvent event) {
        event.registerServerCommand(new MutterCommand());
    }
    
    void doSend(EntityPlayer player, Object where, World world, EnumSet<Style> style, ItemStack item, String format, String[] args) {
        if (where == null) {
            return;
        }
        if (player instanceof FakePlayer) {
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
                    world = te.getWorld();
                    x = te.pos.getX();
                    y = te.pos.getY();
                    z = te.pos.getZ();
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
            if (args == null) args = new String[0];
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
    
    void sendReplacableChatMessage(EntityPlayer player, IChatComponent msg, int msgKey) {
        if (player.worldObj.isRemote) {
            proxy.replaceable(msg, msgKey);
        } else {
            FMLProxyPacket packet = NotifyNetwork.replaceableChatPacket(msg, msgKey);
            NotifyNetwork.broadcast(packet, player, null);
        }
    }
}
