package factorization.misc;

import java.util.ArrayList;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;
import factorization.api.Coord;
import factorization.shared.Core;
import factorization.shared.FzUtil;

public class Mushroomalizer {
    Item red_mushroom = new ItemStack(Blocks.red_mushroom_block).getItem();
    Item brown_mushroom = new ItemStack(Blocks.red_mushroom_block).getItem();
    
    ArrayList<PlayerInteractEvent> shroomQueue = new ArrayList();
    
    @SubscribeEvent
    public void shroomalize(PlayerInteractEvent event) {
        //FIXME: A block place event would be nice...
        //Unfortunately it flashes on the client before it gets corrected.
        if (event.world.isRemote) return;
        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) return;
        if (event.entityPlayer == null) return;
        ItemStack is = event.entityPlayer.getHeldItem();
        if (is == null) return;
        if (is.getItem() != red_mushroom && is.getItem() != brown_mushroom) return;
        shroomQueue.add(event);
    }
    
    
    @SubscribeEvent
    public void tickEnd(ServerTickEvent tick) {
        if (tick.phase != Phase.END) return;
        ForgeDirection[] HORIZONTALS = new ForgeDirection[] { ForgeDirection.NORTH, ForgeDirection.SOUTH, ForgeDirection.EAST, ForgeDirection.WEST };
        for (PlayerInteractEvent event : shroomQueue) {
            Coord at = new Coord(event.entityPlayer.worldObj, event.x, event.y, event.z).add(ForgeDirection.getOrientation(event.face));
            if (event.entityPlayer.isSneaking()) {
                if (event.face > 1) {
                    at.setMd(15);
                } else {
                    at.setMd(10);
                }
                continue;
            }
            Block shroom = at.getBlock();
            int flag = 0;
            for (int i = 0; i < HORIZONTALS.length; i++) {
                ForgeDirection fd = HORIZONTALS[i];
                if (at.add(fd).getBlock() == shroom) {
                    flag |= 1 << i;
                }
            }
            int md = 14;
            // Alone & Surounded
            if (flag == 0) md = 14;
            if (flag == 15) md = 0;
            
            // Edges
            if (flag == 13) md = 8;
            if (flag == 14) md = 2;
            if (flag == 7) md = 4;
            if (flag == 11) md = 6;
            
            // Corners
            if (flag == 6) md = 1;
            if (flag == 10) md = 3;
            if (flag == 5) md = 7;
            if (flag == 9) md = 9;
            
            // All other layouts (spikes & bridges) don't have a state.
            at.setMd(md);
        }
        shroomQueue.clear();
    }
}
