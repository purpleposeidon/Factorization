package factorization.misc;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.EnumSet;

import net.minecraft.entity.player.EntityPlayer;
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
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.ICraftingHandler;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.TickType;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.IConnectionHandler;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.common.network.Player;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;
import factorization.common.Core;
import factorization.common.FzConfig;

@Mod(modid = MiscellaneousNonsense.modId, name = MiscellaneousNonsense.name, version = Core.version, dependencies = "required-after: " + Core.modId)
@NetworkMod(clientSideRequired = true, packetHandler = MiscNet.class, channels = {MiscNet.tpsChannel}, connectionHandler = MiscellaneousNonsense.class)
public class MiscellaneousNonsense implements ITickHandler, IConnectionHandler {
    public static final String modId = Core.modId + ".misc";
    public static final String name = "Factorization Miscellaneous Nonsense";
    public static MiscNet net;
    @SidedProxy(clientSide = "factorization.misc.MiscClientProxy", serverSide = "factorization.misc.MiscProxy")
    public static MiscProxy proxy;
    public static MiscellaneousNonsense instance;
    public static int newMaxChatLength = 250;
    
    public MiscellaneousNonsense() {
        MiscellaneousNonsense.instance = this;
    }
    
    @EventHandler
    public void setParent(FMLPreInitializationEvent event) {
        event.getModMetadata().parent = Core.modId;
    }
    
    @EventHandler
    public void modsLoaded(FMLPostInitializationEvent event) {
        //Fixes lack of creeper dungeons
        DungeonHooks.addDungeonMob("Creeper", 1); //Etho, of all people, found one. It'd be nice if they were just a bit rarer. Scaling everything else up seems like a poor solution tho.
        String THATS_SOME_VERY_NICE_SOURCE_CODE_YOU_HAVE_THERE[] = {
                "##  ##",
                "##  ##",
                "  ##  ",
                " #### ",
                " #  # "
        };
        
        proxy.initializeClient();
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
        if (FzConfig.equal_opportunities_for_mobs) {
            MinecraftForge.EVENT_BUS.register(new MobEqualizer());
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
            if (measurements++ != FzConfig.tps_reporting_interval) {
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
