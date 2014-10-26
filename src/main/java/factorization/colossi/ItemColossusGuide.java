package factorization.colossi;

import java.util.ArrayList;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import factorization.api.Coord;
import factorization.shared.Core;
import factorization.shared.Core.TabType;
import factorization.shared.ItemFactorization;

public class ItemColossusGuide extends ItemFactorization {

    public ItemColossusGuide(String name, TabType tabType) {
        super(name, tabType);
        Core.loadBus(this);
    }
    
    @Override
    public ItemStack onItemRightClick(ItemStack is, World world, EntityPlayer player) {
        if (world.isRemote) return is;
        if (player instanceof FakePlayer) return is;
        
        int range = WorldGenColossus.GENERATION_SPACING * 3 / 2;
        if (MinecraftServer.getServer().getCurrentPlayerCount() == 1) {
            range = WorldGenColossus.GENERATION_SPACING * 3;
        }
        ArrayList<Coord> nearby = WorldGenColossus.getCandidatesNear(new Coord(player), range, true);
        if (nearby.isEmpty()) {
            player.addChatComponentMessage(new ChatComponentTranslation("colossus.no_nearby"));
            return is;
        }
        int limit = 4;
        for (Coord at : nearby) {
            String t = at.toString();
            t = at.getChunk().isTerrainPopulated + " " + t;
            player.addChatComponentMessage(new ChatComponentText(t));
            if (limit-- <= 0) break;
        }
        player.addChatComponentMessage(new ChatComponentText("Found: " + nearby.size()));
        
        return is;
    }

}
