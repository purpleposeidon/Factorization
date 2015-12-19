package factorization.truth.minecraft;

import factorization.api.Coord;
import factorization.api.ICoordFunction;
import factorization.common.FzConfig;
import factorization.shared.Core;
import factorization.util.StatUtil;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.AchievementList;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatisticsFile;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;

import java.util.HashSet;

public class DistributeDocs {
    static HashSet<String> needyPlayers = new HashSet<String>();
    static final String guideKey = "fzColossusGuide";
    static StatBase guideGet = new StatBase("factorization.dropcolossusguide", new ChatComponentTranslation("factorization.dropcolossusguide")).registerStat();

    static Item getGivenItem() {
        if (FzConfig.gen_colossi) {
            return Core.registry.colossusGuide;
        } else {
            return Core.registry.logicMatrixProgrammer;
        }
    }
    
    static boolean givenBook(EntityPlayer player) {
        if (!FzConfig.players_discover_colossus_guides) return true;
        StatUtil.FzStat stat = StatUtil.load(player, guideGet);
        return stat.get() > 0 || player.getEntityData().hasKey(guideKey);
    }
    
    static void setGivenBook(EntityPlayer player) {
        if (!FzConfig.players_discover_colossus_guides) return;
        needyPlayers.remove(player.getName());
        StatUtil.load(player, guideGet).add(1);
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
        needyPlayers.add(player.getName());
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
        String name = player.getName();
        if (!needyPlayers.contains(name)) {
            return;
        }
        StatisticsFile sfw = StatUtil.getStatsFile(player);
        if (sfw == null) return;
        if (!sfw.hasAchievementUnlocked(AchievementList.diamonds)) {
            return;
        }
        Item toGive = getGivenItem();

        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack is = player.inventory.getStackInSlot(i);
            if (is == null) continue;
            if (is.getItem() == toGive) {
                Core.logInfo("%s already had an Colossus Guide, so won't give another one", player);
                setGivenBook(player);
                return;
            }
        }
        Coord broke = new Coord(event.world, event.pos);
        if (!safeArea(broke)) return;
        broke.spawnItem(new ItemStack(toGive));
        setGivenBook(player);
        Core.logInfo("Giving %s a colossus guide", name);
    }

    boolean safeArea(Coord at) {
        int r = 2;
        Coord min = at.add(-r, -r, -r);
        Coord max = at.add(+r, +r, +r);
        Checker c = new Checker();
        Coord.iterateCube(min, max, c);
        return c.cool;
    }

    static class Checker implements ICoordFunction {
        boolean cool = true;

        @Override
        public void handle(Coord here) {
            Material mat = here.getBlock().getMaterial();
            if (mat == Material.lava || mat == Material.cactus || mat == Material.fire) cool = false;
        }
    }
}
