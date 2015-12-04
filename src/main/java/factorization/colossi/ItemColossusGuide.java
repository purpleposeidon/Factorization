package factorization.colossi;

import factorization.api.Coord;
import factorization.api.DeltaCoord;
import factorization.common.FzConfig;
import factorization.notify.Notice;
import factorization.shared.Core;
import factorization.shared.Core.TabType;
import factorization.shared.ItemFactorization;
import factorization.util.PlayerUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;

import java.util.ArrayList;

public class ItemColossusGuide extends ItemFactorization {

    public ItemColossusGuide(String name, TabType tabType) {
        super(name, tabType);
        Core.loadBus(this);
        setMaxStackSize(1);
        setMaxDamage(24);
    }
    
    @Override
    public ItemStack onItemRightClick(ItemStack is, World world, EntityPlayer player) {
        if (world.isRemote) return is;
        if (player instanceof FakePlayer) return is;
        
        int msgKey = 888888888;
        if (!WorldGenColossus.genOnWorld(world)) {
            Notice.chat(player, msgKey, new ChatComponentTranslation("colossus.is.impossible"));
            return is;
        }
        
        int range = WorldGenColossus.GENERATION_SPACING * 3 / 2;
        if (MinecraftServer.getServer().getCurrentPlayerCount() == 1) {
            range = WorldGenColossus.GENERATION_SPACING * 5 / 2;
        }
        Coord playerPos = new Coord(player);
        ArrayList<Coord> nearby;
        try {
            nearby = WorldGenColossus.getCandidatesNear(playerPos, range, true);
        } catch (WorldGenColossus.LocationException e) {
            Notice.chat(player, msgKey, new ChatComponentTranslation("colossus.is.worldgen_crash"));
            return is;
        }
        if (nearby.isEmpty()) {
            Notice.chat(player, msgKey, new ChatComponentTranslation("colossus.is.no_nearby"));
            return is;
        }
        if (PlayerUtil.isPlayerCreative(player) && player.isSneaking()) {
            int limit = 4;
            for (Coord at : nearby) {
                String t = at.toString();
                t = at.getChunk().isTerrainPopulated + " " + t;
                player.addChatComponentMessage(new ChatComponentText(t));
                if (limit-- <= 0) break;
            }
            player.addChatComponentMessage(new ChatComponentTranslation("colossus.is.creativeFound", nearby.size()));
        } else {
            Coord at = nearby.get(0);
            at.adjust(EnumFacing.EAST); // The heart is positioned behind a cracked block
            DeltaCoord dc = at.difference(playerPos);
            IChatComponent msg;
            if (dc.x == 0 && dc.z == 0) {
                String m;
                if (dc.y > 0) {
                    m = "colossus.is.above";
                } else if (dc.y < 0) {
                    m = "colossus.is.below";
                } else {
                    m = "colossus.is.mineit";
                }
                msg = new ChatComponentTranslation(m);
            } else {
                dc.y = 0;
                msg = getCompass(dc);
            }
            Notice.chat(player, msgKey, msg);
            
            if (!FzConfig.infinite_guide_usage) is.damageItem(1, player);
        }
        
        return is;
    }

    IChatComponent getCompass(DeltaCoord dc) {
        int d = (int) dc.magnitude();
        String pretty = "" + d;
        String unit = ".m";
        if (d > 1000) {
            pretty = String.format("%.1f", d / 1000F);
            unit = ".km";
        }
        return new ChatComponentTranslation(getDirection(dc), pretty, new ChatComponentTranslation("colossus.compass.unit" +unit));
    }
    
    String getDirection(DeltaCoord dc) {
        double angle = Math.toDegrees(dc.getAngleHorizontal());
        angle = (angle + 360) % 360;
        // The circle is divided into 8 parts.
        angle += 360. / 8. / 2.; // Add half a piece to turn
        angle %= 360; // and then wrap
        int a = (int) (angle * 8. / 360.);
        return "colossus.compass." + a;
    }

}
