package factorization.artifact;

import java.util.Calendar;
import java.util.Locale;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatisticsFile;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import factorization.api.Coord;
import factorization.shared.Core;
import factorization.shared.Sound;
import factorization.util.PlayerUtil;
import factorization.util.StatUtil;

public class InspirationManager {
    static InspirationManager instance;
    static final StatBase lastArtifact = new StatBase("factorization.artifact.last", new ChatComponentTranslation("factorization.artifact.last.name")).registerStat();
    static final StatBase beenNotified = new StatBase("factorization.artifact.notified", new ChatComponentTranslation("factorization.artifact.notify.name")).registerStat();
    static final boolean DEBUG = false;
    private static final int days_per_artifact = DEBUG ? 1 : 30;
    private static final int check_tick_rate = DEBUG ? 20 : 20 * 60 * 15;

    public static void init( ){
        instance = new InspirationManager();
        Core.loadBus(instance);
    }


    static int today() {
        final Calendar cal = Calendar.getInstance();
        if (DEBUG) {
            return cal.get(Calendar.DAY_OF_YEAR) * 60 * 60 * 365 + cal.get(Calendar.HOUR) * 60 * 60 + cal.get(Calendar.MINUTE) * 60;
        }
        return cal.get(Calendar.DAY_OF_YEAR) + cal.get(Calendar.YEAR) * 365; // Close enough?
    }

    static boolean canMakeArtifact(EntityPlayer player) {
        if (PlayerUtil.isPlayerCreative(player)) return true;
        int lastArtifactDay = StatUtil.load(player, lastArtifact).get();
        if (lastArtifactDay + days_per_artifact > today()) return false;
        return true;
    }

    public static final ChatStyle aqua = new ChatStyle().setColor(EnumChatFormatting.AQUA);

    public void poke(EntityPlayer player, boolean isLogin) {
        if (PlayerUtil.isPlayerCreative(player)) return;
        if (!canMakeArtifact(player)) return;
        int lastNoticeSent = StatUtil.load(player, beenNotified).get();
        boolean update = false;
        if (lastNoticeSent <= 0) {
            player.addChatMessage(new ChatComponentTranslation("factorization.artifact.cancreate").setChatStyle(aqua));
            update = true;
        }
        final int today = today();
        if (lastNoticeSent != today && isLogin) {
            player.addChatMessage(new ChatComponentTranslation("factorization.artifact.canstillcreate").setChatStyle(aqua));
            update = true;
        }
        if (update) {
            StatUtil.load(player, beenNotified).set(today);
        }

    }

    public static void resetArtifactDelay(EntityPlayer player) {
        StatUtil.load(player, lastArtifact).set(today());
        StatUtil.load(player, beenNotified).set(0);
    }

    @SubscribeEvent
    public void login(PlayerEvent.PlayerLoggedInEvent event) {
        EntityPlayer player = event.player;
        StatisticsFile statsFile = StatUtil.getStatsFile(player);
        if (statsFile == null) return;
        if (StatUtil.load(player, lastArtifact).get() <= 0) {
            resetArtifactDelay(player);
        } else {
            poke(player, true);
        }
    }

    int ticks = 0;
    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (ticks++ < check_tick_rate) return;
        ticks = 0;
        for (Object obj : MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
            if (!(obj instanceof EntityPlayer)) continue;
            EntityPlayer player = (EntityPlayer) obj;
            poke(player, false);
        }
    }

    public static void makeArtifact(EntityPlayer player, ItemStack artifact) {
        String name = player.getName();
        String key = "factorization.artifact.announce";
        if (name.toLowerCase(Locale.ROOT).startsWith("urist")) {
            key += ".urist";
        }
        String artifactName = artifact.getDisplayName();
        String toolName = artifact.getUnlocalizedName() + ".name";
        IChatComponent msg = new ChatComponentTranslation(key, name, artifactName, new ChatComponentTranslation(toolName));
        msg = msg.setChatStyle(aqua);
        MinecraftServer.getServer().getConfigurationManager().sendChatMsgImpl(msg, false);
        resetArtifactDelay(player);
        for (Object obj : MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
            if (!(obj instanceof EntityPlayer)) continue;
            EntityPlayer peep = (EntityPlayer) obj;
            Sound.artifactForged.playAt(new Coord(peep));
        }
    }
}
