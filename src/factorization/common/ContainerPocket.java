package factorization.common;

import java.util.ArrayList;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.world.World;

public class ContainerPocket extends Container {
    final EntityPlayer player;
    final InventoryPlayer playerInv;
    final IInventory inv;
    InventoryCrafting craftMatrix = new InventoryCrafting(this, 3, 3);
    IInventory craftResult = new InventoryCraftResult();
    final World world;

    ArrayList<Slot> inventorySlots = new ArrayList();
    ArrayList<Slot> craftingSlots = new ArrayList();
    ArrayList<Slot> mainInvThenHotbarSlots = new ArrayList();
    Slot craftResultSlot;

    ItemStack fake_is;

    public ContainerPocket(EntityPlayer player) {
        this.player = player;
        this.world = player.worldObj;
        this.playerInv = player.inventory;
        this.inv = new InventoryProxy(playerInv);
        fake_is = new ItemStack(Core.registry.pocket_table, 0);
        craftResultSlot = addSlotToContainer(new RedirectedSlotCrafting(player, craftMatrix, craftResult, 208, 28));
        addPlayerSlots(inv);
        detectAndSendChanges();
        updateCraft();
    }
    
    void addPlayerSlots(IInventory inventoryplayer) {
        int invdx = 0, invdy = 0;
        int y = 3;
        ArrayList<Slot> hotbarSlots = new ArrayList();
        ArrayList<Slot> mainInvSlots = new ArrayList();
        for (int x = 0; x < 9; x++) {
            Slot slot = new Slot(inventoryplayer, x, invdx + 8 + x * 18, 4 + invdy + 8 + y * 18);
            addSlotToContainer(slot);
            hotbarSlots.add(slot);
        }
        int col_limit = 9 - 3;
        for (y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) {
                Slot slot = new Slot(inventoryplayer, x + y * 9 + 9, invdx + 8 + x * 18, invdy + 8 + y * 18);
                addSlotToContainer(slot);
                if (x >= col_limit) {
                    craftingSlots.add(slot);
                } else {
                    mainInvSlots.add(slot);
                }
            }
        }
        inventorySlots.addAll(hotbarSlots);
        inventorySlots.addAll(mainInvSlots);
        mainInvThenHotbarSlots.addAll(mainInvSlots);
        mainInvThenHotbarSlots.addAll(hotbarSlots);
    }
    
    @Override
    protected Slot addSlotToContainer(Slot slot) {
        return super.addSlotToContainer(slot);
    }
    

    class RedirectedSlotCrafting extends SlotCrafting {
        public RedirectedSlotCrafting(EntityPlayer player, IInventory craftMatrix, IInventory craftResult, int posX, int posY) {
            super(player, craftMatrix, craftResult, 0, posX, posY);
        }

        @Override
        public void onPickupFromSlot(EntityPlayer player, ItemStack grabbedStack) {
            super.onPickupFromSlot(player, grabbedStack);
            int i = 0;
            for (Slot slot : craftingSlots) {
                ItemStack repl = craftMatrix.getStackInSlot(i++);
                playerInv.setInventorySlotContents(slot.getSlotIndex(), repl);
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

        @Override
        public boolean isInvNameLocalized() {
            return false;
        }

        @Override
        public boolean isItemValidForSlot(int i, ItemStack itemstack) {
            return true;
        }
    }

    void updateMatrix() {
        int i = 0;
        for (Slot slot : craftingSlots) {
            craftMatrix.stackList[i++] = slot.getStack();
        }
    }

    public void updateCraft() {
        updateMatrix();
        ItemStack result = CraftingManager.getInstance().findMatchingRecipe(craftMatrix, world);
        craftResult.setInventorySlotContents(0, result);
    }
    
    @Override
    public void onCraftMatrixChanged(IInventory inv) {
        super.onCraftMatrixChanged(inv);
    }

    
    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return Core.registry.pocket_table.findPocket(player) != null;
    }

    public void executeCommand(Command cmd, byte arg) {
        switch (cmd) {
        default: return;
        case craftBalance:
            craftBalance();
            break;
        case craftFill:
            craftFill(arg);
            break;
        case craftClear:
            craftClear();
            break;
        case craftSwirl:
            craftSwirl();
            break;
        }
    }

    @Override
    protected void retrySlotClick(int par1, int par2, boolean par3, EntityPlayer par4EntityPlayer) {
        super.retrySlotClick(par1, par2, par3, par4EntityPlayer);
    }
    
    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int slotId) {
        for (Slot slot : inventorySlots) {
            if (slot.slotNumber == slotId) {
                return FactorizationUtil.transferSlotToSlots(player, slot, craftingSlots);
            }
        }
        for (Slot slot : craftingSlots) {
            if (slot.slotNumber == slotId) {
                return FactorizationUtil.transferSlotToSlots(player, slot, inventorySlots);
            }
        }
        if (craftResultSlot.slotNumber == slotId) {
            updateCraft();
            ItemStack res = craftResultSlot.getStack();
            if (res == null) {
                return null;
            }
            ItemStack held = null;
            for (int count = getCraftCount(res); count > 0; count--) {
                held = FactorizationUtil.tryTransferSlotToSlots(player, craftResultSlot, inventorySlots);
                if (held != null) {
                    break;
                }
                updateCraft();
                ItemStack newRes = craftResultSlot.getStack();
                if (newRes == null) {
                    break;
                }
                if (!FactorizationUtil.couldMerge(res, newRes)) {
                    break;
                }
                if (!FactorizationUtil.couldMerge(held, newRes)) {
                    break;
                }
                if (getCraftCount(newRes) <= 0) {
                    break;
                }
            }
            if (held != null) {
                ItemStack cursor = player.inventory.getItemStack();
                if (cursor == null) {
                     player.inventory.setItemStack(held);
                     if (player instanceof EntityPlayerMP && !player.worldObj.isRemote) {
                         EntityPlayerMP emp = (EntityPlayerMP) player;
                         emp.updateHeldItem();
                     }
                     held = null;
                } else if (FactorizationUtil.couldMerge(cursor, held)) {
                    int avail = cursor.getMaxStackSize() - cursor.stackSize;
                    int delta = Math.min(avail, held.stackSize);
                    held.stackSize -= delta;
                    cursor.stackSize += delta;
                    if (held.stackSize >= 0) {
                        held = null;
                    }
                }
                if (held != null && held.stackSize > 0 && !player.worldObj.isRemote) {
                    player.dropPlayerItem(held);
                }
            }
            detectAndSendChanges();
            return null;
        }
        return super.transferStackInSlot(player, slotId);
    }
    
    int getCraftCount(ItemStack res) {
        boolean hasEmpty = false;
        int space_to_fill = 0;
        for (Slot slot : inventorySlots) {
            ItemStack is = slot.getStack();
            if (is == null) {
                hasEmpty = true;
                continue;
            }
            if (FactorizationUtil.couldMerge(res, is)) {
                space_to_fill += is.getMaxStackSize() - is.stackSize;
            }
        }
        if (space_to_fill > 64) {
            space_to_fill = 64;
        }
        int ret = space_to_fill / res.stackSize;
        if (ret == 0 && hasEmpty) {
            return 64 / res.stackSize;
        }
        return ret;
    }

    void craftClear() {
        for (Slot slot : craftingSlots) {
            FactorizationUtil.transferSlotToSlots(player, slot, mainInvThenHotbarSlots);
        }
        updateMatrix();
    }
    
    //InventoryPlayer Slots:
    //09 10 11 12 13 14 15 16 17
    //18 19 20 21 22 23 24 25 26
    //27 28 29 30 31 32 33 34 35
    //00 01 02 03 04 05 06 07 08
    private static final int slots[] = {
        15, 16, 17,
        26,
        35, 34, 33,
        24,
    };
    private static final int slotsTwice[] = {
        15, 16, 17, 26, 35, 34, 33, 24,
        15, 16, 17, 26, 35, 34, 33, 24,
    };
    void craftSwirl() {
        boolean anyAction = false;
        for (int n = 0; n < 8; n++) {
            //1. find a stack with > 1 item in it
            //2. find an empty slot
            //3. move 1 item from former into latter
            boolean any = false;
            for (int slotIndexIndex = 0; slotIndexIndex < slots.length; slotIndexIndex++) {
                ItemStack is = playerInv.getStackInSlot(slots[slotIndexIndex]);
                if (is == null || is.stackSize <= 1) {
                    continue;
                }
                for (int probidex = slotIndexIndex; probidex < slotsTwice.length && probidex < slotIndexIndex + slots.length; probidex++) {
                    ItemStack empty = playerInv.getStackInSlot(slotsTwice[probidex]);
                    if (empty != null) {
                        continue;
                    }
                    playerInv.setInventorySlotContents(slotsTwice[probidex], is.splitStack(1));
                    any = true;
                    break;
                }
            }
            if (!any) {
                break;
            } else {
                anyAction = true;
            }
        }
        if (!anyAction) {
            //Did nothing. Shift the items around.
            ItemStack swapeh = playerInv.getStackInSlot(slots[slots.length - 1]);
            for (int i = 0; i < slots.length; i++) {
                ItemStack here = playerInv.getStackInSlot(slotsTwice[i]);
                playerInv.setInventorySlotContents(slotsTwice[i], swapeh);
                swapeh = here;
            }
            playerInv.setInventorySlotContents(slots[0], swapeh);
        }
        updateMatrix();
    }
    
    void craftBalance() {
        class Accumulator {
            ItemStack toMatch;
            int stackCount = 0;
            ArrayList<Integer> matchingSlots = new ArrayList<Integer>(9);

            public Accumulator(ItemStack toMatch, int slot) {
                this.toMatch = toMatch;
                stackCount = toMatch.stackSize;
                toMatch.stackSize = 0;
                matchingSlots.add(slot);
            }

            boolean add(ItemStack ta, int slot) {
                if (FactorizationUtil.couldMerge(toMatch, ta)) {
                    stackCount += ta.stackSize;
                    ta.stackSize = 0;
                    matchingSlots.add(slot);
                    return true;
                }
                return false;
            }
        }
        ArrayList<Accumulator> list = new ArrayList<Accumulator>(9);
        for (Slot s : craftingSlots) {
            int slot = s.getSlotIndex();
            ItemStack here = playerInv.getStackInSlot(slot);
            if (here == null || here.stackSize == 0) {
                continue;
            }
            boolean found = false;
            for (Accumulator acc : list) {
                if (acc.add(here, slot)) {
                    found = true;
                }
            }
            if (!found) {
                list.add(new Accumulator(here, slot));
            }
        }

        for (Accumulator acc : list) {
            int delta = acc.stackCount / acc.matchingSlots.size();
            // this should be incapable of being 0
            delta = Math.min(delta, 1); // ...we'll make sure anyways.
            for (int slot : acc.matchingSlots) {
                if (acc.stackCount <= 0) {
                    break;
                }
                playerInv.getStackInSlot(slot).stackSize = delta;
                acc.stackCount -= delta;
            }
            // we now may have a few left over, which we'll distribute
            while (acc.stackCount > 0) {
                for (int slot : acc.matchingSlots) {
                    if (acc.stackCount <= 0) {
                        break;
                    }
                    playerInv.getStackInSlot(slot).stackSize++;
                    acc.stackCount--;
                }
            }
        }

        updateMatrix();
    }

    void craftFill(byte slot) {
        final ItemStack toMove = playerInv.getStackInSlot(slot);
        if (toMove == null) {
            return;
        }
        for (Slot matrixSlot : craftingSlots) {
            if (toMove.stackSize <= 0) {
                break;
            }
            if (matrixSlot.getStack() == null) {
                matrixSlot.putStack(toMove.splitStack(1));
            }
        }
        playerInv.setInventorySlotContents(slot, FactorizationUtil.normalize(toMove));
        updateMatrix();
    }
}
