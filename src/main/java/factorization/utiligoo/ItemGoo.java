package factorization.utiligoo;

import factorization.api.Coord;
import factorization.common.Command;
import factorization.coremodhooks.HandleAttackKeyEvent;
import factorization.coremodhooks.HandleUseKeyEvent;
import factorization.shared.Core;
import factorization.shared.Core.TabType;
import factorization.shared.DropCaptureHandler;
import factorization.shared.ICaptureDrops;
import factorization.shared.ItemFactorization;
import factorization.shared.NetworkFactorization.MessageType;
import factorization.util.FzUtil;
import factorization.util.InvUtil;
import factorization.util.InvUtil.FzInv;
import factorization.util.ItemUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTool;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.util.*;

public class ItemGoo extends ItemFactorization {
    public ItemGoo(String name, TabType tabType) {
        super(name, tabType);
        setMaxStackSize(32);
        setHasSubtypes(true);
        Core.loadBus(this);
    }


    @Override
    public boolean onItemUseFirst(ItemStack is, EntityPlayer player, World world, BlockPos pos, EnumFacing fd, float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return false;
        }
        GooData data = GooData.getNullGooData(is, world);
        if (data != null && data.checkWorld(player, new Coord(world, pos))) return false;
        if (player.isSneaking()) {
            if (data == null) return false;
            ArrayList<Integer> toRemove = new ArrayList();
            for (int i = 0; i < data.coords.length; i += 3) {
                data.coords[i + 0] = data.coords[i + 0] + fd.getDirectionVec().getX();
                int goo_y = data.coords[i + 1] = data.coords[i + 1] + fd.getDirectionVec().getY();
                data.coords[i + 2] = data.coords[i + 2] + fd.getDirectionVec().getZ();
                if (goo_y < 0 || goo_y > 0xFF) {
                    toRemove.add(i + 0);
                    toRemove.add(i + 1);
                    toRemove.add(i + 2);
                }
            }
            if (!toRemove.isEmpty()) {
                int[] removed = new int[toRemove.size()];
                int i = 0;
                for (Integer val : toRemove) {
                    removed[i++] = val;
                }
                data.coords = ArrayUtils.removeAll(data.coords, removed);
                if (data.coords.length == 0) {
                    data.wipe(is, world);
                }
                is.stackSize += removed.length / 3;
            }
            data.markDirty();
            return true;
        }
        if (is.stackSize <= 1) return false;
        if (data == null) {
            data = GooData.getGooData(is, world);
        }
        for (int i = 0; i < data.coords.length; i += 3) {
            int ix = data.coords[i + 0];
            int iy = data.coords[i + 1];
            int iz = data.coords[i + 2];
            if (pos.getX() == ix && pos.getY() == iy && pos.getZ() == iz) {
                expandSelection(is, data, player, world, pos, fd);
                return true;
            }
        }
        data.coords = ArrayUtils.addAll(data.coords, pos.getX(), pos.getY(), pos.getZ());
        data.getDimensionId() = FzUtil.getWorldDimension(world);
        data.markDirty();
        is.stackSize--;
        return true;
    }


    public void executeCommand(Command command, EntityPlayerMP player) {
        ItemStack held = player.getHeldItem();
        if (command == Command.gooSelectNone) {
            trySelectNone(player, held);
            return;
        }
        MovingObjectPosition mop = getMovingObjectPositionFromPlayer(player.worldObj, player, false);
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;
        
        for (int slot = 0; slot < 9; slot++) {
            ItemStack is = player.inventory.getStackInSlot(slot);
            if (is == null || is.getItem() != this) continue;
            GooData data = GooData.getNullGooData(is, player.worldObj);
            if (data == null) continue;
            if (data.checkWorld(player, null)) continue;
            for (int i = 0; i < data.coords.length; i += 3) {
                if (eq(data.coords, i, mop.getBlockPos())) {
                    final FzInv playerInv = InvUtil.openInventory(player, true);
                    DropCaptureHandler.startCapture(new ICaptureDrops() {
                        @Override
                        public boolean captureDrops(ArrayList<ItemStack> stacks) {
                            boolean any = false;
                            for (ItemStack is : stacks) {
                                if (ItemUtil.normalize(is) == null) continue;
                                is.stackSize = ItemUtil.getStackSize(playerInv.push(is.copy()));
                                any = true;
                            }
                            return any;
                        }
                    }, new Coord(player), Double.MAX_VALUE);
                    try {
                        if (command == Command.gooLeftClick) {
                            leftClick(player, data, is, held, mop);
                        } else if (command == Command.gooRightClick) {
                            rightClick(player, data, is, held, mop);
                        }
                    } finally {
                        DropCaptureHandler.endCapture();
                    }
                    return;
                }
            }
        }
    }
    
    private void leftClick(EntityPlayerMP player, GooData data, ItemStack gooItem, ItemStack held, MovingObjectPosition mop) {
        // Punch with tool: remove all blocks that can be harvested by the tool
        // Normal punch: degoo 3x3x3 goo area
        // shift-punch: degoo punched block
        if (held != null && (held.getItem() instanceof ItemTool || !held.getItem().getToolClasses(held).isEmpty())) {
            // mineSelection(gooItem, data, player.worldObj, mop, player, held);
        } else {
            int radius = player.isSneaking() ? 0 : 2;
            degooArea(player, data, gooItem, mop, radius);
        }
    }
    
    private void rightClick(EntityPlayer player, GooData data, ItemStack gooItem, ItemStack held, MovingObjectPosition mop) {
        // goo click: expand the selection.
        // ItemBlock click: replace everything with held item
        if (held == null) {
            Coord at = new Coord(player.worldObj, mop);
            Block bat = at.getBlock();
            if (!(bat instanceof BlockStairs || bat instanceof BlockSlab)) return;
            for (int i = 0; i < data.coords.length; i += 3) {
                at.x = data.coords[i + 0];
                at.y = data.coords[i + 1];
                at.z = data.coords[i + 2];
                IBlockState bs = at.getState();
                Block b = bs.getBlock();
                int md = at.getMd();
                if (b.hasTileEntity(bs)) return;
                if (b instanceof BlockStairs) {
                    if (!player.isSneaking()) {
                        md ^= 0x4;
                    } else {
                        md = ((md + 1) % 0x4) | (md & 0x4);
                    }
                    at.setMd(md);
                } else if (b instanceof BlockSlab) {
                    if (!player.isSneaking()) {
                        at.setMd(md ^ 0x8);
                    }
                }
            }
        } else if (held.getItem() == this) {
            int n = player.isSneaking() ? 1 : 2;
            for (int i = 0; i < n; i++) {
                expandSelection(gooItem, data, player, player.worldObj, mop.getBlockPos(), mop.sideHit);
            }
        } else if (held.getItem() instanceof ItemBlock) {
            replaceBlocks(gooItem, data, player.worldObj, player, mop, held);
            for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
                ItemStack is = player.inventory.getStackInSlot(i);
                if (is != null && ItemUtil.normalize(is) == null) {
                    player.inventory.setInventorySlotContents(i, null);
                }
            }
        }
    }
    
    private boolean check(int offset, int i, int x) {
        return offset == 0 || i == x;
    }
    
    boolean similarBlocks(Coord a, Coord b) {
        if (a.getBlock() == b.getBlock()) return true;
        ItemStack ais = a.getBrokenBlock();
        if (ais == null) return false;
        return ItemUtil.identical(ais, b.getBrokenBlock());
    }
    
    private void expandSelection(ItemStack is, GooData data, EntityPlayer player, World world, BlockPos pos, EnumFacing dir) {
        Coord src = new Coord(world, pos);
        HashSet<Coord> found = new HashSet();
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        for (int i = 0; i < data.coords.length; i += 3) {
            int ix = data.coords[i + 0];
            int iy = data.coords[i + 1];
            int iz = data.coords[i + 2];
            if (check(dir.getDirectionVec().getX(), ix, x) && check(dir.getDirectionVec().getY(), iy, y) && check(dir.getDirectionVec().getZ(), iz, z)) {
                Coord at = new Coord(world, ix, iy, iz);
                Coord adj = at.add(dir);
                if (adj.isSolid() || adj.isSolidOnSide(dir.getOpposite())) continue;
                if (!similarBlocks(src, at)) continue;
                for (EnumFacing fd : EnumFacing.VALUES) {
                    if (fd == dir || fd == dir.getOpposite()) continue;
                    Coord n = at.add(fd);
                    if (similarBlocks(at, n)) {
                        Coord nadj = n.add(dir);
                        if (nadj.isSolidOnSide(dir.getOpposite())) continue;
                        if (nadj.getBlock() instanceof BlockSlab && (nadj.getMd() & 8) == 0) {
                            continue;
                        }
                        found.add(n);
                    }
                }
            }
        }
        ArrayList<Coord> addable = new ArrayList();
        nextCoord: for (Coord c : found) {
            for (int i = 0; i < data.coords.length; i += 3) {
                int ix = data.coords[i + 0];
                int iy = data.coords[i + 1];
                int iz = data.coords[i + 2];
                if (c.x == ix && c.y == iy && c.z == iz) continue nextCoord;
            }
            addable.add(c);
        }
        Collections.sort(addable);
        int count = addable.size();
        if (!player.capabilities.isCreativeMode) {
            count = Math.min(count, is.stackSize - 1);
            count = Math.min(maxStackSize - 1 - data.coords.length / 3, count);
        } else {
            int creativeMax = 1024;
            creativeMax -= data.coords.length / 3;
            count = Math.min(count, creativeMax);
        }
        if (count <= 0) return;
        int[] use = new int[count * 3];
        for (int i = 0; i < count; i++) {
            Coord c = addable.get(i);
            use[i * 3 + 0] = c.x;
            use[i * 3 + 1] = c.y;
            use[i * 3 + 2] = c.z;
        }
        is.stackSize -= count;
        if (player.capabilities.isCreativeMode) {
            is.stackSize = Math.max(1, is.stackSize);
        }
        data.coords = ArrayUtils.addAll(data.coords, use);
        data.markDirty();
    }
    
    private boolean degooArea(EntityPlayer player, GooData data, ItemStack gooItem, MovingObjectPosition mop, int radius) {
        if (player.worldObj.isRemote) return false;
        if (data == null) return false;
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return false;
        int d = radius;
        boolean any = false;
        BlockPos pos = mop.getBlockPos();
        for (int dx = -d; dx <= d; dx++) {
            for (int dy = -d; dy <= d; dy++) {
                for (int dz = -d; dz <= d; dz++) {
                    any |= deselectCoord(gooItem, data, player.worldObj, pos.add(dx, dy, dz), false);
                }
            }
        }
        if (any) {
            data.markDirty();
        }
        return false;
    }
    
    private void replaceBlocks(ItemStack is, GooData data, World world, EntityPlayer player, MovingObjectPosition mop, ItemStack source) {
        if (ItemUtil.normalize(source) == null) return;
        int removed = 0;
        Coord at = new Coord(world, 0, 0, 0);
        boolean creative = player.capabilities.isCreativeMode;
        ArrayList<Integer> to_remove = new ArrayList();
        for (int i = 0; i < data.coords.length; i += 3) {
            if (source.stackSize <= 0) {
                for (ItemStack replace : player.inventory.mainInventory) {
                    if (source != replace && ItemUtil.identical(source, replace)) {
                        if (ItemUtil.normalize(replace) != null) {
                            source = replace;
                            break;
                        }
                    }
                }
                if (ItemUtil.normalize(source) == null) break;
            }
            at.x = data.coords[i + 0];
            at.y = data.coords[i + 1];
            at.z = data.coords[i + 2];
            if (creative) {
                at.setAir();
            } else {
                if (at.getHardness() < 0) continue;
                Block block = at.getBlock();
                int md = at.getMd();
                EntityPlayerMP emp = (EntityPlayerMP) player;
                block.onBlockHarvested(world, at.toBlockPos(), at.getState(), emp);
                boolean destroyed = block.removedByPlayer(world, at.toBlockPos(), emp, true);
                if (destroyed) {
                    block.onBlockDestroyedByPlayer(world, at.toBlockPos(), at.getState());
                }
                block.harvestBlock(world, emp, at.toBlockPos(), at.getState(), at.getTE());
            }
            to_remove.add(i + 0);
            to_remove.add(i + 1);
            to_remove.add(i + 2);
            
            ItemBlock ib = (ItemBlock) source.getItem();
            int origSize = source.stackSize;
            ib.onItemUse(source, player, player.worldObj, at.toBlockPos(), mop.sideHit, (float) mop.hitVec.xCoord, (float) mop.hitVec.yCoord, (float) mop.hitVec.zCoord);
            if (creative) {
                source.stackSize = origSize; // Great work, guys.
            }
            removed++;
        }
        if (removed <= 0) return;
        data.removeIndices(to_remove, is, world);
        if (!creative) {
            misplaceSomeGoo(is, world.rand, removed);
        } else {
            is.stackSize += removed;
            is.stackSize = Math.min(is.stackSize, maxStackSize);
        }
    }
    
    private boolean deselectCoord(ItemStack is, GooData data, World world, BlockPos pos, boolean bulkAction) {
        for (int i = 0; i < data.coords.length; i += 3) {
            if (eq(data.coords, i, pos)) {
                data.coords = ArrayUtils.removeAll(data.coords, i, i + 1, i + 2);
                is.stackSize++;
                if (data.coords.length == 0) {
                    data.wipe(is, world);
                } else if (!bulkAction) {
                    data.markDirty();
                }
                return true;
            }
        }
        return false;
    }
    
    private void mineSelection(ItemStack is, GooData data, World world, MovingObjectPosition mop, EntityPlayerMP player, ItemStack tool) {
        boolean creative = player.capabilities.isCreativeMode;
        Item toolItem = creative ? null : tool.getItem();
        ArrayList<Integer> toRemove = new ArrayList();
        int removed = 0;
        float origHardness = Float.POSITIVE_INFINITY; // creative ? Float.POSITIVE_INFINITY : new Coord(world, mop).getHardness();
        for (int i = 0; i < data.coords.length; i += 3) {
            int ix = data.coords[i + 0];
            int iy = data.coords[i + 1];
            int iz = data.coords[i + 2];
            BlockPos pos = new BlockPos(data.coords[i], data.coords[i + 1], data.coords[i + 2]);
            Block b = world.getBlock(pos);
            if (b.isAir(world, pos)) continue;
            float hardness = b.getBlockHardness(world, pos);
            if (hardness < 0 && !creative) continue;
            if (hardness > origHardness) continue;
            boolean canBreak = creative || hardness == 0;
            if (toolItem != null) {
                canBreak |= toolItem.canHarvestBlock(b, tool) || toolItem.getStrVsBlock(tool, b) > 1;
            }
            if (canBreak)  {
                if (player.theItemInWorldManager.tryHarvestBlock(pos)) {
                    removed++;
                    toRemove.add(i);
                    toRemove.add(i + 1);
                    toRemove.add(i + 2);
                }
            }
            if (ItemUtil.normalize(tool) == null) break;
        }
        if (removed == 0) return;
        data.removeIndices(toRemove, is, world);
        if (!creative) {
            misplaceSomeGoo(is, world.rand, removed);
        } else {
            is.stackSize += removed;
            is.stackSize = Math.min(is.stackSize, maxStackSize);
        }
    }
    
    private void misplaceSomeGoo(ItemStack is, Random rand, int removed) {
        if (rand.nextInt(100) < 20) {
            removed--;
        }
        is.stackSize += removed;
    }
    
    @Override
    public void onUpdate(ItemStack is, World world, Entity player, int inventoryIndex, boolean isHeld) {
        if (world.isRemote) return;
        GooData data = GooData.getNullGooData(is, world);
        if (data == null) return;
        if (!(player instanceof EntityPlayer)) return;
        if (!data.isPlayerOutOfDate(player)) return;
        NBTTagCompound dataTag = new NBTTagCompound();
        data.writeToNBT(dataTag);
        FMLProxyPacket toSend = Core.network.entityPacket(player, MessageType.UtilityGooState, dataTag);
        Core.network.broadcastPacket((EntityPlayer) player, new Coord(player), toSend);
    }
    
    @SideOnly(Side.CLIENT)
    public static void handlePacket(ByteBuf input) throws IOException {
        NBTTagCompound dataTag = ByteBufUtils.readTag(input);
        World world = Minecraft.getMinecraft().theWorld;
        GooData data = new GooData(dataTag.getString("mapname")); // NOTE: this resets data.last_traced_index to -1. We might have to reset it manually if networking gets more complicated.
        data.readFromNBT(dataTag);
        world.setItemData(data.mapName, data);
    }
    

    @Override
    protected void addExtraInformation(ItemStack is, EntityPlayer player, List list, boolean verbose) {
        super.addExtraInformation(is, player, list, verbose);
        GooData data = GooData.getNullGooData(is, player.worldObj);
        if (data != null) {
            list.add(I18n.format("item.factorization:utiligoo.placed", data.coords.length / 3));
            if (player != null && player.worldObj != null) {
                if (data.getDimensionId() != FzUtil.getWorldDimension(player.worldObj)) {
                    list.add(I18n.format("item.factorization:utiligoo.wrongDimension"));
                }
            }
            int minX = 0, minY = 0, minZ = 0;
            int maxX = 0, maxY = 0, maxZ = 0;
            for (int i = 0; i < data.coords.length; i += 3) {
                int x = data.coords[i + 0];
                int y = data.coords[i + 1];
                int z = data.coords[i + 2];
                if (i == 0) {
                    minX = maxX = x;
                    minY = maxY = y;
                    minZ = maxZ = z;
                } else {
                    minX = Math.min(x, minX);
                    minY = Math.min(y, minY);
                    minZ = Math.min(z, minZ);
                    maxX = Math.max(x, maxX);
                    maxY = Math.max(y, maxY);
                    maxZ = Math.max(z, maxZ);
                }
            }
            list.add(I18n.format("item.factorization:utiligoo.min", minX, minY, minZ));
            list.add(I18n.format("item.factorization:utiligoo.max", maxX, maxY, maxZ));
            if (Core.dev_environ) {
                list.add("#" + data.mapName);
            }
        }
    }
    
    boolean gooHilighted(EntityPlayer player, MovingObjectPosition mop) {
        if (mop == null || mop.typeOfHit != MovingObjectType.BLOCK) return false;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack is = player.inventory.getStackInSlot(slot);
            if (is == null || is.getItem() != this) continue;
            GooData data = GooData.getNullGooData(is, player.worldObj);
            if (data == null) continue;
            for (int i = 0; i < data.coords.length; i += 3) {
                if (eq(data.coords, i, mop.getBlockPos())) {
                    return true;
                }
            }
        }
        return false;
    }
    
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void interceptGooClick(HandleUseKeyEvent event) {
        // Only used for replacing blocks.
        Minecraft mc = Minecraft.getMinecraft();
        MovingObjectPosition mop = mc.objectMouseOver;
        EntityPlayer player = mc.thePlayer;
        if (gooHilighted(player, mop)) {
            Command.gooRightClick.call(player);
            event.setCanceled(true);
            mc.rightClickDelayTimer = 4;
        }
    }
    
    private boolean trySelectNone(EntityPlayer player, ItemStack is) {
        if (is == null || !(is.getItem() instanceof ItemGoo)) return false;
        GooData data = GooData.getNullGooData(is, player.worldObj);
        if (data == null) return false;
        if (player.worldObj.isRemote) {
            return true;
        }
        int deployed_goo = data.coords.length / 3;
        data.wipe(is, player.worldObj);
        is.stackSize += deployed_goo;
        is.stackSize = Math.min(is.stackSize, maxStackSize);
        return true;
    }
    
    long break_prevention = 0;
    int goo_recently_clicked_index = -1;
    
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void interceptGooBreak(HandleAttackKeyEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        MovingObjectPosition mop = mc.objectMouseOver;
        EntityPlayer player = mc.thePlayer;
        if (break_prevention > 0) {
            if (break_prevention > System.currentTimeMillis()) {
                if (goo_recently_clicked_index == idOfHeld(player) && !player.isSneaking()) {
                    Command.gooSelectNone.call(player);
                    goo_recently_clicked_index = -1;
                }
                event.setCanceled(true);
                delayBreak();
                return;
            }
            break_prevention = 0;
        }
        ItemStack held = player.getHeldItem();
        if (held != null && (held.getItem() instanceof ItemTool || !held.getItem().getToolClasses(held).isEmpty())) return;
        if (gooHilighted(player, mop)) {
            Command.gooLeftClick.call(player);
            event.setCanceled(true);
            delayBreak();
            goo_recently_clicked_index = idOfHeld(player);
        }
    }
    
    private int idOfHeld(EntityPlayer player) {
        if (player == null) return -1;
        GooData gd = GooData.getNullGooData(player.getHeldItem(), player.worldObj);
        if (gd == null) return -1;
        return player.getHeldItem().getItemDamage();
    }
    
    private void delayBreak() {
        int delay = 450;
        Minecraft mc = Minecraft.getMinecraft();
        break_prevention = System.currentTimeMillis() + delay;
        mc.leftClickCounter = delay;
    }
    
    
    ThreadLocal<Boolean> processing = new ThreadLocal<Boolean>();
    
    @SubscribeEvent
    public void mineGooeyBlocks(BreakEvent event) {
        if (processing.get() != null) return;
        EntityPlayer p = event.getPlayer();
        if (!(p instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) p;
        boolean creative = player.capabilities.isCreativeMode;
        ItemStack held = player.getHeldItem();
        if (held == null && !creative) return;
        MovingObjectPosition mop = getMovingObjectPositionFromPlayer(player.worldObj, player, false);
        if (mop == null) return;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack is = ItemUtil.normalize(player.inventory.getStackInSlot(slot));
            if (is == null || is.getItem() != this) continue;
            GooData data = GooData.getNullGooData(is, player.worldObj);
            if (data == null) continue;
            for (int i = 0; i < data.coords.length; i += 3) {
                if (eq(data.coords, i, mop.getBlockPos())) {
                    processing.set(true);
                    try {
                        mineSelection(is, data, player.worldObj, mop, player, held);
                    } finally {
                        processing.remove();
                    }
                    return;
                }
            }
        }
    }

    static boolean eq(int[] coords, int i, BlockPos pos) {
        return coords[i] == pos.getX() && coords[i + 1] == pos.getY() && coords[i + 2] == pos.getZ();
    }
}
