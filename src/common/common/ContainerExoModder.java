package factorization.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import net.minecraft.src.Container;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.IInventory;
import net.minecraft.src.InventoryPlayer;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.Slot;
import factorization.api.Coord;
import factorization.api.IExoUpgrade;

public class ContainerExoModder extends Container {
    Coord benchLocation;
    InventoryPlayer inv;
    EntityPlayer player;
    public InventoryUpgrader upgrader;
    SlotExoArmor armorSlot;
    ArrayList<Slot> upgradeSlots = new ArrayList<Slot>(8), playerSlots = new ArrayList<Slot>(9*4);

    public class InventoryUpgrader implements IInventory {
        public ItemStack armor;
        public ItemStack[] upgrades = new ItemStack[9];

        ItemStack lastArmor;

        @Override
        public int getSizeInventory() {
            return 9;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            if (slot == 100) {
                return armor;
            }
            return upgrades[slot - 100];
        }

        @Override
        public ItemStack decrStackSize(int slot, int i) {
            ItemStack is = getStackInSlot(slot);
            if (is == null) {
                return null;
            }
            ItemStack ret = is.copy();
            i = Math.min(i, is.stackSize);
            ret.stackSize = i;
            is.stackSize -= i;
            if (is.stackSize <= 0) {
                is = null;
            }
            if (slot == 100) {
                armor = is;
            }
            else {
                upgrades[slot - 100] = is;
            }
            return ret;
        }

        @Override
        public ItemStack getStackInSlotOnClosing(int slot) {
            //don't shove stuff in, I think???
            //Not used...?
            return getStackInSlot(slot);
        }

        @Override
        public void setInventorySlotContents(int slot, ItemStack is) {
            if (slot == 100) {
                armor = is;
            }
            else {
                upgrades[slot - 100] = is;
            }
        }

        @Override
        public String getInvName() {
            return "Exo-Modder";
        }

        @Override
        public int getInventoryStackLimit() {
            return 1;
        }

        @Override
        public boolean isUseableByPlayer(EntityPlayer var1) {
            return true;
        }

        public void openChest() {
        }

        public void closeChest() {
        }

        @Override
        public void onInventoryChanged() {
        }

    }

    class SlotExoArmor extends Slot {
        ArrayList<Slot> upgradeSlots;

        public SlotExoArmor(IInventory par1iInventory, int par2, int par3, int par4,
                ArrayList<Slot> upgradeSlots) {
            super(par1iInventory, par2, par3, par4);
            this.upgradeSlots = upgradeSlots;
        }

        @Override
        public ItemStack decrStackSize(int par1) {
            //stuff that armor full
            ItemStack is = super.decrStackSize(par1);
            if (is == null) {
                return null;
            }
            if (!(is.getItem() instanceof ExoArmor)) {
                return is;
            }
            ExoArmor armor = (ExoArmor) is.getItem();
            for (int slot = 0; slot < armor.slotCount; slot++) {
                Slot upgradeSlot = upgradeSlots.get(slot);
                ItemStack upgrade = armor.getStackInSlot(is, slot);
                if (upgrade != null) {
                    continue;
                }
                ItemStack up = upgradeSlot.getStack();
                if (armor.isValidUpgrade(up)) {
                    armor.setStackInSlot(is, slot, upgradeSlot.decrStackSize(1));
                }
            }
            return is;
        }

        @Override
        public void putStack(ItemStack is) {
            //dump items out of the armor
            super.putStack(is);
            unpackArmor();
        }
        
        void packArmor() {
            super.putStack(decrStackSize(1));
        }

        void unpackArmor() {
            ItemStack is = getStack();
            if (is == null) {
                return;
            }
            if (!(is.getItem() instanceof ExoArmor)) {
                return;
            }
            ExoArmor armor = (ExoArmor) is.getItem();
            for (int slot = 0; slot < armor.slotCount; slot++) {
                Slot upgradeSlot = upgradeSlots.get(slot);
                ItemStack upgrade = armor.getStackInSlot(is, slot);
                if (upgrade == null || upgradeSlot.getHasStack()) {
                    continue;
                }
                upgradeSlot.putStack(upgrade);
                armor.setStackInSlot(is, slot, null);
            }
        }

    }

    class SlotExoUpgrade extends Slot {
        int exoIndex;
        public SlotExoUpgrade(int exoIndex, IInventory inv, int slotIndex, int posX, int posY) {
            super(inv, slotIndex, posX, posY);
            this.exoIndex = exoIndex;
        }
        
        @Override
        public boolean isItemValid(ItemStack is) {
            if (is == null) {
                return false;
            }
            if (!(is.getItem() instanceof IExoUpgrade)) {
                return false;
            }
            ItemStack armor = armorSlot.getStack();
            if (armor == null) {
                return false;
            }
            if (!(armor.getItem() instanceof ExoArmor)) {
                return false;
            }
            ExoArmor ma = (ExoArmor) armor.getItem();
            IExoUpgrade upgrade = (IExoUpgrade) is.getItem();
            if (!upgrade.canUpgradeArmor(armor, ma.armorType)) {
                return false;
            }
            return ma.slotCount > exoIndex;
        }
        
    }
    
    public ContainerExoModder(EntityPlayer player, Coord benchLocation) {
        this.benchLocation = benchLocation;
        this.inv = player.inventory;
        this.player = player;
        this.upgrader = new InventoryUpgrader();

        //player inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                Slot s = new Slot(inv, col + row * 9 + 9, 8 + col * 18, 116 + row * 18);
                this.addSlotToContainer(s);
                playerSlots.add(s);
            }
        }

        for (int col = 0; col < 9; col++) {
            Slot s = new Slot(inv, col, 8 + col * 18, 174);
            this.addSlotToContainer(s);
            playerSlots.add(s);
        }

        //slots for upgrades
        for (int col = 0; col < 8; col++) {
            Slot u = new SlotExoUpgrade(col, upgrader, 101 + col, 27 + col * 18, 7);
            this.addSlotToContainer(u);
            upgradeSlots.add(u);
        }
        //slot for the armor
        armorSlot = new SlotExoArmor(upgrader, 100, 7, 7, upgradeSlots);
        this.addSlotToContainer(armorSlot);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        if (benchLocation.getId() == Core.resource_id && ResourceType.EXOMODDER.is(benchLocation.getMd())) {
            Coord p = new Coord(player);
            return benchLocation.distance(p) <= 8;
        }
        return false;
    }

    @Override
    public void onCraftGuiClosed(EntityPlayer player) {
        super.onCraftGuiClosed(player);
        if (armorSlot.getHasStack()) {
            ItemStack is = armorSlot.decrStackSize(armorSlot.getStack().stackSize);
            player.dropPlayerItem(is);
        }
        for (Slot slot : armorSlot.upgradeSlots) {
            if (slot.getHasStack()) {
                ItemStack is = slot.decrStackSize(slot.getStack().stackSize);
                player.dropPlayerItem(is);
            }
        }
    }

    @Override
    public ItemStack transferStackInSlot(int slotNumber) {
        try {
            Slot clickedSlot = (Slot) inventorySlots.get(slotNumber);
            Iterable<Slot> targetSlots;
            if (clickedSlot.getStack() == null) {
                return null;
            }
            Item clickedItem = clickedSlot.getStack().getItem();
            if (clickedSlot == armorSlot) {
                //pack the armor
                armorSlot.packArmor();
                targetSlots = playerSlots;
            } else if (clickedSlot instanceof SlotExoUpgrade) {
                targetSlots = playerSlots;
            } else {
                if (clickedItem instanceof ExoArmor) {
                    targetSlots = (Iterable) Arrays.asList(armorSlot);
                } else if (clickedItem instanceof IExoUpgrade) {
                    targetSlots = upgradeSlots;
                } else {
                    return null;
                }
            }
            return FactorizationUtil.transferSlotToSlots(clickedSlot, targetSlots);
        } finally {
            armorSlot.unpackArmor();
        }
    }

}
