package factorization.misc;

import ibxm.Player;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;

import net.minecraft.entity.item.EntityFireworkRocket;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.AchievementList;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.DungeonHooks;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import factorization.api.Coord;
import factorization.common.FzConfig;
import factorization.shared.Core;

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
    public static Embarkener embarkener = null;
    
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
                if (item == Items.hoeStone || item == Items.hoeIron) {
                    player.addStat(AchievementList.buildHoe, 1);
                }
                if (item == Items.swordStone || item == Items.swordIron) {
                    player.addStat(AchievementList.buildSword, 1);
                }
            }
        });
        proxy.registerLoadAlert();
        proxy.registerSprintKey();
        TickRegistry.registerTickHandler(this, Side.SERVER);
        TickRegistry.registerTickHandler(this, Side.CLIENT);
        if (FzConfig.equal_opportunities_for_mobs) {
            MinecraftForge.EVENT_BUS.register(new MobEqualizer());
        }
        if (FzConfig.embarken_wood) {
            embarkener = new Embarkener();
        }
        if (FzConfig.proper_projectile_physics) {
            MinecraftForge.EVENT_BUS.register(new ProperProjectilePhysics());
        }
        if (FzConfig.buffed_nametags) {
            MinecraftForge.EVENT_BUS.register(new BuffNametags());
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
            try { rockets(ms); } finally { /* What could I say? */ }
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
    

    private boolean launched = false;
    final static int check_time = 20*60;
    int checks = check_time;
    void rockets(MinecraftServer ms) {
        launched = false;
        if (checks-- > 0 || launched) {
            return;
        }
        checks = check_time;
        
        //Test date
        boolean right_day = false;
        if (ms.worldServers == null && ms.worldServers.length > 0) {
            Calendar cal = ms.worldServers[0].getCurrentDate();
            if (cal.get(Calendar.DAY_OF_MONTH) == 21 && cal.get(Calendar.MONTH) == Calendar.DECEMBER) {
                right_day = true;
            }
        }
        if (!right_day) return;
        
        //Test for night time
        World w = ms.getEntityWorld();
        if (w == null) return;
        float angle = w.getCelestialAngle(1);
        if (!(angle > 0.3 && angle < 0.7)) {
            return;
        }
        
        //Get chunks
        List<EntityPlayer> players = (List<EntityPlayer>) w.playerEntities;
        if (players.isEmpty()) {
            return;
        }
        HashSet<Chunk> loadedChunks = new HashSet();
        for (EntityPlayer player : players) {
            int d = 3;
            Coord p = new Coord(player);
            for (int dx = -d; dx <= d; dx++) {
                for (int dz = -d; dz <= d; dz++) {
                    Coord vis = p.add(dx*16, 0, dz*16);
                    if (vis.blockExists()) {
                        loadedChunks.add(vis.getChunk());
                    }
                }
            }
        }
        
        //Make rocket item
        //NORELEASE: Check the fireworks!
        ItemStack red_is = new ItemStack(Items.fireworks);
        NBTTagCompound tag = new NBTTagCompound(); //("Fireworks")
        tag.setByte("Flight", (byte) 3);
        NBTTagList explosions = new NBTTagList(); //("Explosions")
        {
            NBTTagCompound explo = new NBTTagCompound();
            explo.setBoolean("Trail", true);
            explo.setBoolean("Flicker", true);
            int[] colors = new int[] {0xFFFF25};
            explo.setIntArray("Colors", colors);
            explo.setIntArray("FadeColors", colors);
            explosions.appendTag(explo);
        }
        {
            NBTTagCompound explo = new NBTTagCompound();
            explo.setBoolean("Trail", true);
            explo.setBoolean("Flicker", true);
            int[] colors = new int[] {0xFF2525};
            explo.setIntArray("Colors", colors);
            explo.setIntArray("FadeColors", colors);
            explo.setByte("Type", (byte)2);
            explosions.appendTag(explo);
        }
        tag.setTag("Explosions", explosions);
        
        NBTTagCompound wrap = new NBTTagCompound(); //("tag")
        wrap.setTag("Fireworks", tag);
        red_is.setTagCompound(wrap);
        
        //Spawn rockets
        ArrayList<Chunk> chunks = new ArrayList(loadedChunks);
        Collections.shuffle(chunks);
        int rocketCount = 206;
        for (Chunk chunk : chunks) {
            if (w.rand.nextBoolean()) continue;
            if (rocketCount <= 0) break;
            rocketCount--;
            int dx = w.rand.nextInt(16);
            int dz = w.rand.nextInt(16);
            int x = chunk.xPosition*16 + dx;
            int z = chunk.zPosition*16 + dz;
            int y = chunk.getHeightValue(dx, dz);
            
            w.spawnEntityInWorld(new EntityFireworkRocket(w, x, y, z, red_is));
        }
    }
}
