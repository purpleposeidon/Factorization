package factorization.misc;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.item.EntityFireworkRocket;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.stats.AchievementList;
import net.minecraft.stats.StatisticsFile;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.DungeonHooks;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import cpw.mods.fml.relauncher.Side;
import factorization.api.Coord;
import factorization.common.FzConfig;
import factorization.shared.Core;

@Mod(modid = MiscellaneousNonsense.modId, name = MiscellaneousNonsense.name, version = Core.version, dependencies = "required-after: " + Core.modId)
public class MiscellaneousNonsense {
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
        // Fixes lack of creeper dungeons
        DungeonHooks.addDungeonMob("Creeper", 1);
        // Etho, of all people, found one. It'd be nice if they were just a bit rarer.
        // Scaling everything else up seems like a poor solution tho.
        @SuppressWarnings("unused")
        String THATS_SOME_VERY_NICE_SOURCE_CODE_YOU_HAVE_THERE[] = {
                "##  ##",
                "##  ##",
                "  ##  ",
                " #### ",
                " #  # "
        };
        
        proxy.initializeClient();
        proxy.registerLoadAlert();
        Core.loadBus(this);
        if (FzConfig.equal_opportunities_for_mobs) {
            Core.loadBus(new MobEqualizer());
        }
        if (FzConfig.embarken_wood) {
            Core.loadBus(new Embarkener());
        }
        if (FzConfig.proper_projectile_physics) {
            Core.loadBus(new ProperProjectilePhysics());
        }
        if (FzConfig.buffed_nametags) {
            Core.loadBus(new BuffNametags());
        }
        if (FzConfig.limit_integrated_server && FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
            Core.loadBus(new TickSynchronizer());
        }
    }
    
    public static class TickSynchronizer {
        /*
         * I'm tired of getting murdered by mobs while my client is frozen.
         */
        long pokeValue = 0;
        long serversLastSeenPoke = 0;
        Minecraft mc = Minecraft.getMinecraft();
        static final boolean enabled = true;
        @SubscribeEvent
        public void serverTick(TickEvent.ServerTickEvent event) {
            if (event.phase == Phase.END) return;
            if (!enabled) return;
            IntegratedServer is = Minecraft.getMinecraft().getIntegratedServer();
            if (is != null) {
                if (is.isServerStopped()) return;
                if (!is.isServerRunning()) return;
            }
            if (pokeValue % 5 != 0 && !isPlayerInDanger(mc.thePlayer)) return;
            
            if (pokeValue != serversLastSeenPoke) {
                serversLastSeenPoke = pokeValue;
                return;
            }
            
            synchronized (this) {
                long originalPoke = pokeValue;
                long maxWaitTime = 1000*1;
                do {
                    try {
                        this.wait(maxWaitTime);
                    } catch (InterruptedException e) {
                        return;
                    }
                } while (originalPoke == pokeValue);
            }
            serversLastSeenPoke = pokeValue;
        }
        
        @SubscribeEvent
        public void clientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == Phase.END) return;
            pokeValue++;
            synchronized (this) {
                this.notifyAll();
            }
        }
        
        static boolean isPlayerInDanger(EntityPlayer player) {
            if (player == null || !player.isEntityAlive()) return false;
            if (player.isBurning() && player.getActivePotionEffect(Potion.fireResistance) == null) return false;
            if (player.hurtTime > 0) return true;
            if (player.getAir() != 300) return true; // 300 from EntityLivingBase.onEntityUpdate; see usages of player.setAir()
            if (player.fallDistance > 1) return true;
            if (player.ticksExisted < 20*10) return true;
            if (player.getFoodStats().getFoodLevel() <= 2) return true;
            if (player.worldObj.getWorldInfo().getVanillaDimension() != 0) return true; // Grrrr....
            for (PotionEffect pot : (Iterable<PotionEffect>) player.getActivePotionEffects()) {
                int id = pot.getPotionID();
                // Any particularly harmful potions
                if (id == Potion.wither.id || id == Potion.poison.id || id == Potion.weakness.id || id == Potion.hunger.id) {
                    return true;
                }
            }
            return false;
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
    @SubscribeEvent
    public void tickServer(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
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
            FMLProxyPacket packet = MiscellaneousNonsense.net.makeTpsReportPacket(getTpsRatio());
            MiscNet.channel.sendToAll(packet);
            last_tps = tps;
        }
    }
    
    @SubscribeEvent
    public void patLagssie(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        LagssieWatchDog.ticks++;
    }
    
    
    @SubscribeEvent
    public void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        MinecraftServer ms = MinecraftServer.getServer();
        {
            // Give the first achievement, because it is stupid and nobody cares.
            // If you're using this mod, you've probably opened your inventory before anyways.
            StatisticsFile sfw = ms.getConfigurationManager().func_148538_i(event.player.getCommandSenderName());
            if (sfw != null && !sfw.hasAchievementUnlocked(AchievementList.openInventory) && FMLCommonHandler.instance().getSide() == Side.CLIENT) {
                sfw.func_150873_a(event.player, AchievementList.openInventory, -1);
                sfw.func_150873_a(event.player, AchievementList.openInventory, 300); // Literally, hundreds of times. :D
                Core.logInfo("Achievement Get! %s, you've opened your inventory hundreds of times already! Yes! You're welcome!", event.player.getCommandSenderName());
            }
        }
        {
            if (ms.getTickCounter() >= ms.tickTimeArray.length) {
                //Startup time is ignored; early birds will get a TPS packet soon enough
                MiscNet.channel.sendTo(MiscNet.makeTpsReportPacket(getTpsRatio()), (EntityPlayerMP) event.player);
            }
        }
        fixReachDistance((EntityPlayerMP)event.player);
    }
    
    @SubscribeEvent
    public void fixReachDistance(PlayerEvent.PlayerRespawnEvent event) {
        fixReachDistance((EntityPlayerMP)event.player);
    }
    
    public void fixReachDistance(EntityPlayerMP player) {
        if (player.worldObj.isRemote) return;
        double old_rd = player.theItemInWorldManager.getBlockReachDistance();
        // Place 7 blocks in a tower. On the edge of the top block, place another block.
        // Place a slab on the ground below it. Look up, try to place a block against the top one.
        double new_rd = old_rd + 1;
        player.theItemInWorldManager.setBlockReachDistance(new_rd);
    }
    
    
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
