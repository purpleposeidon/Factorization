package factorization.misc;

import factorization.api.Coord;
import factorization.common.FzConfig;
import factorization.util.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.FMLEventChannel;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BlockUndo {
    public static final String channelName = "FZ|blockundo";
    public static final FMLEventChannel channel = NetworkRegistry.INSTANCE.newEventDrivenChannel(channelName);
    public static final BlockUndo instance = new BlockUndo();
    private BlockUndo() {
        channel.register(this);
    }

    private static void send(EntityPlayer player, PlacedBlock at) {
        ByteBuf payload = Unpooled.buffer();
        at.write(payload);
        FMLProxyPacket packet = new FMLProxyPacket(new PacketBuffer(payload), channelName);
        channel.sendTo(packet, (EntityPlayerMP) player);
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void addBlockUndo(FMLNetworkEvent.ClientCustomPacketEvent event) {
        PlacedBlock at = PlacedBlock.read(event.packet.payload());
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        markPlacement(player, at);
    }

    private static class PlacedBlock {
        final int w, idmd;
        final BlockPos pos;
        final ItemStack orig;

        private PlacedBlock(int w, BlockPos pos, int idmd, ItemStack orig) {
            this.w = w;
            this.pos = pos;
            this.idmd = idmd;
            this.orig = orig;
        }

        @Override
        public String toString() {
            return pos.toString();
        }

        void write(ByteBuf out) {
            out.writeInt(w);
            out.writeInt(pos.getX());
            out.writeInt(pos.getY());
            out.writeInt(pos.getZ());
            out.writeInt(idmd);
            ByteBufUtils.writeItemStack(out, orig);
        }

        static PlacedBlock read(ByteBuf in) {
            return new PlacedBlock(in.readInt(), new BlockPos(in.readInt(), in.readInt(), in.readInt()), in.readInt(),
                    ByteBufUtils.readItemStack(in));
        }
    }

    public static int UNDO_MAX = 6;
    public static float MAX_TRUE_SPEED_STANDARD = 0.25F / 2;
    public static float MAX_TRUE_SPEED_TILEENTITY = 0.125F / 2;
    public static float ANTI_WARP_SPEED = 64;

    final ConcurrentHashMap<String, List<PlacedBlock>> recentlyPlaced = new ConcurrentHashMap<String, List<PlacedBlock>>();

    private static String getName(EntityPlayer player) {
        return player.getName() + " #" + player.worldObj.isRemote;
    }

    private static ItemStack toItem(Block b, World w, BlockPos pos, IBlockState bs) {
        for (ItemStack is : b.getDrops(w, pos, bs, 0)) {
            return is;
        }
        return null;
    }

    void markPlacement(EntityPlayer player, PlacedBlock at) {
        final List<PlacedBlock> coords;
        String playerName = getName(player);
        if (!recentlyPlaced.containsKey(playerName)) {
            coords = Collections.synchronizedList(new ArrayList<PlacedBlock>());
            recentlyPlaced.put(playerName, coords);
        } else {
            coords = recentlyPlaced.get(playerName);
        }
        synchronized (recentlyPlaced) {
            synchronized (coords) {
                for (Iterator<PlacedBlock> it = coords.iterator(); it.hasNext(); ) {
                    PlacedBlock c = it.next();
                    World w = DimensionManager.getWorld(c.w);
                    if (w == null || w.isAirBlock(c.pos)) {
                        it.remove();
                    } else if (c.pos.equals(at.pos)) {
                        it.remove();
                    }
                }
                coords.add(at);
            }
            if (coords.size() > UNDO_MAX) {
                coords.remove(0);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST) // Act after any cancellations
    public void recordBlock(BlockEvent.PlaceEvent event) {
        IBlockState bs = event.placedBlock;
        Block block = bs.getBlock();
        if (event.player == null) return;
        if (event.world.isRemote) return;
        if (event.player instanceof FakePlayer) return;
        if (block.getBlockHardness(event.world, event.pos) <= 0F) return;
        if (PlayerUtil.isPlayerCreative(event.player)) return;

        int idmd = hash(bs);
        final ItemStack theItem = toItem(block, event.world, event.pos, bs);
        final PlacedBlock at = new PlacedBlock(FzUtil.getWorldDimension(event.world), event.pos, idmd, theItem);
        markPlacement(event.player, at);
        if (event.player instanceof EntityPlayerMP) {
            send(event.player, at);
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

    private Map<Integer, Long> playerBreakage = new ConcurrentHashMap<Integer, Long>();
    private boolean stillBusy(EntityPlayer player) {
        Integer code = player.hashCode();
        Long last = playerBreakage.get(code);
        if (last == null) last = -1000L;
        final long end = player.worldObj.getTotalWorldTime() + 8;
        return last > end;
    }

    private void markBusy(EntityPlayer player) {
        Integer code = player.hashCode();
        playerBreakage.put(code, player.worldObj.getTotalWorldTime());
    }


    private void determineBreakSpeed(PlayerEvent.BreakSpeed event) {
        final int y = event.pos.getY();
        if (y == -1) return; // Event specifies that 'y' might be -1 for unknown usage?
        final IBlockState bs = event.state;
        final Block block = bs.getBlock();
        final int x = event.pos.getX();
        final int z = event.pos.getZ();
        final EntityPlayer player = event.entityPlayer;
        if (stillBusy(player)) return;
        if (!canUndo(event, event.pos, block, bs)) return;
        // Duplicate logic to figure out what the *actual* break speed will be, so that we don't make this actual break speed too fast
        float hardness = block.getBlockHardness(player.worldObj, event.pos);
        if (hardness < 0.0F) {
            // Block is invulnerable
            return;
        }
        String heldName = DataUtil.getName(player.getHeldItem());
        if (heldName == null) heldName = "";
        if (heldName.startsWith("TConstruct:")) {
            return; // avoid warp-speed issues
        }
        final float harvestingSpeed = ForgeHooks.canHarvestBlock(block, player, player.worldObj, event.pos) ? 30F : 100F;
        final float max_true_speed = block.hasTileEntity(bs) ? MAX_TRUE_SPEED_TILEENTITY : MAX_TRUE_SPEED_STANDARD;
        float true_speed = event.newSpeed / hardness / harvestingSpeed;
        if (true_speed > max_true_speed) return;
        float boost = max_true_speed * hardness * harvestingSpeed;
        event.newSpeed = Math.max(event.newSpeed * boost, event.newSpeed);
        event.newSpeed = Math.min(ANTI_WARP_SPEED, event.newSpeed);
        // ... this code is wrong. It's suuuper fast for enderchests. Everything too complicated?
        // Maybe just a single blind speed, and be done with it?
    }

    private boolean canUndo(PlayerEvent event, BlockPos pos, Block block, IBlockState bs) {
        final EntityPlayer player = event.entityPlayer;
        List<PlacedBlock> coords = recentlyPlaced.get(getName(player));
        if (coords == null) return false;

        int w = FzUtil.getWorldDimension(player.worldObj);
        for (PlacedBlock hot : coords) {
            if (hot.w == w && hot.pos.equals(pos)) {
                if (hash(bs) != hot.idmd) {
                    continue;
                }
                return true;
            }
        }
        return false;
    }

    static int hash(IBlockState bs) {
        return DataUtil.getId(bs.getBlock());
    }

    @SubscribeEvent(priority = EventPriority.HIGH) // Cancel before most things, but permission-handlers can cancel before us
    public void playerRemovedBlock(BlockEvent.BreakEvent event) {
        EntityPlayer thePlayer = event.getPlayer();
        markBusy(thePlayer);
        List<PlacedBlock> coords = recentlyPlaced.get(getName(thePlayer));
        if (coords == null) return;
        PlacedBlock heat = null;
        final World w = event.world;
        final int x = event.pos.getX();
        final int y = event.pos.getY();
        final int z = event.pos.getZ();
        final BlockPos pos = event.pos;
        final IBlockState bs = event.state;
        final Block block = event.state.getBlock();
        final TileEntity te = w.getTileEntity(pos);
        int wDim = FzUtil.getWorldDimension(w);
        if (PlayerUtil.isPlayerCreative(thePlayer)) return;
        for (Iterator<PlacedBlock> iterator = coords.iterator(); iterator.hasNext(); ) {
            PlacedBlock hot = iterator.next();
            if (hot.w == wDim && hot.pos.equals(event.pos)) {
                heat = hot;
                iterator.remove();
                break;
            }
        }
        if (heat == null || !(thePlayer instanceof EntityPlayerMP)) {
            return;
        }
        if (!ItemUtil.identical(heat.orig, toItem(block, w, pos, bs))) {
            return;
        }
        EntityPlayerMP real_player = (EntityPlayerMP) thePlayer;
        final ItemStack heldItem = real_player.getHeldItem();
        String heldName = DataUtil.getName(heldItem);
        if (heldName == null) heldName = "";
        if (heldName.startsWith("TConstruct:")) {
            return; // avoid warp-speed issues
        }
        if (ForgeHooks.canToolHarvestBlock(w, pos, heldItem)) {
            return;
        }
        Item blockDrops = block.getItemDropped(bs, thePlayer.worldObj.rand, 0);
        Item blocksItem = DataUtil.getItem(block); // banners: this can be null
        if (blockDrops != blocksItem) {
            if (!real_player.isSneaking() && block.getBlockHardness(w, pos) < 1) {
                return;
            }
        }
        String harvestTool = block.getHarvestTool(bs);
        int harvestLevel = block.getHarvestLevel(bs);
        Item harvester = findAppropriateTool(harvestTool, harvestLevel);
        if (harvester == null) return;
        event.setCanceled(true);
        ItemStack tool = new ItemStack(harvester);
        tool.setItemDamage(tool.getMaxDamage());
        tool.addEnchantment(Enchantment.silkTouch, 1);
        tool.stackSize = 0;
        EntityPlayer fake_player = PlayerUtil.makePlayer(new Coord(w, pos), "BlockUndo");
        fake_player.setCurrentItemOrArmor(0, tool);
        {
            // See ItemInWorldManager.tryHarvestBlock

            boolean canHarvest = block.canHarvestBlock(w, pos, fake_player);
            boolean removed;
            {
                block.onBlockHarvested(w, pos, bs, fake_player);
                removed = block.removedByPlayer(w, pos, fake_player, canHarvest);
                if (removed) {
                    block.onBlockDestroyedByPlayer(w, pos, bs);
                }
            }
            if (canHarvest && removed) {
                block.harvestBlock(w, fake_player, pos, bs, te);
            }
            int xp = event.getExpToDrop();
            if (removed && xp > 0) {
                block.dropXpOnBlockBreak(w, pos, xp);
            }
        }
        double r = 0.5;
        AxisAlignedBB box = new AxisAlignedBB(x - r, y - r, z - r, x + 1 + r, y + 1 + r, z + 1 + r);
        if (FzConfig.blockundo_grab) {
            for (Object o : w.getEntitiesWithinAABB(EntityItem.class, box)) {
                EntityItem ei = (EntityItem) o;
                int orig_delay = ei.delayBeforeCanPickup;
                ei.setNoPickupDelay();
                ei.onCollideWithPlayer(real_player);
                ei.setDefaultPickupDelay();
                ei.setPickupDelay(orig_delay);
            }
        }
        PlayerUtil.recycleFakePlayer(fake_player);
    }


    private final HashMap<String, Item> cache = new HashMap<String, Item>();
    private Item findAppropriateTool(String tool, int level) {
        if (tool == null && level == -1) {
            return Items.diamond_pickaxe;
        }
        String name = tool + "#" + level;
        Item ret = cache.get(name);
        if (ret != null) {
            return ret;
        }
        if (cache.containsKey(name)) return null;
        for (Object obj : Item.itemRegistry) {
            Item item = (Item) obj;
            final ItemStack dummy = new ItemStack(item);
            if (item.getToolClasses(dummy).contains(tool)) {
                if (item.getHarvestLevel(dummy, tool) >= level) {
                    cache.put(name, item);
                    return item;
                }
            }
        }
        cache.put(name, null);
        return null;

    }

}
