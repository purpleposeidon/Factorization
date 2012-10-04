package factorization.common;

import java.io.DataInput;
import java.io.IOException;

import net.minecraft.src.Block;
import net.minecraft.src.EntityItem;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.InventoryPlayer;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.Packet;
import net.minecraftforge.common.ISidedInventory;
import net.minecraftforge.common.ForgeDirection;
import static net.minecraftforge.common.ForgeDirection.*;
import factorization.api.Coord;
import factorization.common.NetworkFactorization.MessageType;

//Based off of technology found stockpiled in the long-abandoned dwarven fortress of "Nod Semor, the Toad of Unity".

//Special hack: If the item can stack && we have > 1, the input slot shall be split into 2 half stacks.
//If the item doesn't stack, then the input slot'll have stacksize = 0
//NO! We *can* have for the stackable items case have 1, and 1 even if there's less than 2 itemCount. Just clear it after the first one is emptied

public class TileEntityBarrel extends TileEntityFactorization {
    static final int normalBarrelSize = 64*64;
    static final int largeBarrelSize = 1024 * 64;
    // EMC of TNT is 964.
    static final int maxStackDrop = 64; // how many stacks required for an explosion; depends on item.maxStackSize
    static final float explosionStrength = 2.5F; //explosion base strength
    static final float explosionStrengthMin = 1.0F; //if items don't stack very high, explosion strength will be weakened

    // save these guys
    public ItemStack item;
    private ItemStack topStack; //always 0 unless nearly full
    private int middleCount;
    private ItemStack bottomStack; //always full unless nearly empty
    public int upgrade = 0;

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.BARREL;
    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.Barrel;
    }

    //These are some core barrel item-count manipulating functions.

    /**
     * @return # of items in barrel, including the top/bottom stacks.
     */
    public int getItemCount() {
        if (item == null) {
            return 0;
        }
        if (topStack == null || !itemMatch(topStack)) {
            topStack = item.copy();
            topStack.stackSize = 0;
        }
        if (bottomStack == null || !itemMatch(bottomStack)) {
            bottomStack = item.copy();
            bottomStack.stackSize = 0;
        }
        return topStack.stackSize + middleCount + bottomStack.stackSize;
    }
    
    public int getMaxSize() {
        if (upgrade == 0) {
            return normalBarrelSize;
        }
        return largeBarrelSize;
    }

    /**
     * redistribute count to the item stacks.
     */
    public void updateStacks() {
        if (item == null) {
            topStack = bottomStack = null;
            middleCount = 0;
            return;
        }
        int count = getItemCount();
        if (count == 0) {
            topStack = bottomStack = item = null;
            middleCount = 0;
            return;
        }
        int upperLine = getMaxSize() - item.getMaxStackSize();
        if (count > upperLine) {
            topStack = item.copy();
            topStack.stackSize = count - upperLine;
            count -= topStack.stackSize;
        }
        else {
            topStack.stackSize = 0;
        }
        bottomStack.stackSize = Math.min(item.getMaxStackSize(), count);
        count -= bottomStack.stackSize;
        middleCount = count;
    }

    public void changeItemCount(int delta) {
        middleCount = getItemCount() + delta;
        if (middleCount < 0) {
            throw new Error("Tried making item count negative! At " + getCoord());
        }
        if (middleCount > getMaxSize()) {
            System.err.println("Factorization barrel size " + middleCount + " is larger than the maximum, " + getMaxSize() + " at " + getCoord());
        }
        topStack = bottomStack = null;
        updateStacks();
        broadcastItemCount();
    }

    public void setItemCount(int val) {
        topStack = bottomStack = null;
        middleCount = val;
        changeItemCount(0);
    }

    private ItemStack makeStack(int count) {
        if (item == null) {
            throw new Error();
        }
        ItemStack ret = item.copy();
        ret.stackSize = count;
        assert ret.stackSize > 0 && ret.stackSize <= item.getMaxStackSize();
        return ret;
    }

    //end hard-core count manipulation
    @Override
    public int getSizeInventory() {
        // Top is the input slot; if we're typed it's an IS of size 0; if it's full, then it's however more will fit.
        // Bottom is the output slot; always as full as possible
        return 2;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (item == null) {
            cleanBarrel();
            return null;
        }
        if (slot == 0) {
            return bottomStack;
        }
        if (slot == 1) {
            return topStack;
        }
        return null;
    }

    static int sizeOf(ItemStack is) {
        if (is == null) {
            return 0;
        }
        return is.stackSize;
    }

    private boolean itemMatch(ItemStack is) {
        if (is == null || item == null) {
            return false;
        }
        item.stackSize = is.stackSize;
        return ItemStack.areItemStacksEqual(item, is);
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        ItemStack is = getStackInSlot(slot);
        if (is == null) {
            return null;
        }
        ItemStack ret = is.splitStack(amount);
        updateStacks();
        broadcastItemCount();
        return ret;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack is) {
        ItemStack old_item = item;
        taintBarrel(is);
        if (is != null && !itemMatch(is)) {
            //whoever's doing this is a douchebag. Forget about the item.
            new Exception().printStackTrace();
            return;
        }
        switch (slot) {
        case 0:
            bottomStack = is;
            break;
        case 1:
            topStack = is;
            break;
        }
        if (old_item != item) {
            broadcastItem();
        }
        broadcastItemCount();
    }

    @Override
    public String getInvName() {
        return "Barrel";
    }

    @Override
    public int getStartInventorySide(ForgeDirection side) {
        switch (side) {
        case DOWN:
            return 0; // -Y
        case UP:
            return 1; // +y
        default:
            return -1;
        }
    }

    @Override
    public int getSizeInventorySide(ForgeDirection side) {
        if (side == UP || side == DOWN) {
            return 1;
        }
        return 0;
    }

    void info(EntityPlayer entityplayer) {
        if (item == null && getItemCount() == 0) {
            Core.notify(entityplayer, getCoord(), "Empty");
        } else if (getItemCount() >= getMaxSize()) {
            Core.notify(entityplayer, getCoord(), "Full of %s", Core.proxy.translateItemStack(item));
        } else {
            Core.notify(entityplayer, getCoord(), "%s %s", "" + getItemCount(), Core.proxy.translateItemStack(item));
        }
    }

    void taintBarrel(ItemStack is) {
        if (is == null) {
            return;
        }
        if (getItemCount() != 0) {
            return;
        }
        if (is.getMaxStackSize() >= getMaxSize()) {
            return;
        }
        item = is.copy();
        broadcastItem();
    }

    void broadcastItem() {
        if (worldObj != null && !worldObj.isRemote) {
            Core.network.broadcastMessage(null, getCoord(), MessageType.BarrelItem, item);
        }
    }

    void broadcastItemCount() {
        if (worldObj != null && !worldObj.isRemote) {
            Core.network.broadcastMessage(null, getCoord(), MessageType.BarrelCount, getItemCount());
        }
    }

    void cleanBarrel() {
        if (getItemCount() == 0) {
            topStack = bottomStack = item = null;
            middleCount = 0;
        } else {
            assert item != null;
        }
    }
    
    @Override
    public boolean takeUpgrade(ItemStack is) {
        if (is.getItem() == Core.registry.barrel_enlarge && upgrade == 0) {
            upgrade = 1;
            return true;
        }
        return false;
    }
    
    long lastClick = -1000; //NOTE: This really should be player-specific!

    //* 			Left-Click		Right-Click
    //* No Shift:	Remove stack	Add item
    //* Shift:		Remove 1 item	Use item
    //* Double:						Add all but 1 item

    @Override
    public boolean activate(EntityPlayer entityplayer) {
        // right click: put an item in
        if (entityplayer.worldObj.isRemote) {
            return true;
        }
        if (worldObj.getWorldTime() - lastClick < 10 && item != null) {
            addAllItems(entityplayer);
            return true;
        }
        lastClick = worldObj.getWorldTime();
        int handslot = entityplayer.inventory.currentItem;
        if (handslot < 0 || handslot > 8) {
            return true;
        }

        ItemStack is = entityplayer.inventory.getStackInSlot(handslot);
        if (is == null) {
            info(entityplayer);
            return true;
        }

        if (is.isItemDamaged()) {
            if (getItemCount() == 0) {
                Core.notify(entityplayer, getCoord(), "Damaged items can not be stored");
            } else {
                info(entityplayer);
            }
            return true;
        }

        taintBarrel(is);

        if (!itemMatch(is)) {
            if (Core.proxy.translateItemStack(is).equals(Core.proxy.translateItemStack(item))) {
                Core.notify(entityplayer, getCoord(), "That item is different");
            } else {
                info(entityplayer);
            }
            return true;
        }
        int free = getMaxSize() - getItemCount();
        if (free <= 0) {
            info(entityplayer);
            return true;
        }
        int take = Math.min(free, is.stackSize);
        is.stackSize -= take;
        changeItemCount(take);
        if (is.stackSize == 0) {
            entityplayer.inventory.setInventorySlotContents(handslot, null);
        }
        return true;
    }

    @Override
    public void click(EntityPlayer entityplayer) {
        // left click: remove a stack
        if (entityplayer.worldObj.isRemote) {
            return;
        }
        if (getItemCount() == 0 || item == null) {
            info(entityplayer);
            return;
        }
        int to_remove = Math.min(item.getMaxStackSize(), getItemCount());
        if (entityplayer.isSneaking() && to_remove >= 1) {
            to_remove = 1;
        }
        ejectItem(makeStack(to_remove), false, entityplayer);
        changeItemCount(-to_remove);
        cleanBarrel();
    }

    void addAllItems(EntityPlayer entityplayer) {
        ItemStack hand = entityplayer.inventory.getStackInSlot(entityplayer.inventory.currentItem);
        if (hand != null) {
            taintBarrel(hand);
        }
        if (hand != null && !itemMatch(hand)) {
            if (Core.proxy.translateItemStack(hand).equals(Core.proxy.translateItemStack(item))) {
                Core.notify(entityplayer, getCoord(), "That item is different");
            } else {
                info(entityplayer);
            }
            return;
        }
        InventoryPlayer inv = entityplayer.inventory;
        int total_delta = 0;
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            int free_space = getMaxSize() - (getItemCount() + total_delta);
            if (free_space <= 0) {
                break;
            }
            ItemStack is = inv.getStackInSlot(i);
            if (is == null || is.stackSize <= 0) {
                continue;
            }
            if (!itemMatch(is)) {
                continue;
            }
            int toAdd = Math.min(is.stackSize, free_space);
            if (is == hand && toAdd > 1) {
                toAdd -= 1;
            }
            total_delta += toAdd;
            is.stackSize -= toAdd;
            if (is.stackSize <= 0) {
                inv.setInventorySlotContents(i, null);
            }
        }
        changeItemCount(total_delta);
        if (total_delta > 0) {
            Core.proxy.updatePlayerInventory(entityplayer);
        }
    }
    
    public boolean canLose() {
        return getItemCount() > maxStackDrop*item.getMaxStackSize();
    }

    @Override
    public void dropContents() {
        if (upgrade > 0) {
            FactorizationUtil.spawnItemStack(getCoord(), new ItemStack(Core.registry.barrel_enlarge));
        }
        if (item == null || getItemCount() <= 0) {
            return;
        }
        // not all items will be dropped if it's big enough to blow up.
        // This replaces lag-o-death with explosion-o-death
        int count = getItemCount();
        for (int i = 0; i < maxStackDrop; i++) {
            int to_drop;
            to_drop = Math.min(item.getMaxStackSize(), count);
            count -= to_drop;
            ejectItem(makeStack(to_drop), getItemCount() > 64 * 16, null);
            if (count <= 0) {
                break;
            }
        }
        if (count > 0) {
            broadcastMessage(null, MessageType.BarrelLoss, upgrade);
        }
        topStack = null;
        middleCount = 0;
        bottomStack = null;
    }
    
    static void spawnBreakParticles(Coord c, int upgrade) {
        if (upgrade > 0) {
            //ender particles. Also, drop the item.
            for (int theta = 0; theta < 360; theta += 1) {
                if (rand.nextInt(10) != 2) {
                    continue;
                }
                float speed = 2F;
                float start = -2F;
                double a = Math.toRadians(theta);
                //c.w.spawnParticle("portal", c.x+0.5F, c.y+0.5F, c.z+0.5F, 0, 0, 0);
                c.w.spawnParticle("portal", c.x+0.5F + Math.cos(a)*start, c.y, c.z+0.5F + Math.sin(a)*start, Math.cos(a)*speed, 0, Math.sin(a)*speed);
            }
        } else {
            //This technically shouldn't happen.
            c.w.spawnParticle("largesmoke", c.x+0.5F, c.y+0.5F, c.z+0.5F, 0, 0, 0);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        saveItem("item_type", tag, item);
        tag.setInteger("item_count", getItemCount());
        tag.setInteger("upgrade", upgrade);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        item = readItem("item_type", tag);
        setItemCount(tag.getInteger("item_count"));
        upgrade = tag.getInteger("upgrade");
    }

    @Override
    void sendFullDescription(EntityPlayer player) {
        super.sendFullDescription(player);
        broadcastItem();
        changeItemCount(0);
    }

    @Override
    public Packet getAuxillaryInfoPacket() {
        int ic = getItemCount();
        if (ic == 0) {
            return getDescriptionPacketWith(MessageType.BarrelDescription, ic, upgrade, 0xDEAD);
        }
        return getDescriptionPacketWith(MessageType.BarrelDescription, ic, upgrade, item);
    }

    @Override
    void doLogic() {
    }

    @Override
    public boolean canUpdate() {
        //XXX TODO: Barrels don't need this. (Just to make sure the MD is enforced, since an incorrect MD'd be so dangerous)
        //Can probably get rid of it in... well, several versions. Maybe in September?
        return true;
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        updateStacks();
    }

    @Override
    public boolean handleMessageFromServer(int messageType, DataInput input) throws IOException {
        if (super.handleMessageFromServer(messageType, input)) {
            return true;
        }
        switch (messageType) {
        case MessageType.BarrelCount:
            setItemCount(input.readInt());
            break;
        case MessageType.BarrelDescription:
            int i = input.readInt();
            upgrade = input.readInt();
            if (i > 0) {
                item = FactorizationHack.loadItemStackFromDataInput(input);
            }
            setItemCount(i);
            break;
        case MessageType.BarrelItem:
            item = FactorizationHack.loadItemStackFromDataInput(input);
            break;
        default:
            return false;
        }
        getCoord().dirty();
        return true;
    }
    
    @Override
    public String toString() {
        return "Barrel of " + getItemCount() + " " + item;
    }
}
