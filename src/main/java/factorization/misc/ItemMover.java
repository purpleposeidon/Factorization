package factorization.misc;

import factorization.util.ItemUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;

public class ItemMover {
    static final int CLICK_BUTTON = 0;
    static final int MODE = 0;

    // Wow, is this complicated!
    // We're moving some items from one slot to another.
    // When we're exporting, we let the container figure out what slot to use.
    // However we can't do this when importing, so we've got to find the best slot ourselves.
    // Inventories can do arbitrary things, so we've got to carefully manage every aspect to prevent item loss/dupe


    public static void moveItems(EntityPlayer player, int slotId, int motion) {
        if (player.worldObj.isRemote) return;
        Slot slot = player.openContainer.getSlot(slotId);
        if (slot == null) return;
        ItemStack search = ItemUtil.normalize(slot.getStack());
        if (search == null) return;
        if (motion == 0) return;
        final InventoryPlayer pinv = player.inventory;
        if (pinv.getItemStack() != null) return;
        ItemStack buffer = null;
        int amount = Math.abs(motion);

        //motion = reevaluateMotion(slot, player, motion);

        if (!slot.canTakeStack(player) || !slot.isItemValid(search)) return;
        Slot stock = getRestockSlot(player, slot, search);
        /*if (stock != null) {
            if (stock.yDisplayPosition < slot.yDisplayPosition) {
                motion = -motion;
            }
        } else {
            motion = reevaluateMotion(slot, player, motion);
        }*/

        if (motion <= -1) {
            // move all but 1 into buffer, shift-click the 1 item, restore buffer
            buffer = leave(slot, amount);
            if (buffer == null && ItemUtil.normalize(slot.getStack()) == null) return; // The slot might be low on items
            player.openContainer.transferStackInSlot(player, slotId);
            if (pinv.getItemStack() == null && buffer != null) {
                pinv.setItemStack(buffer);
                buffer = null;
                player.openContainer.slotClick(slotId, CLICK_BUTTON, MODE, player);
            }
        } else if (motion >= +1) {
            // find the furthest away slot, remove all but 1, pick up, drop
            if (stock == null) return;
            ItemStack stockStack = ItemUtil.normalize(stock.getStack());
            if (stockStack == null) return;
            int free = ItemUtil.getFreeSpace(search, 64);
            amount = Math.min(free, amount);
            if (amount <= 0) return;
            buffer = leave(stock, amount); // The slot might be low on items
            if (buffer == null && ItemUtil.normalize(stock.getStack()) == null) return;
            player.openContainer.slotClick(stock.slotNumber, CLICK_BUTTON, MODE, player);
            if (pinv.getItemStack() != null) {
                player.openContainer.slotClick(slotId, CLICK_BUTTON, MODE, player);
                if (pinv.getItemStack() == null && buffer != null) {
                    pinv.setItemStack(buffer);
                    buffer = null;
                    player.openContainer.slotClick(stock.slotNumber, CLICK_BUTTON, MODE, player);
                } else if (pinv.getItemStack() != null) {
                    // It might not have fit, despite our precaution
                    // The buffer & held are 'probably' the same, and the original slot is 'probably' empty.
                    ItemStack held = pinv.getItemStack();
                    if (ItemUtil.couldMerge(held, buffer)) {
                        held.stackSize += ItemUtil.getStackSize(buffer);
                        buffer = null;
                    }
                    if (stock.getStack() == null) {
                        player.openContainer.slotClick(stock.slotNumber, CLICK_BUTTON, MODE, player);
                    }
                }
            }
        }

        // If the inventory done us dirty, we might still have something in the buffer
        if (buffer != null) {
            if (pinv.getItemStack() == null) {
                pinv.setItemStack(buffer);
            } else {
                player.dropPlayerItemWithRandomChoice(buffer, true);
            }
        }
        if (pinv.getItemStack() != null) {
            // We started with nothing held, and so must update
            updateHeldItem(player);
        }
    }

    private static void updateHeldItem(EntityPlayer player) {
        if (player instanceof EntityPlayerMP) {
            ((EntityPlayerMP) player).updateHeldItem();
        }
    }

    private static int reevaluateMotion(Slot slot, EntityPlayer player, int motion) {
        // Make sure that pressing 'up' makes the item move up
        if (slot.inventory == player.inventory) {
            return -motion;
        }
        return motion;
    }

    private static ItemStack leave(Slot slot, int amount) {
        int avail = slot.getStack().stackSize;
        int toTake = avail - amount;
        if (toTake <= 0) return null;
        return slot.decrStackSize(toTake);
    }

    private static Slot getRestockSlot(EntityPlayer player, Slot src, ItemStack search) {
        ArrayList<Slot> foreign = new ArrayList<Slot>();
        ArrayList<Slot> local = new ArrayList<Slot>();
        for (Slot slot : (Iterable<Slot>) player.openContainer.inventorySlots) {
            if (ItemUtil.identical(search, slot.getStack())) {
                if (!slot.canTakeStack(player) || !slot.isItemValid(search)) continue;
                if (slot.inventory == src.inventory) {
                    if (slot != src) local.add(slot);
                } else {
                    foreign.add(slot);
                }
            }
        }
        if (!foreign.isEmpty()) return foreign.get(0);
        if (!local.isEmpty()) return local.get(0);
        return null;
    }
}
