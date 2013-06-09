package factorization.misc;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.NetLoginHandler;
import net.minecraft.network.packet.NetHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.Packet1Login;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.AchievementList;
import net.minecraft.util.MathHelper;
import net.minecraftforge.common.DungeonHooks;
import cpw.mods.fml.common.ICraftingHandler;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.PostInit;
import cpw.mods.fml.common.Mod.PreInit;
import cpw.mods.fml.common.Mod.ServerStarting;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.TickType;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.IConnectionHandler;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.common.network.Player;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;
import factorization.common.Core;

@Mod(modid = MiscellaneousNonsense.modId, name = MiscellaneousNonsense.name, version = Core.version, dependencies = "required-after: " + Core.modId)
@NetworkMod(clientSideRequired = true, packetHandler = MiscNet.class, channels = {MiscNet.cmdChannel, MiscNet.tpsChannel}, connectionHandler = MiscellaneousNonsense.class)
public class MiscellaneousNonsense implements ITickHandler, IConnectionHandler {
    public static final String modId = Core.modId + ".misc";
    public static final String name = "Factorization Miscellaneous Nonsense";
    public static MiscNet net;
    @SidedProxy(clientSide = "factorization.misc.MiscClientProxy", serverSide = "factorization.misc.MiscProxy")
    public static MiscProxy proxy;
    public static MiscellaneousNonsense instance;
    
    public static final String RichardG_touches_himself_while_reading_my_code = "Confirmed to be true; there have been multiple sightings by respected authorities";
    
    public MiscellaneousNonsense() {
        MiscellaneousNonsense.instance = this;
    }
    
    @PreInit
    public void setParent(FMLPreInitializationEvent event) {
        event.getModMetadata().parent = Core.modId;
    }
    
    @PostInit
    public void modsLoaded(FMLPostInitializationEvent event) {
        //Fixes lack of creeper dungeons
        DungeonHooks.addDungeonMob("Creeper", 1); //Etho, of all people, found one. It'd be nice if they were just a bit rarer.
        String THATS_SOME_VERY_NICE_SOURCE_CODE_YOU_HAVE_THERE[] = {
                "##  ##",
                "##  ##",
                "  ##  ",
                " #### ",
                " #  # "
        };
        
        //Fixes achievements
        proxy.fixAchievements();
        GameRegistry.registerCraftingHandler(new ICraftingHandler() {
            @Override public void onSmelting(EntityPlayer player, ItemStack item) { }
            
            @Override
            public void onCrafting(EntityPlayer player, ItemStack stack, IInventory craftMatrix) {
                if (player == null) {
                    return;
                }
                Item item = stack.getItem();
                if (item == Item.hoeStone || item == Item.hoeIron) {
                    player.addStat(AchievementList.buildHoe, 1);
                }
                if (item == Item.swordStone || item == Item.swordIron) {
                    player.addStat(AchievementList.buildSword, 1);
                }
            }
        });
        proxy.registerLoadAlert();
        proxy.registerSprintKey();
        TickRegistry.registerTickHandler(this, Side.SERVER);
        TickRegistry.registerTickHandler(this, Side.CLIENT);
        //TODO: Make middle-clicking nicer
    }
    
    @ServerStarting
    public void addCommands(FMLServerStartingEvent event) {
        event.registerServerCommand(new FogCommand());
    }
    
    static class FogCommand extends CommandBase {

        @Override
        public String getCommandName() {
            return "f";
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (!(sender instanceof EntityPlayerMP)) {
                return;
            }
            EntityPlayerMP player = (EntityPlayerMP) sender;
            player.playerNetServerHandler.sendPacketToPlayer(instance.makeCmdPacket(args));
        }
        
        static List<String> fogCommands = Arrays.asList("far", "0", "normal", "1", "short", "2", "tiny", "3", "micro", "4", "microfog", "5", "+", "-", "pauserender", "now", "about", "clear", "saycoords", "saveoptions");
        
        @Override
        public List addTabCompletionOptions(ICommandSender sender, String[] args) {
            //TODO: Non-lame
            if (args.length == 1) {
                String arg0 = args[0];
                List<String> ret = new LinkedList();
                for (String cmd : fogCommands) {
                    if (cmd.startsWith(arg0)) {
                        ret.add(cmd);
                    }
                }
                return ret;
            } else if (args.length > 1) {
                return new LinkedList();
            }
            return fogCommands;
        }
        
        @Override
        public boolean canCommandSenderUseCommand(ICommandSender sender) {
            return sender instanceof EntityPlayerMP;
        }
        
        @Override
        public int getRequiredPermissionLevel() {
            return 0;
        }
    }
    
    public Packet makeCmdPacket(String items[]) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(outputStream);
            for (String i : items) {
                output.writeUTF(i);
            }
            output.flush();
            return PacketDispatcher.getPacket(MiscNet.cmdChannel, outputStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public Packet makeTpsReportPacket(float tps) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(outputStream);
            output.writeFloat(tps);
            output.flush();
            return PacketDispatcher.getPacket(MiscNet.tpsChannel, outputStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    
    private final double expected_tick_time_ms = 1000D/20D; //20 ticks/second = 20 ticks/1000 ms
    public float getTpsRatio() {
        //Yoink from GuiStatsComponent.updateStats
        MinecraftServer ms = MinecraftServer.getServer();
        double ticks_time_ms = MathHelper.average(ms.tickTimeArray)*1.0E-6D;
        return (float) Math.min(expected_tick_time_ms/ticks_time_ms, 1);
    }
    
    private float last_tps = -1;
    private int measurements = 0;
    @Override
    public void tickStart(EnumSet<TickType> type, Object... tickData) {
        //lag();
        if (type.contains(TickType.SERVER)) {
            MinecraftServer ms = MinecraftServer.getServer();
            if (ms.getTickCounter() < ms.tickTimeArray.length) {
                //Ignore startup
                return;
            }
            if (measurements++ != Core.tps_reporting_interval) {
                return;
            }
            measurements = 0;
            float tps = getTpsRatio();
            if (tps != last_tps) {
                PacketDispatcher.sendPacketToAllPlayers(makeTpsReportPacket(getTpsRatio()));
                last_tps = tps;
            }
            return;
        } else {
            LagssieWatchDog.ticks++;
        }
    }
    
    @Override
    public void tickEnd(EnumSet<TickType> type, Object... tickData) { }
    
    private static EnumSet<TickType> serverTicks = EnumSet.of(TickType.SERVER, TickType.CLIENT, TickType.RENDER);

    @Override
    public EnumSet<TickType> ticks() {
        return serverTicks;
    }

    @Override
    public String getLabel() {
        return "fz.misc";
    }

    @Override
    public void playerLoggedIn(Player player, NetHandler netHandler, INetworkManager manager) {
        MinecraftServer ms = MinecraftServer.getServer();
        if (ms.getTickCounter() < ms.tickTimeArray.length) {
            //Ignore startup
            return;
        }
        PacketDispatcher.sendPacketToPlayer(makeTpsReportPacket(getTpsRatio()), player);
    }

    @Override
    public String connectionReceived(NetLoginHandler netHandler, INetworkManager manager) {
        return null;
    }

    @Override
    public void connectionOpened(NetHandler netClientHandler, String server, int port, INetworkManager manager) { }

    @Override
    public void connectionOpened(NetHandler netClientHandler, MinecraftServer server, INetworkManager manager) { }

    @Override
    public void connectionClosed(INetworkManager manager) {
        proxy.handleTpsReport(1);
    }

    @Override
    public void clientLoggedIn(NetHandler clientHandler, INetworkManager manager, Packet1Login login) {}
    
    public static void lag() {
        try {
            Thread.sleep(1000 / 10);
        } catch (InterruptedException e) { }
    }
}
