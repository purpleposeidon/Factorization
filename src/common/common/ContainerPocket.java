package factorization.common;

import java.util.Arrays;
import java.util.List;

import net.minecraft.src.ContainerWorkbench;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.IInventory;
import net.minecraft.src.InventoryPlayer;
import net.minecraft.src.ItemStack;
import net.minecraft.src.Slot;
import net.minecraft.src.SlotCrafting;

public class ContainerPocket extends ContainerWorkbench {

    EntityPlayer player;
    InventoryProxy inv;
    SlotCrafting slotCrafting;

    static final List<Integer> craftArea = Arrays.asList( //
    // these our Container slots
    6, 7, 8, 15, 16, 17, 24, 25, 26);
    static final List<Integer> inventoryArea = Arrays.asList( //
    // these are Container slots
    0, 1, 2, 3, 4, 5, // top row
            9, 10, 11, 12, 13, 14, // middle row
            18, 19, 20, 21, 22, 23, // bottom row
            27, 28, 29, 30, 31, 32, 33, 34, 35, 36 // hotbar
    );
    static final List<Integer> playerNormalInvSlots = Arrays.asList( //
    // these are Inventory slots
    9, 10, 11, 12, 13, 14, //
            18, 19, 20, 21, 22, 23, //
            27, 28, 29, 30, 31, 32, //
            0, 1, 2, 3, 4, 5, 6, 7, 8);
    static final List<Integer> playerNormalInvSlotsAlt = Arrays.asList( //
    // these are Inventory slots
    0, 1, 2, 3, 4, 5, 6, 7, 8,
            9, 10, 11, 12, 13, 14, //
            18, 19, 20, 21, 22, 23, //
            27, 28, 29, 30, 31, 32);

    static final List<Integer> playerCraftInvSlots = Arrays.asList( //
    // these are Inventory slots
    15, 16, 17, //
            24, 25, 26, //
            33, 34, 35);

    public ContainerPocket(EntityPlayer player) {
        super(player.inventory, player.worldObj, 0, 0, 0);
        this.player = player;
        this.inv = new InventoryProxy(player.inventory);

        // undo some stuff super did
        inventorySlots.clear();
        inventoryItemStacks.clear();

        addPlayerSlots(inv);
        slotCrafting = new RedirectedSlotCrafting(player, this.craftMatrix, this.craftResult, 0,
                205 + 3, 25 + 3);
        this.addSlotToContainer(slotCrafting);
        updateCraftingResults();
        updateCraft();
    }

    class RedirectedSlotCrafting extends SlotCrafting {
        public RedirectedSlotCrafting(EntityPlayer par1EntityPlayer, IInventory par2iInventory,
                IInventory par3iInventory, int par4, int par5, int par6) {
            super(par1EntityPlayer, par2iInventory, par3iInventory, par4, par5, par6);
        }

        @Override
        public void onPickupFromSlot(EntityPlayer player, ItemStack par1ItemStack) {
            updateMatrix();
            for (int i : craftArea) {
                i += 9;
                player.inventory.setInventorySlotContents(i, Core.registry.fake_is);
            }
            super.onPickupFromSlot(player, par1ItemStack);
            int j = 0;
            for (int i : craftArea) {
                i += 9;
                player.inventory.setInventorySlotContents(i, craftMatrix.getStackInSlot(j));
                j++;
            }
            updateCraft();
        }
    }

    class InventoryProxy implements IInventory {
        IInventory src;

        public InventoryProxy(IInventory src) {
            this.src = src;
        }

        int remapSlotId(int i) {
            return i;
        }

        @Override
        public int getSizeInventory() {
            return src.getSizeInventory();
        }

        @Override
        public ItemStack getStackInSlot(int var1) {
            return src.getStackInSlot(remapSlotId(var1));
        }

        @Override
        public ItemStack decrStackSize(int var1, int var2) {
            ItemStack ret = src.decrStackSize(remapSlotId(var1), var2);
            updateCraft();
            return ret;
        }

        @Override
        public ItemStack getStackInSlotOnClosing(int var1) {
            return src.getStackInSlotOnClosing(remapSlotId(var1));
        }

        @Override
        public void setInventorySlotContents(int var1, ItemStack var2) {
            src.setInventorySlotContents(remapSlotId(var1), var2);
            updateCraft();
        }

        @Override
        public String getInvName() {
            return src.getInvName();
        }

        @Override
        public int getInventoryStackLimit() {
            return src.getInventoryStackLimit();
        }

        @Override
        public void onInventoryChanged() {
            src.onInventoryChanged();
            updateCraft();
        }

        @Override
        public boolean isUseableByPlayer(EntityPlayer var1) {
            return src.isUseableByPlayer(var1);
        }

        @Override
        public void openChest() {
            src.openChest();
        }

        @Override
        public void closeChest() {
            src.closeChest();
        }
    }

    void addPlayerSlots(IInventory inventoryplayer) {
        int invdx = 0, invdy = 0;
        int col_limit = 9 - 3;
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) {
                addSlotToContainer(new Slot(inventoryplayer, x + y * 9 + 9, invdx + 8 + x * 18, invdy + 8 + y
                        * 18));
            }
        }

        int y = 3;
        for (int x = 0; x < 9; x++) {
            addSlotToContainer(new Slot(inventoryplayer, x, invdx + 8 + x * 18, 4 + invdy + 8 + y * 18));
        }
    }

    // @Override
    // public void updateCraftingResults() {
    // updateCraft();
    // super.updateCraftingResults();
    // }

    void updateMatrix() {
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                ItemStack is = player.inventory.getStackInSlot(6 + x + (1 + y) * 9);
                craftMatrix.setInventorySlotContents(x + y * 3, is);
            }
        }
    }

    public void updateCraft() {
        updateMatrix();
        onCraftMatrixChanged(player.inventory);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return Core.registry.pocket_table.findPocket(player) != null;
    }

    @Override
    public void putStackInSlot(int par1, ItemStack par2ItemStack) {
        super.putStackInSlot(par1, par2ItemStack);
        updateCraft();
    }

    @Override
    //-- stupid server.
    public void putStacksInSlots(ItemStack[] par1ArrayOfItemStack) {
        // super.putStacksInSlots(par1ArrayOfItemStack);
        for (int var2 = 0; var2 < par1ArrayOfItemStack.length; ++var2) {
            this.getSlot(var2).putStack(par1ArrayOfItemStack[var2]);
        }
        // NOTE: above code is from super
        updateCraft();
    }

    @Override
    public void onCraftGuiClosed(EntityPlayer par1EntityPlayer) {
        //NOTE: This is super.super.onCraftGuiClosed(par1EntityPlayer)
        InventoryPlayer var2 = par1EntityPlayer.inventory;

        if (var2.getItemStack() != null) {
            par1EntityPlayer.dropPlayerItem(var2.getItemStack());
            var2.setItemStack((ItemStack) null);
        }
    }
    
    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int i) {
        ItemStack ret = null;

        if (i == 36) {
            // crafting result slot
            transferCraftResults(player);
        } else if (craftArea.contains(i)) {
            ret = FactorizationUtil.transferStackToArea(player.inventory, i + 9, player.inventory,
                    playerNormalInvSlots);
        } else if (inventoryArea.contains(i)) {
            if (i >= 9 * 3) {
                // hotbar
                ret = FactorizationUtil.transferStackToArea(player.inventory, i - 9 * 3,
                        player.inventory, playerCraftInvSlots);
            } else {
                // main inv
                ret = FactorizationUtil.transferStackToArea(player.inventory, i + 9,
                        player.inventory, playerCraftInvSlots);
            }
        }

        updateCraft();
        return null;
    }

    boolean spaceFreeFor(ItemStack res) {
        int freeSpace = 0;
        for (int i : inventoryArea) {
            if (freeSpace >= res.getMaxStackSize()) {
                return true;
            }
            ItemStack is = inv.getStackInSlot(i);
            if (is == null) {
                freeSpace += res.getMaxStackSize();
                continue;
            }
            if (is.isItemEqual(res)) {
                freeSpace += is.getMaxStackSize() - is.stackSize;
            }
        }
        return freeSpace >= res.getMaxStackSize();
    }

    void transferCraftResults(EntityPlayer player) {
        // move 1 or (maxStackSize - 1)
        ItemStack res = craftResult.getStackInSlot(0);
        if (res == null) {
            return;
        }

        if (player.inventory.getItemStack() != null) {
            int would_fit = 0;
            for (int i : playerNormalInvSlots) {
                ItemStack is = inv.getStackInSlot(i);
                if (is == null) {
                    would_fit += res.getMaxStackSize();
                    continue;
                }
                if (is.isItemEqual(res)) {
                    would_fit += is.getMaxStackSize() - is.stackSize;
                }
            }
            if (would_fit < res.stackSize) {
                return;
            }
        }

        int materialRemaining = res.getMaxStackSize();
        // figure out how many times we can craft
        for (int i : playerCraftInvSlots) {
            ItemStack is = inv.getStackInSlot(i);
            if (is == null || is.getMaxStackSize() == 1) {
                continue;
            }
            materialRemaining = Math.min(is.stackSize, materialRemaining);
        }

        int productRemaining = res.getMaxStackSize();

        while (spaceFreeFor(res) && productRemaining >= 0 && materialRemaining > 0) {
            res = craftResult.getStackInSlot(0);
            if (res == null || res.stackSize <= 0) {
                break;
            }
            productRemaining -= res.stackSize;
            materialRemaining--;
            assert res != null;
            slotCrafting.onPickupFromSlot(player, res);
            craftResult.setInventorySlotContents(0, res);
            ItemStack remainder = FactorizationUtil.transferStackToArea(craftResult, 0, player.inventory, playerNormalInvSlotsAlt);

            updateCraft();
            if (remainder != null && remainder.stackSize != 0) {
                player.inventory.setItemStack(remainder);
                break;
            }
            if (materialRemaining == 1) {
                break;
            }
        }
    }
}
