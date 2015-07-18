package factorization.misc;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import factorization.util.DataUtil;
import factorization.util.FzUtil;
import factorization.util.PlayerUtil;
import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.BlockEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.WeakHashMap;

public class HotBlocks {
    public static final HotBlocks instance = new HotBlocks();
    private HotBlocks() { }

    private static class HotBlock {
        final int w, x, y, z, idmd;

        private HotBlock(int w, int x, int y, int z, int idmd) {
            this.w = w;
            this.x = x;
            this.y = y;
            this.z = z;
            this.idmd = idmd;
        }
    }

    public static int HOT_BLOCK_COUNT = 5;
    public static float MAX_TRUE_SPEED_STANDARD = 0.25F;
    public static float MAX_TRUE_SPEED_TILEENTITY = 0.125F;

    WeakHashMap<EntityPlayer, ArrayList<HotBlock>> hots = new WeakHashMap<EntityPlayer, ArrayList<HotBlock>>();

    @SubscribeEvent(priority = EventPriority.LOWEST) // Act after any cancellations
    public void heatBlock(BlockEvent.PlaceEvent event) {
        if (event.player instanceof FakePlayer) return;
        if (event.block.getBlockHardness(event.world, event.x, event.y, event.z) <= 0F) return;
        if (PlayerUtil.isPlayerCreative(event.player)) return;
        ArrayList<HotBlock> coords;
        if (!hots.containsKey(event.player)) {
            hots.put(event.player, coords = new ArrayList<HotBlock>());
        } else {
            coords = hots.get(event.player);
        }
        int idmd = (DataUtil.getId(event.block) << 4) /*+ event.blockMetadata*/;
        coords.add(new HotBlock(FzUtil.getWorldDimension(event.world), event.x, event.y, event.z, idmd));
        if (coords.size() > HOT_BLOCK_COUNT) {
            coords.remove(0);
        }
    }

    private ThreadLocal<Boolean> working = new ThreadLocal<Boolean>();
    @SubscribeEvent
    public void boostBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (working.get() != null) {
            return;
        }
        working.set(true);
        try {
            determineBreakSpeed(event);
        } finally {
            working.remove();
        }
    }

    private void determineBreakSpeed(PlayerEvent.BreakSpeed event) {
        final int y = event.y;
        if (y == -1) return; // Event specifies that 'y' might be -1 for unknown usage?
        final Block block = event.block;
        final int md = event.metadata;
        final int x = event.x;
        final int z = event.z;
        if (!isHot(event, x, y, z, block, md)) return;
        // Duplicate logic to figure out what the *actual* break speed will be, so that we don't make this actual break speed too fast
        final EntityPlayer player = event.entityPlayer;
        float hardness = block.getBlockHardness(player.worldObj, x, y, z);
        if (hardness < 0.0F) {
            // Block is invulnerable
            return;
        }
        final float harvestingSpeed = ForgeHooks.canHarvestBlock(block, player, md) ? 30F : 100F;
        final float max_true_speed = block.hasTileEntity(md) ? MAX_TRUE_SPEED_TILEENTITY : MAX_TRUE_SPEED_STANDARD;
        float true_speed = event.newSpeed / hardness / harvestingSpeed;
        if (true_speed > max_true_speed) return;
        float boost = max_true_speed * hardness * harvestingSpeed;
        event.newSpeed = Math.max(event.newSpeed * boost, event.newSpeed);
    }

    private boolean isHot(PlayerEvent event, int x, int y, int z, Block block, int metadata) {
        final EntityPlayer player = event.entityPlayer;
        ArrayList<HotBlock> coords = hots.get(player);
        if (coords == null) return false;

        int w = FzUtil.getWorldDimension(player.worldObj);
        for (HotBlock hot : coords) {
            if (hot.w == w && hot.x == x && hot.y == y && hot.z == z) {
                int idmd = (DataUtil.getId(block) << 4) /*+ metadata*/;
                if (idmd != hot.idmd) {
                    continue;
                }
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent(priority = EventPriority.HIGH) // Cancel before most things, but permission-handlers can cancel before us
    public void playerRemovedBlock(BlockEvent.BreakEvent event) {
        ArrayList<HotBlock> coords = hots.get(event.getPlayer());
        if (coords == null) return;
        HotBlock heat = null;
        final World w = event.world;
        final int x = event.x;
        final int y = event.y;
        final int z = event.z;
        final Block block = event.block;
        final int md = event.blockMetadata;
        int wDim = FzUtil.getWorldDimension(w);
        EntityPlayer thePlayer = event.getPlayer();
        if (PlayerUtil.isPlayerCreative(thePlayer)) return;
        for (Iterator<HotBlock> iterator = coords.iterator(); iterator.hasNext(); ) {
            HotBlock hot = iterator.next();
            if (hot.w == wDim && hot.x == x && hot.y == y && hot.z == z) {
                heat = hot;
                iterator.remove();
                break;
            }
        }
        if (heat != null && thePlayer instanceof EntityPlayerMP) {
            event.setCanceled(true);
            EntityPlayerMP player = (EntityPlayerMP) thePlayer;
            ItemStack real_held = player.getHeldItem();
            ItemStack tool = new ItemStack(Items.diamond_pickaxe);
            tool.setItemDamage(tool.getMaxDamage() - 1);
            tool.addEnchantment(Enchantment.silkTouch, 1);
            player.setCurrentItemOrArmor(0, tool);
            {
                block.onBlockHarvested(w, x, y, z, md, player);
                boolean canDestroy = block.removedByPlayer(w, player, x, y, z, true);

                if (canDestroy) {
                    block.onBlockDestroyedByPlayer(w, x, y, z, md);
                }
                block.harvestBlock(w, event.getPlayer(), x, y, z, md);
                if (canDestroy) {
                    int xp = block.getExpDrop(w, md, 0);
                    block.dropXpOnBlockBreak(w, x, y, z, xp);
                }
            }
            tool.stackSize = 0;
            player.setCurrentItemOrArmor(0, real_held);
        }
    }

}
