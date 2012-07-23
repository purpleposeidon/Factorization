package factorization.common;

import net.minecraft.src.Block;
import net.minecraft.src.Container;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.IInventory;
import net.minecraft.src.InventoryPlayer;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.Slot;

public class ContainerFactorization extends Container {
    public TileEntityFactorization factory;
    FactoryType type;
    int slot_start, slot_end;
    int player_slot_start, player_slot_end;
    EntityPlayer entityplayer;
    int invdx = 0, invdy = 0;

    public ContainerFactorization(EntityPlayer entityplayer,
            TileEntityFactorization factory) {
        this.factory = factory;
        this.entityplayer = entityplayer;
        this.type = factory.getFactoryType();
    }

    public ContainerFactorization(EntityPlayer entityplayer, FactoryType type) {
        this.factory = null;
        this.entityplayer = entityplayer;
        this.type = type;
    }

    class FactorySlot extends Slot {
        int[] allowed;
        int[] forbidden;

        public FactorySlot(IInventory iinventory, int slotNumber, int posX,
                int posY, int[] a, int[] f) {
            super(iinventory, slotNumber, posX, posY);
            allowed = a;
            forbidden = f;
        }

        @Override
        public boolean isItemValid(ItemStack itemstack) {
            if (!super.isItemValid(itemstack)) {
                return false;
            }
            if (allowed != null) {
                for (int a : allowed) {
                    if (itemstack.getItem().shiftedIndex == a) {
                        return true;
                    }
                }
                return false;
            }
            if (forbidden != null) {
                for (int f : forbidden) {
                    if (itemstack.getItem().shiftedIndex == f) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }
    }

    class StackLimitedSlot extends Slot {
        int max_size;

        public StackLimitedSlot(int max_size, IInventory par1iInventory, int par2, int par3,
                int par4) {
            super(par1iInventory, par2, par3, par4);
            this.max_size = max_size;
        }

        @Override
        public int getSlotStackLimit() {
            return max_size;
        }

    }

    public void addSlotsForGui(TileEntityFactorization ent, InventoryPlayer inventoryplayer) {
        FactoryType type = ent.getFactoryType();
        switch (type) {
        case ROUTER:
            TileEntityRouter router = (TileEntityRouter) ent;
            addSlot(new Slot(router, 0, 62, 22));
            for (int i = 0; i < 9; i++) {
                addSlot(new StackLimitedSlot(1, router, 1 + i, 8 + i * 18, 44 - 0xFFFFFF));
            }
            break;
        case CUTTER:
            TileEntityCutter cutter = (TileEntityCutter) ent;
            addSlot(new Slot(cutter, 8, 80, 17));
            for (int i = 0; i < 8; i++) {
                int top = 41;
                int left = 53;
                if (i >= 4) {
                    top = 60;
                }
                left += (i % 4) * 18;

                addSlot(new Slot(cutter, i, left, top));
            }
            break;
        case MAKER:
            TileEntityMaker maker = (TileEntityMaker) ent;
            int[] ic = { Core.registry.item_craft.shiftedIndex };
            // input: No item_craft allowed!
            addSlot(new FactorySlot(maker, 0, 58, 19, null, ic));
            int[] fuel = { Core.registry.item_craft.shiftedIndex,
                    Item.paper.shiftedIndex, Item.book.shiftedIndex,
                    Block.bookShelf.blockID, Item.painting.shiftedIndex,
                    Item.map.shiftedIndex };
            // craft: Must be paper/craft
            addSlot(new FactorySlot(maker, 1, 58, 55, fuel, null));
            // output: Nothing goes in
            addSlot(new FactorySlot(maker, 2, 152, 37, null, null));
            break;
        case STAMPER:
        case PACKAGER:
            TileEntityStamper stamper = (TileEntityStamper) ent;
            addSlot(new Slot(stamper, 0, 44, 43));
            addSlot(new FactorySlot(stamper, 1, 116, 43, null, null));
            break;
        case SLAGFURNACE:
            TileEntitySlagFurnace furnace = (TileEntitySlagFurnace) ent;
            addSlot(new Slot(furnace, 0, 56, 17));
            addSlot(new Slot(furnace, 1, 56, 53));
            addSlot(new Slot(furnace, 2, 115, 34));
            addSlot(new Slot(furnace, 3, 141, 34));
            break;
        default:
            break;
        }
        addPlayerSlots(inventoryplayer);
        if (type == FactoryType.ROUTER) {
            slot_end = 1;
        }
    }

    void addPlayerSlots(InventoryPlayer inventoryplayer) {
        player_slot_start = inventorySlots.size();
        for (int i = 0; i < 3; i++) {
            for (int k = 0; k < 9; k++) {
                addSlot(new Slot(inventoryplayer, k + i * 9 + 9, invdx + 8 + k * 18, invdy + 84 + i * 18));
            }
        }

        for (int j = 0; j < 9; j++) {
            addSlot(new Slot(inventoryplayer, j, 8 + j * 18, 142));
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

    @Override
    public ItemStack transferStackInSlot(int i) {
        ItemStack itemstack = null;
        Slot slot = (Slot) inventorySlots.get(i);
        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();
            if (i < slot_end) {
                if (!mergeItemStack(itemstack1, player_slot_start,
                        player_slot_end, true)) {
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
                slot.onPickupFromSlot(itemstack1);
            } else {
                return null;
            }
        }
        return itemstack;
    }
}
