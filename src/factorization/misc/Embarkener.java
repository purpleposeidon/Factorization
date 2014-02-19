package factorization.misc;

import java.util.ArrayList;
import java.util.EnumSet;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.block.BlockLog;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;
import factorization.api.Coord;
import factorization.common.FzConfig;
import factorization.shared.Core;

public class Embarkener implements ITickHandler {
    public Embarkener() {
        if (FzConfig.embarken_wood) {
            addLogBarkRecipes();
            MinecraftForge.EVENT_BUS.register(this);
            TickRegistry.registerTickHandler(this, Side.SERVER);
        }
    }

    int wood_rendertype = Blocks.wood.getRenderType();
    boolean isWoodish(Block block) {
        if (block == null) return false;
        return block.blockMaterial == Material.wood && block instanceof BlockLog && block.getRenderType() == wood_rendertype;
    }
    
    void addLogBarkRecipes() {
        int count = 0;
        for (Block block : Blocks.blocksList) {
            if (isWoodish(block)) {
                for (int md = 0; md < 4; md++) {
                    count++;
                    ItemStack log = new ItemStack(block, 1, md);
                    ItemStack barked = new ItemStack(block, 4, md | 0xC);
                    Core.registry.shapelessRecipe(barked, log, log, log, log);
                }
            }
        }
        Core.logInfo("Added %s 'barking' recipes for blocks that look sort of like wood logs; this can be disabled in the config file.", count);
    }
    
    ArrayList<EmbarkenEvent> embarkenQueue = new ArrayList(); // Not LinkedList; won't have > 10 events per tick.
    
    static class EmbarkenEvent {
        Coord target;
        int orig_stacksize;
        ItemStack stack;
        EntityPlayer player;
        BlockLog expectedBlock;
        
        public EmbarkenEvent(Coord target, int orig_stacksize, ItemStack stack, EntityPlayer player, BlockLog expectedBlock) {
            this.target = target;
            this.orig_stacksize = orig_stacksize;
            this.stack = stack;
            this.player = player;
            this.expectedBlock = expectedBlock;
        }
        
        void handle() {
            if (stack.stackSize >= orig_stacksize) return;
            if (player.isDead) return;
            if (!target.blockExists()) return;
            if (target.getBlock() != expectedBlock) return;
            target.setMd(target.getMd() | 0xC);
        }
    }
    
    @ForgeSubscribe
    public void enbarkenWood(PlayerInteractEvent event) {
        //FIXME: A block place event would be nice...
        //Unfortunately it flashes on the client before it gets corrected.
        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) return;
        if (event.entityPlayer == null) return;
        ItemStack is = event.entityPlayer.getHeldItem();
        if (is == null) return;
        if (is.itemID >= Blocks.blocksList.length) return;
        if ((is.getItemDamage() & 0xC) != 0xC) return;
        if (!(is.getItem() instanceof ItemBlock)) return;
        Block theBlock = Blocks.blocksList[is.itemID];
        if (!isWoodish(theBlock)) return;
        Coord target = new Coord(event.entityPlayer.worldObj, event.x, event.y, event.z);
        target.adjust(ForgeDirection.getOrientation(event.face));
        if (!target.isReplacable()) return;
        embarkenQueue.add(new EmbarkenEvent(target, is.stackSize, is, event.entityPlayer, (BlockLog) theBlock));
    }

    @Override
    public void tickStart(EnumSet<TickType> type, Object... tickData) {}

    @Override
    public void tickEnd(EnumSet<TickType> type, Object... tickData) {
        for (EmbarkenEvent e : embarkenQueue) {
            e.handle();
        }
        embarkenQueue.clear();
    }

    EnumSet<TickType> myTicks = EnumSet.of(TickType.SERVER);
    
    @Override
    public EnumSet<TickType> ticks() {
        return myTicks;
    }

    @Override
    public String getLabel() {
        return "fz.misc.embarken";
    }
    
    
}
