package factorization.misc;

import factorization.common.FzConfig;
import factorization.shared.Core;
import factorization.util.FzUtil;
import factorization.util.PlayerUtil;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLeashKnot;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemSword;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.stats.AchievementList;
import net.minecraft.stats.StatisticsFile;
import net.minecraft.util.MathHelper;
import net.minecraftforge.common.DungeonHooks;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.relauncher.Side;

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
        FzUtil.setCoreParent(event);
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
        if (FzConfig.mushroomalize) {
            Core.loadBus(new Mushroomalizer());
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
        if (FzConfig.disable_endermen_griefing) {
            int i = 0;
            for (Block block : (Iterable<Block>) Block.blockRegistry) {
                if (EntityEnderman.getCarriable(block)) {
                    EntityEnderman.setCarriable(block, false);
                    i++;
                }
            }
            Core.logInfo("Endermen griefing disabled for " + i + " blocks.");
        }
        if (FzConfig.blockundo) {
            Core.loadBus(BlockUndo.instance);
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
            // if (player.worldObj.getWorldInfo().getVanillaDimension() != 0) return true; // Grrrr....
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
        if (ms == null) return 1F;
        double ticks_time_ms = MathHelper.average(ms.tickTimeArray)*1.0E-6D;
        return (float) Math.min(expected_tick_time_ms/ticks_time_ms, 1);
    }
    
    private float last_tps = -1;
    private int measurements = 0;
    @SubscribeEvent
    public void tickServer(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
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
        {
            // Give the first achievement, because it is stupid and nobody cares.
            // If you're using this mod, you've probably opened your inventory before anyways.
            StatisticsFile sfw = PlayerUtil.getStatsFile(event.player);
            if (sfw != null && !sfw.hasAchievementUnlocked(AchievementList.openInventory) && FMLCommonHandler.instance().getSide() == Side.CLIENT) {
                sfw.func_150873_a(event.player, AchievementList.openInventory, -1);
                sfw.func_150873_a(event.player, AchievementList.openInventory, 300); // Literally, hundreds of times. :D
                Core.logInfo("Achievement Get! %s, you've opened your inventory hundreds of times already! Yes! You're welcome!", event.player.getCommandSenderName());
            }
        }
        {
            MinecraftServer ms = MinecraftServer.getServer();
            if (ms != null && ms.getTickCounter() >= ms.tickTimeArray.length) {
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
    
    @SubscribeEvent
    public void doTheZorroThing(EntityInteractEvent event) {
        EntityPlayer player = event.entityPlayer;
        if (player.worldObj.isRemote) return;
        if (player.isRiding()) return;
        if (!(event.target instanceof EntityHorse)) return;
        EntityHorse horse = (EntityHorse) event.target;
        if (player.fallDistance <= 2) return;
        if (!horse.isHorseSaddled()) return;
        if (horse.getLeashed()) {
            if (!(horse.getLeashedToEntity() instanceof EntityLeashKnot)) return;
            horse.getLeashedToEntity().interactFirst(player);
        }
        boolean awesome = false;
        if (player.fallDistance > 5 && player.getHeldItem() != null) {
            Item held = player.getHeldItem().getItem();
            boolean has_baby = false;
            if (player.riddenByEntity instanceof EntityAgeable) {
                EntityAgeable ea = (EntityAgeable) player.riddenByEntity;
                has_baby = ea.isChild();
            }
            awesome = held instanceof ItemSword || held instanceof ItemAxe || held instanceof ItemBow || player.riddenByEntity instanceof EntityPlayer || has_baby;
        }
        if (awesome) {
            horse.addPotionEffect(new PotionEffect(Potion.moveSpeed.id, 20 * 40, 2, false, false));
            horse.addPotionEffect(new PotionEffect(Potion.resistance.id, 20 * 40, 1, true, true));
            horse.addPotionEffect(new PotionEffect(Potion.jump.id, 20 * 40, 1, true, true));
        } else {
            horse.addPotionEffect(new PotionEffect(Potion.moveSpeed.id, 20 * 8, 1, false, false));
        }
        horse.playLivingSound();
    }
    
    
    public static void lag() {
        try {
            Thread.sleep(1000 / 10);
        } catch (InterruptedException e) { }
    }
    
    @EventHandler
    public void registerCommands(FMLServerStartingEvent event) {
        event.registerServerCommand(new MC16009());
        event.registerServerCommand(new Deglitch());
        event.registerServerCommand(new SafeCommandHelp());
    }
}
