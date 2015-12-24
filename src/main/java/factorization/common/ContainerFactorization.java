package factorization.common;

import factorization.redstone.TileEntityParaSieve;
import factorization.shared.TileEntityFactorization;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerFactorization extends Container {
    public TileEntityFactorization factory;
    FactoryType type;
    int slot_start, slot_end;
    int player_slot_start, player_slot_end;
    public EntityPlayer entityplayer;
    public int invdx = 0, invdy = 0;

    public ContainerFactorization(EntityPlayer entityplayer, TileEntityFactorization factory) {
        this.factory = factory;
        this.entityplayer = entityplayer;
        this.type = factory.getFactoryType();
    }


    public void addSlotsForGui(TileEntityFactorization ent, InventoryPlayer inventoryplayer) {
        FactoryType type = ent.getFactoryType();
        EntityPlayer player = inventoryplayer.player;
        switch (type) {
        case PARASIEVE:
            TileEntityParaSieve proto = (TileEntityParaSieve) ent;
            for (int i = 0; i < proto.filters.length/2; i++) {
                addSlotToContainer(new Slot(proto, i*2, 53 + i*18, 17));
                addSlotToContainer(new Slot(proto, i*2 + 1, 53 + i*18, 35));
            }
            invdy -= 18;
            break;
        default:
            break;
        }
        addPlayerSlots(inventoryplayer);
    }

    void addPlayerSlots(InventoryPlayer inventoryplayer) {
        player_slot_start = inventorySlots.size();
        for (int i = 0; i < 3; i++) {
            for (int k = 0; k < 9; k++) {
                addSlotToContainer(new Slot(inventoryplayer, k + i * 9 + 9, invdx + 8 + k * 18, invdy + 84 + i * 18));
            }
        }

        for (int j = 0; j < 9; j++) {
            addSlotToContainer(new Slot(inventoryplayer, j, 8 + j * 18, invdy + 142));
        }
        player_slot_end = inventorySlots.size();
        slot_start = 0;
        slot_end = player_slot_start;
    }

    @Override
    public boolean canInteractWith(EntityPlayer entityplayer) {
        if (factory == null) {
            return true;
        }
        return factory.isUseableByPlayer(entityplayer);
    }

    @SuppressWarnings("incomplete-switch")
    @Override
    //transferStackInSlot
    public ItemStack transferStackInSlot(EntityPlayer player, int i) {
        Slot slot = inventorySlots.get(i);
        ItemStack itemstack = slot.getStack();
        if (itemstack == null) {
            return null;
        }

        ItemStack itemstack1 = slot.getStack();
        itemstack = itemstack1.copy();
        if (i < slot_end) {
            if (!mergeItemStack(itemstack1, player_slot_start, player_slot_end, true)) {
                return null;
            }
        } else if (!mergeItemStack(itemstack1, slot_start, slot_end, false)) {
            return null;
        }
        if (itemstack1.stackSize == 0) {
            slot.putStack(null);
        } else {
            slot.onSlotChanged();
        }
        if (itemstack1.stackSize != itemstack.stackSize) {
            slot.onPickupFromSlot(player, itemstack1);
        } else {
            return null;
        }
        return itemstack;
    }
}
