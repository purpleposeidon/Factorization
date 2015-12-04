package factorization.util;

import factorization.api.Coord;
import factorization.shared.TileEntityCommon;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.ILockableContainer;
import net.minecraft.world.World;

import java.util.List;

/**
 * Everything to do with moving items around
 */
public final class InvUtil {

    public static void givePlayerItem(EntityPlayer player, ItemStack is) {
        FzInv inv = openInventory(player.inventory, EnumFacing.UP);
        ItemStack drop = inv.push(is);
        if (drop != null) {
            player.dropPlayerItemWithRandomChoice(drop, false);
        }
        player.openContainer.detectAndSendChanges();
    }

    public static ItemStack transferSlotToSlots(EntityPlayer player, Slot clickSlot, Iterable<Slot> destinations) {
        return transferSlotToSlots(player, clickSlot, destinations, 64);
    }

    public static ItemStack transferSlotToSlots(EntityPlayer player, Slot clickSlot, Iterable<Slot> destinations, int maxTransfer) {
        ItemStack got = tryTransferSlotToSlots(player, clickSlot, destinations, maxTransfer);
        if (got != null) {
            clickSlot.putStack(got);
        }
        return null;
    }

    public static ItemStack tryTransferSlotToSlots(EntityPlayer player, Slot clickSlot, Iterable<Slot> destinations) {
        return tryTransferSlotToSlots(player, clickSlot, destinations, 64);
    }

    public static ItemStack tryTransferSlotToSlots(EntityPlayer player, Slot clickSlot, Iterable<Slot> destinations, int maxTransfer) {
        ItemStack clickStack = ItemUtil.normalize(clickSlot.getStack());
        if (clickStack == null) {
            return null;
        }
        int clickStackSize = Math.min(maxTransfer, clickStack.stackSize);
        clickSlot.onPickupFromSlot(player, clickStack);
        //try to fill up partially filled slots
        for (Slot slot : destinations) {
            ItemStack is = ItemUtil.normalize(slot.getStack());
            if (is == null || !ItemUtil.couldMerge(is, clickStack)) {
                continue;
            }
            if (maxTransfer <= 0) return null;
            int freeSpace = Math.min(is.getMaxStackSize() - is.stackSize, slot.getSlotStackLimit() - is.stackSize);
            if (freeSpace <= 0) {
                continue;
            }
            if (!slot.isItemValid(clickStack)) {
                continue;
            }
            int delta = Math.min(freeSpace, clickStackSize);
            delta = Math.min(delta, maxTransfer);
            if (delta <= 0) continue;
            is.stackSize += delta;
            slot.putStack(is);
            clickStack.stackSize -= delta;
            clickStackSize -= delta;
            if (clickStackSize <= 0) {
                clickSlot.putStack(null);
                return null;
            }
        }
        //try to fill up empty slots
        for (Slot slot : destinations) {
            if (slot.getHasStack() || !slot.isItemValid(clickStack)) {
                continue;
            }
            if (maxTransfer <= 0) return null;
            int freeSpace = Math.min(slot.getSlotStackLimit(), clickStack.getMaxStackSize());
            int delta = Math.min(freeSpace, clickStackSize);
            delta = Math.min(delta, maxTransfer);
            if (delta <= 0) continue;
            ItemStack toPut = clickStack.copy();
            toPut.stackSize = delta;
            slot.putStack(toPut);
            clickStack.stackSize -= delta;
            clickStackSize -= delta;
            clickStack = ItemUtil.normalize(clickStack);
            if (clickStack == null) {
                clickSlot.putStack(null);
                return null;
            }
        }

        return ItemUtil.normalize(clickStack);
    }

    public static FzInv openInventory(IInventory orig_inv, final EnumFacing side) {
        return openInventory(orig_inv, side, true);
    }

    public static FzInv openInventory(IInventory orig_inv, final EnumFacing side, boolean openBothChests) {
        if (orig_inv == null) {
            return null;
        }
        if (orig_inv instanceof TileEntityChest) {
            orig_inv = openDoubleChest((TileEntityChest) orig_inv, openBothChests);
            if (orig_inv == null) {
                return null;
            }
        }
        if (orig_inv instanceof ISidedInventory) {
            final net.minecraft.inventory.ISidedInventory inv = (net.minecraft.inventory.ISidedInventory) orig_inv;
            int[] invSlots = inv.getSlotsForFace(side);
            if (invSlots == null) {
                invSlots = new int[0];
            }
            final int[] slotMap = invSlots;
            return new FzInv(inv) {
                @Override
                int slotIndex(int i) {
                    return slotMap[i];
                }

                @Override
                public int size() {
                    return slotMap.length;
                }

                @Override
                public boolean canExtract(int slot, ItemStack is) {
                    if (is == null) {
                        return false;
                    }
                    return inv.canExtractItem(slotMap[slot], is, side);
                }

                @Override
                public boolean canInsert(int i, ItemStack is) {
                    if (forceInsert) {
                        return true;
                    }
                    return super.canInsert(i, is) && inv.canInsertItem(slotIndex(i), is, side);
                }};
        } else {
            return new PlainInvWrapper(orig_inv);
        }
    }

    public static FzInv openInventory(Entity ent, boolean access_players) {
        if (ent instanceof EntityPlayer && !access_players) {
            return null;
        }
        if (ent instanceof IInventory) {
            return openInventory((IInventory) ent, EnumFacing.UP);
        }
        if (ent instanceof EntityPlayer) {
            InventoryPlayer ip = ((EntityPlayer)ent).inventory;
            return openInventory(ip, EnumFacing.UP).slice(0, ip.mainInventory.length);
        }
        return null;
    }

    public static boolean canAccessSlot(IInventory inv, int slot) {
        if (inv instanceof ISidedInventory) {
            ISidedInventory isi = (ISidedInventory) inv;
            //O(n). Ugh.
            for (EnumFacing face : EnumFacing.VALUES) {
                int[] slots = isi.getSlotsForFace(face);
                for (int j = 0; j < slots.length; j++) {
                    if (slots[j] == slot) {
                        return true;
                    }
                }
            }
        } else {
            return true;
        }
        return false;
    }

    /**
     * If you are accessing multiple chests, and some might be adjacent you'll want to treat them as a double chest. Calling this function with a lower chest
     * will return 'null'; calling with an upper chest will return an InventoryLargeChest. If it's a single chest, it'll return that chest.
     *
     * @param chest
     * @return
     */
    public static IInventory openDoubleChest(TileEntityChest chest, boolean openBothSides) {
        ILockableContainer origChest = chest;
        World world = chest.getWorld();
        int i = chest.getPos().getX(), j = chest.getPos().getY(), k = chest.getPos().getZ();
        Block cb = chest.getBlockType();
        if (cb == null) {
            return null;
        }
        Block chestBlock = chest.getBlockType();
        for (EnumFacing face : new EnumFacing[] { EnumFacing.NORTH, EnumFacing.WEST }) {
            // Upper chest
            BlockPos peek = chest.getPos().offset(face);
            if (world.getBlockState(peek).getBlock() == chestBlock) {
                return new InventoryLargeChest("doublechest", (ILockableContainer) world.getTileEntity(peek), origChest);
            }
        }
        for (EnumFacing face : new EnumFacing[] { EnumFacing.SOUTH, EnumFacing.EAST }) {
            // If we're the lower chest, perhaps skip ourselves
            BlockPos peek = chest.getPos().offset(face);
            if (world.getBlockState(peek).getBlock() == chestBlock) {
                if (openBothSides) {
                    return new InventoryLargeChest("doublechest", origChest, (ILockableContainer) world.getTileEntity(peek));
                }
                return null;
            }

        }
        return chest;
    }

    public static IInventory openDoubleChest(IInventory inv, boolean openBothSides) {
        if (inv instanceof TileEntityChest) {
            return openDoubleChest((TileEntityChest) inv, openBothSides);
        }
        return inv;
    }

    public static void collapseItemList(List<ItemStack> total) {
        int i = 0;
        while (i < total.size()) {
            ItemStack is = ItemUtil.normalize(total.get(i));
            if (is == null) {
                total.remove(i);
                continue;
            }
            int s = i + 1;
            while (s < total.size()) {
                ItemStack other = ItemUtil.normalize(total.get(s));
                if (other == null) {
                    total.remove(s);
                    continue;
                }
                if (ItemUtil.couldMerge(is, other)) {
                    int free = is.getMaxStackSize() - is.stackSize;
                    if (free <= 0) {
                        break;
                    }
                    int delta = Math.min(free, other.stackSize);
                    is.stackSize += delta;
                    other.stackSize -= delta;
                    if (other.stackSize <= 0) {
                        total.remove(s);
                        continue;
                    }
                }
                s++;
            }
            i++;
        }
    }

    public static boolean emptyBuffer(EntityPlayer entityplayer, List<ItemStack> buffer, TileEntityCommon te) {
        if (buffer.isEmpty()) return false;
        ItemStack is = buffer.remove(0);
        new Coord(te).spawnItem(is).onCollideWithPlayer(entityplayer);
        te.markDirty();
        return true;
    }

    public static EntityItem spawnItemStack(Coord c, ItemStack item) {
        if (item == null) {
            return null;
        }
        EntityItem entityitem = new EntityItem(c.w, c.x + 0.5, c.y + 0.5, c.z + 0.5, item);
        entityitem.motionY = 0.2 + NumUtil.rand.nextGaussian() * 0.02;
        entityitem.motionX = NumUtil.rand.nextGaussian() * 0.02;
        entityitem.motionZ = NumUtil.rand.nextGaussian() * 0.02;
        c.w.spawnEntityInWorld(entityitem);
        return entityitem;
    }

    public static EntityItem spawnItemStack(Entity c, ItemStack item) {
        if (item == null) {
            return null;
        }
        EntityItem entityitem = new EntityItem(c.worldObj, c.posX + c.width/2, c.posY + c.height/2, c.posZ + c.width/2, item);
        entityitem.motionY = 0.2 + NumUtil.rand.nextGaussian() * 0.02;
        entityitem.motionX = NumUtil.rand.nextGaussian() * 0.02;
        entityitem.motionZ = NumUtil.rand.nextGaussian() * 0.02;
        c.worldObj.spawnEntityInWorld(entityitem);
        return entityitem;
    }

    public static abstract class FzInv {
        public abstract int size();
        abstract int slotIndex(int i);

        boolean forceInsert = false;
        boolean callInvChanged = true;

        public final IInventory under;

        public FzInv(IInventory inv) {
            this.under = inv;
        }

        public void setInsertForce(boolean b) {
            forceInsert = b;
        }

        public void setCallOnInventoryChanged(boolean b) {
            callInvChanged = b;
        }

        public void onInvChanged() {
            if (callInvChanged) {
                under.markDirty();
            }
        }

        public ItemStack get(int i) {
            return under.getStackInSlot(slotIndex(i));
        }

        public void set(int i, ItemStack is) {
            under.setInventorySlotContents(slotIndex(i), is);
        }

        public int getFreeSpace(int i) {
            ItemStack dest = get(i);
            if (dest == null) {
                return under.getInventoryStackLimit();
            }
            int ret = Math.min(under.getInventoryStackLimit(), dest.getMaxStackSize()) - dest.stackSize;
            return Math.max(0, ret);
        }

        public int getFreeSpaceFor(ItemStack target, int maxNeeded) {
            int space = 0;
            int spaceInEmpty = Math.min(target.getMaxStackSize(), under.getInventoryStackLimit());
            for (int i = 0; i < size(); i++) {
                if (!canInsert(i, target)) continue;
                ItemStack is = get(i);
                if (is == null) {
                    space += spaceInEmpty;
                } else if (ItemUtil.couldMerge(target, is)) {
                    space += spaceInEmpty - is.stackSize;
                } else {
                    continue;
                }
                if (space >= maxNeeded) {
                    return space;
                }
            }
            return space;
        }

        public boolean canPush(ItemStack is) {
            for (int i = 0; i < size(); i++) {
                ItemStack here = get(i);
                if (get(i) == null) {
                    return true;
                }
                if (ItemUtil.couldMerge(here, is)) {
                    return true;
                }
            }
            return false;
        }

        public ItemStack pushInto(int i, ItemStack is) {
            int slotIndex = slotIndex(i);
            if (!canInsert(i, is)) {
                return is;
            }
            ItemStack dest = under.getStackInSlot(slotIndex);
            if (dest == null) {
                ItemStack toPut = is;
                int stack_limit = under.getInventoryStackLimit();
                if (toPut.stackSize > stack_limit) {
                    toPut = is.splitStack(stack_limit);
                } else {
                    is = null;
                }
                under.setInventorySlotContents(slotIndex, toPut);
                onInvChanged();
                return is;
            }
            if (!ItemUtil.couldMerge(dest, is)) {
                return is;
            }

            int dest_free = getFreeSpace(i);
            if (dest_free < 1) {
                return is;
            }
            int delta = Math.min(dest_free, is.stackSize);
            dest.stackSize += delta;
            is.stackSize -= delta;
            under.setInventorySlotContents(slotIndex, dest);
            onInvChanged();
            return ItemUtil.normalize(is);
        }

        public boolean canExtract(int slot, ItemStack is) {
            return true;
        }

        public boolean canInsert(int i, ItemStack is) {
            if (forceInsert) {
                return true;
            }
            return under.isItemValidForSlot(slotIndex(i), is) && ItemUtil.couldMerge(get(i), is);
        }

        public boolean isEmpty() {
            for (int i = 0; i < size(); i++) {
                if (get(i) != null) {
                    return false;
                }
            }
            return true;
        }

        public boolean transfer(FzInv dest_inv, int max_transfer, ItemStack exclude) {
            for (int i = 0; i < size(); i++) {
                ItemStack is = ItemUtil.normalize(get(i));
                if (is == null || is == exclude) {
                    continue;
                }
                if (!canExtract(i, is)) {
                    continue;
                }
                if (is.stackSize <= max_transfer) {
                    int orig_size = is.stackSize;
                    is = dest_inv.push(is);
                    if (orig_size != ItemUtil.getStackSize(is)) {
                        set(i, is);
                        onInvChanged();
                        return true;
                    }
                } else {
                    ItemStack to_push = is.copy();
                    int orig_size = Math.min(to_push.stackSize, max_transfer);
                    to_push.stackSize = orig_size;
                    to_push = dest_inv.push(to_push);
                    int taken = orig_size - ItemUtil.getStackSize(to_push);
                    if (taken > 0) {
                        is.stackSize -= taken;
                        is = ItemUtil.normalize(is);
                        set(i, is);
                        onInvChanged();
                        return true;
                    }
                }
            }
            return false;
        }

        public int transfer(int i, FzInv dest_inv, int dest_i, int max_transfer) {
            ItemStack src = ItemUtil.normalize(get(i));
            if (src == null) {
                return 0;
            }
            if (!canExtract(i, src)) {
                return 0;
            }
            ItemStack dest = dest_inv.get(dest_i);
            if (dest == null) {
                dest = src.copy();
                dest.stackSize = 0;
            } else if (!ItemUtil.couldMerge(src, dest)) {
                return 0;
            }
            if (!dest_inv.canInsert(dest_i, src)) {
                return 0;
            }
            int dest_free = dest_inv.getFreeSpace(dest_i);
            if (dest_free < 1) {
                return 0;
            }
            int delta = Math.min(dest_free, src.stackSize);
            delta = Math.min(max_transfer, delta);
            dest.stackSize += delta;
            src.stackSize -= delta;
            src = ItemUtil.normalize(src);
            dest_inv.set(dest_i, dest);
            set(i, src);
            if (callInvChanged) {
                dest_inv.under.markDirty();
                under.markDirty();
            }
            return delta;
        }

        public ItemStack push(ItemStack is) {
            is = ItemUtil.normalize(is);
            //First, fill up already existing stacks
            int first_empty = -1;
            for (int i = 0; i < size(); i++) {
                if (is == null) return null;
                ItemStack dest = get(i);
                if (dest != null) {
                    is = ItemUtil.normalize(pushInto(i, is));
                } else if (first_empty == -1) {
                    first_empty = i;
                }
            }
            //Second, add to null stacks
            if (first_empty == -1) return is; // No nulls found.
            for (int i = first_empty; i < size(); i++) {
                if (is == null) {
                    return null;
                }
                ItemStack dest = get(i);
                if (dest == null) {
                    is = ItemUtil.normalize(pushInto(i, is));
                }
            }
            return is;
        }

        public ItemStack peek() {
            for (int i = 0; i < size(); i++) {
                ItemStack is = ItemUtil.normalize(get(i));
                if (is != null) {
                    return is;
                }
            }
            return null;
        }

        public ItemStack pull() {
            for (int i = 0; i < size(); i++) {
                ItemStack ret = pull(i, 64);
                if (ret != null) {
                    return ret;
                }
            }
            return null;
        }

        public ItemStack pullFromSlot(int slot) {
            return pull(slot, 64);
        }

        public ItemStack pullWithLimit(int limit) {
            for (int i = 0; i < size(); i++) {
                ItemStack ret = pull(i, limit);
                if (ret != null) {
                    return ret;
                }
            }
            return null;
        }

        public ItemStack pull(int slot, int limit) {
            int i = slotIndex(slot);
            ItemStack is = under.getStackInSlot(i);
            if (ItemUtil.normalize(is) == null) {
                return null;
            }
            if (!canExtract(slot, is)) {
                return null;
            }
            return under.decrStackSize(i, limit);
        }

        public ItemStack pull(ItemStack toMatch, int limit, boolean strict) {
            ItemStack ret = null;
            for (int i = 0; i < size(); i++) {
                ItemStack is = get(i);
                if (strict) {
                    if (!ItemUtil.couldMerge(toMatch, is)) {
                        continue;
                    }
                } else {
                    if (!ItemUtil.wildcardSimilar(toMatch, is)) {
                        continue;
                    }
                }
                ItemStack pulled = ItemUtil.normalize(pull(i, limit));
                if (pulled == null) {
                    continue;
                }
                limit -= pulled.stackSize;
                if (ret == null) {
                    ret = pulled;
                } else {
                    ret.stackSize += pulled.stackSize;
                }
                if (limit <= 0) {
                    break;
                }
            }
            return ret;
        }

        private int slice_index(int i) {
            int size = size();
            while (i < 0 && size > 0) {
                i += size; //super inefficient!
            }
            return i;
        }

        public FzInv slice(int start, int end) {
            start = slice_index(start);
            end = slice_index(end);
            start = Math.max(start, 0);
            end = Math.min(end, size());
            if (end < start) {
                end = start;
            }
            if (start > end) {
                start = end;
            }
            return new SubsetInv(this, start, end);
        }
    }

    public static class SubsetInv extends FzInv {
        final FzInv ui;
        int start, end;
        public SubsetInv(FzInv ui, int start, int end) {
            super(ui.under);
            this.ui = ui;
            this.start = start;
            this.end = end;
        }

        @Override
        public int size() {
            return end - start;
        }

        @Override
        int slotIndex(int i) {
            return ui.slotIndex(start + i);
        }

    }

    public static class PlainInvWrapper extends FzInv {
        final int length;
        public PlainInvWrapper(IInventory inv) {
            super(inv);
            length = inv.getSizeInventory();
        }

        @Override
        int slotIndex(int i) {
            return i;
        }

        @Override
        public int size() {
            return length;
        }
    }
}
