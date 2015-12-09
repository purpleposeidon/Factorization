package factorization.misc;

import factorization.api.Coord;
import factorization.util.SpaceUtil;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;

import java.util.ArrayList;

public class Mushroomalizer {
    Item red_mushroom = new ItemStack(Blocks.red_mushroom_block).getItem();
    Item brown_mushroom = new ItemStack(Blocks.brown_mushroom_block).getItem();
    
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
        if (shroomQueue.isEmpty()) return;
        EnumFacing[] HORIZONTALS = new EnumFacing[] { EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.EAST, EnumFacing.WEST };
        for (PlayerInteractEvent event : shroomQueue) {
            if (event.isCanceled()) continue;
            Coord at = new Coord(event.entityPlayer.worldObj, event.pos).add(event.face);
            if (event.entityPlayer.isSneaking()) {
                // Stalk
                if (event.face.getAxis() == EnumFacing.Axis.Y) {
                    at.setMd(15); // Vertical stalk
                } else {
                    at.setMd(10); // All-sides stalk
                }
                continue;
            }
            Block shroom = at.getBlock();
            int flag = 0;
            for (int i = 0; i < HORIZONTALS.length; i++) {
                EnumFacing fd = HORIZONTALS[i];
                if (at.add(fd).getBlock() == shroom) {
                    flag |= 1 << i;
                }
            }
            int md = 14;
            // Alone & Surounded
            if (flag == 0) md = 14;
            if (flag == 15) {
                md = 0;
                if (at.add(EnumFacing.UP).getBlock() != shroom) {
                    md = 9;
                }
            }
            
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
