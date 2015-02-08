package factorization.docs;

import java.util.HashSet;

import factorization.util.PlayerUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.AchievementList;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatisticsFile;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.world.BlockEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import factorization.api.Coord;
import factorization.common.FzConfig;
import factorization.shared.Core;

public class DistributeDocs {
    static HashSet<String> needyPlayers = new HashSet();
    static final String guideKey = "fzColossusGuide";
    static StatBase guideGet = new StatBase("factorization.dropcolossusguide", new ChatComponentTranslation("factorization.dropcolossusguide")).registerStat();
    
    static boolean givenBook(EntityPlayer player) {
        if (!FzConfig.players_discover_colossus_guides) return true;
        StatisticsFile statsFile = PlayerUtil.getStatsFile(player);
        return (statsFile != null && statsFile.writeStat(guideGet) > 0) || player.getEntityData().hasKey(guideKey);
    }
    
    static void setGivenBook(EntityPlayer player) {
        if (!FzConfig.players_discover_colossus_guides) return;
        needyPlayers.remove(player.getCommandSenderName());
        StatisticsFile statsFile = PlayerUtil.getStatsFile(player);
        if (statsFile != null) {
            statsFile.func_150873_a(player, guideGet, 1);
        }
        player.getEntityData().setBoolean(guideKey, true);
    }
    
    @SubscribeEvent
    public void onPlayerLogon(PlayerLoggedInEvent event) {
        if (givenBook(event.player)) {
            setGivenBook(event.player);
            return;
        }
        if (!(event.player instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        needyPlayers.add(player.getCommandSenderName());
    }
    
    @SubscribeEvent
    public void breakBlock(BlockEvent.BreakEvent event) {
        if (event.world.rand.nextInt(32) != 0 && !Core.dev_environ) return;
        EntityPlayer ply = event.getPlayer();
        if (!(ply instanceof EntityPlayerMP) || ply instanceof FakePlayer) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) ply;
        if (player.theItemInWorldManager.getGameType() == WorldSettings.GameType.CREATIVE) return;
        String name = player.getCommandSenderName();
        if (!needyPlayers.contains(name)) {
            return;
        }
        StatisticsFile sfw = PlayerUtil.getStatsFile(player);
        if (sfw == null) return;
        if (!sfw.hasAchievementUnlocked(AchievementList.diamonds)) {
            return;
        }
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack is = player.inventory.getStackInSlot(i);
            if (is == null) continue;
            if (is.getItem() == Core.registry.colossusGuide) {
                Core.logInfo("%s already had an Colossus Guide, so won't give another one", player);
                setGivenBook(player);
                return;
            }
        }
        Coord broke = new Coord(event.world, event.x, event.y, event.z);
        broke.spawnItem(new ItemStack(Core.registry.colossusGuide));
        setGivenBook(player);
        Core.logInfo("Giving %s a colossus guide", name);
    }
}
