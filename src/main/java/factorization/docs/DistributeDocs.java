package factorization.docs;

import java.util.HashSet;
import java.util.Random;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.AchievementList;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatisticsFile;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.world.BlockEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import factorization.api.Coord;
import factorization.shared.Core;
import factorization.shared.FzUtil;
import factorization.shared.FzUtil.FzInv;

public class DistributeDocs {
    static HashSet<String> needyPlayers = new HashSet();
    
    static boolean givenBook(EntityPlayer player) {
        StatisticsFile statsFile = FzUtil.getStatsFile(player);
        return (statsFile != null && statsFile.writeStat(bookGet) > 0) || player.getEntityData().hasKey("fzDocd");
    }
    
    static void setGivenBook(EntityPlayer player) {
        needyPlayers.remove(player.getCommandSenderName());
        StatisticsFile statsFile = FzUtil.getStatsFile(player);
        if (statsFile != null) {
            statsFile.func_150873_a(player, bookGet, 1);
        }
        player.getEntityData().setBoolean("fzDocd", true);
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
    
    Random rand = new Random();
    static StatBase bookGet = new StatBase("factorization.dropdocbook", new ChatComponentTranslation("factorization.dropdocbook")).registerStat();
    
    
    @SubscribeEvent
    public void breakBlock(BlockEvent.BreakEvent event) {
        if (rand.nextInt(32) != 0 && !Core.dev_environ) return;
        EntityPlayer ply = event.getPlayer();
        if (!(ply instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) ply;
        if (player.theItemInWorldManager.getGameType() != WorldSettings.GameType.SURVIVAL) return;
        String name = player.getCommandSenderName();
        if (!needyPlayers.contains(name)) {
            return;
        }
        StatisticsFile sfw = FzUtil.getStatsFile(player);
        if (sfw == null) return;
        if (!sfw.hasAchievementUnlocked(AchievementList.acquireIron)) {
            return;
        }
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack is = player.inventory.getStackInSlot(i);
            if (is == null) continue;
            if (is.getItem() == Core.registry.docbook) {
                Core.logInfo("%s already had an FzDocBook, so won't give another one");
                setGivenBook(player);
                return;
            }
        }
        Coord broke = new Coord(event.world, event.x, event.y, event.z);
        for (ForgeDirection fd : ForgeDirection.VALID_DIRECTIONS) {
            if (fd.offsetY != 0) continue;
            broke.adjust(fd);
            boolean cool = isCoolPlace(broke, broke);
            broke.adjust(fd.getOpposite());
            if (cool) {
                Core.logInfo("Giving %s a book", name);
                broke.adjust(fd);
                spawnAt(broke);
                setGivenBook(player);
                return;
            }
        }
    }
    
    boolean isCoolPlace(Coord at, Coord orig) {
        if (!at.isSolid()) return false;
        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            Coord c = at.add(dir);
            if (!c.isSolid()) {
                if (!c.equals(orig)) return false;
            }
        }
        return at.getBlock().isReplaceableOreGen(at.w, at.x, at.y, at.z, Blocks.stone);
    }
    
    void spawnAt(Coord at) {
        at.setIdMd(Blocks.dropper, rand.nextInt(6), true);
        FzInv inv = FzUtil.openInventory(at.getTE(IInventory.class), ForgeDirection.UP);
        if (inv == null) return;
        int target = inv.size()/2;
        inv.set(target, new ItemStack(Core.registry.docbook));
    }
}
