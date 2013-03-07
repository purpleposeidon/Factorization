package factorization.common;

import java.util.ArrayList;
import java.util.Arrays;

import net.minecraft.inventory.Container;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.inventory.IInventory;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotFurnace;
import net.minecraft.tileentity.TileEntityFurnace;

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

        public FactorySlot(IInventory iinventory, int slotNumber, int posX, int posY, int[] a, int[] f) {
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
                    if (itemstack.getItem().itemID == a) {
                        return true;
                    }
                }
                return false;
            }
            if (forbidden != null) {
                for (int f : forbidden) {
                    if (itemstack.getItem().itemID == f) {
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
        EntityPlayer player = inventoryplayer.player;
        switch (type) {
        case ROUTER:
            TileEntityRouter router = (TileEntityRouter) ent;
            addSlotToContainer(new Slot(router, 0, 62, 22));
            for (int i = 0; i < 9; i++) {
                addSlotToContainer(new StackLimitedSlot(64, router, 1 + i, 8 + i * 18, 44 - 0xFFFFFF));
            }
            break;
        case MAKER:
            TileEntityMaker maker = (TileEntityMaker) ent;
            int[] ic = { Core.registry.item_craft.itemID };
            // input: No item_craft allowed!
            addSlotToContainer(new FactorySlot(maker, 0, 51, 19, null, ic));
            int[] paper = { Item.paper.itemID };
            int[] packet = { Core.registry.item_craft.itemID };
            // paper: Must be paper
            addSlotToContainer(new FactorySlot(maker, 1, 42, 55, paper, null));
            // craft: Must be a craft packet
            addSlotToContainer(new FactorySlot(maker, 2, 60, 55, packet, null));
            // output: Nothing goes in
            addSlotToContainer(new FactorySlot(maker, 3, 152, 37, null, null));
            break;
        case STAMPER:
        case PACKAGER:
            TileEntityStamper stamper = (TileEntityStamper) ent;
            addSlotToContainer(new Slot(stamper, 0, 44, 43));
            addSlotToContainer(new FactorySlot(stamper, 1, 116, 43, null, null));
            break;
        case SLAGFURNACE:
            TileEntitySlagFurnace furnace = (TileEntitySlagFurnace) ent;
            addSlotToContainer(new Slot(furnace, 0, 56, 17));
            addSlotToContainer(new Slot(furnace, 1, 56, 53));
            addSlotToContainer(new SlotFurnace(player, furnace, 2, 114, 22));
            addSlotToContainer(new SlotFurnace(player, furnace, 3, 114, 48));
            break;
        case GRINDER:
            TileEntityGrinder grinder = (TileEntityGrinder) ent;
            addSlotToContainer(new Slot(grinder, 0, 56, 35));
            addSlotToContainer(new SlotFurnace(player, grinder, 1, 116, 35));
            break;
        case MIXER:
            TileEntityMixer mixer = (TileEntityMixer) ent;
            //inputs
            addSlotToContainer(new Slot(mixer, 0, 38, 25));
            addSlotToContainer(new Slot(mixer, 1, 56, 25));
            addSlotToContainer(new Slot(mixer, 2, 38, 43));
            addSlotToContainer(new Slot(mixer, 3, 56, 43));
            //outputs
            addSlotToContainer(new SlotFurnace(player, mixer, 4, 112, 25));
            addSlotToContainer(new SlotFurnace(player, mixer, 5, 130, 25));
            addSlotToContainer(new SlotFurnace(player, mixer, 6, 112, 43));
            addSlotToContainer(new SlotFurnace(player, mixer, 7, 130, 43));
            break;
        case CRYSTALLIZER:
            TileEntityCrystallizer crys = (TileEntityCrystallizer) ent;
            //surounding inputs
            addSlotToContainer(new Slot(crys, 0, 80, 13));
            addSlotToContainer(new Slot(crys, 1, 108, 29));
            addSlotToContainer(new Slot(crys, 2, 108, 55));
            addSlotToContainer(new Slot(crys, 3, 80, 69));
            addSlotToContainer(new Slot(crys, 4, 52, 55));
            addSlotToContainer(new Slot(crys, 5, 52, 29));
            //central output
            addSlotToContainer(new SlotFurnace(player, crys, 6, 80, 40));
            break;
        default:
            //Fun Fact: progress is done by subclassing; see ContainerSlagFurnace
            //Additional Fun Fact: Juse use SlotFurnace for output slots!
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

    @Override
    //transferStackInSlot
    public ItemStack transferStackInSlot(EntityPlayer player, int i) {
        Slot slot = (Slot) inventorySlots.get(i);
        ItemStack itemstack = slot.getStack();
        if (itemstack == null) {
            return null;
        }
        
        switch (type) {
        //inventory shift click, need special logic
        case SLAGFURNACE:
            if (i >= 4) {
                if (TileEntityFurnace.getItemBurnTime(itemstack) > 0) {
                    return FactorizationUtil.transferSlotToSlots(slot, Arrays.asList((Slot) inventorySlots.get(1)));
                } else {
                    return FactorizationUtil.transferSlotToSlots(slot, Arrays.asList((Slot) inventorySlots.get(0)));
                }
            }
            break;
        case MAKER:
            if (i >= 4) {
                Item item = itemstack.getItem();
                if (item == Item.paper) {
                    return FactorizationUtil.transferSlotToSlots(slot, Arrays.asList((Slot) inventorySlots.get(1), (Slot) inventorySlots.get(0)));
                }
                if (item == Core.registry.item_craft) {
                    return FactorizationUtil.transferSlotToSlots(slot, Arrays.asList((Slot) inventorySlots.get(2)));
                }
                return FactorizationUtil.transferSlotToSlots(slot, Arrays.asList((Slot) inventorySlots.get(0)));
            }
            break;
        case STAMPER:
        case PACKAGER:
        case GRINDER:
            if (i >= 2) {
                return FactorizationUtil.transferSlotToSlots(slot, Arrays.asList((Slot) inventorySlots.get(0)));
            }
            break;
        case MIXER:
            if (i >= 8) {
                ArrayList<Slot> av = new ArrayList(4);
                for (int j = 0; j < 4; j++) {
                    av.add((Slot)inventorySlots.get(j));
                }
                return FactorizationUtil.transferSlotToSlots(slot, av);
            }
            break;
        case CRYSTALLIZER:
            if (i >= 8) {
                ArrayList<Slot> av = new ArrayList(6);
                for (int j = 0; j < 6; j++) {
                    av.add((Slot)inventorySlots.get(j));
                }
                return FactorizationUtil.transferSlotToSlots(slot, av);
            }
            break;
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
