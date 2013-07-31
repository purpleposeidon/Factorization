package factorization.common;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;

import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.FzOrientation;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.DataInNBT;
import factorization.api.datahelpers.DataOutNBT;
import factorization.api.datahelpers.Share;
import factorization.common.NetworkFactorization.MessageType;
import factorization.notify.Notify;

public class TileEntityDayBarrel extends TileEntityFactorization {
    public ItemStack item;
    private ItemStack topStack;
    private int middleCount;
    private ItemStack bottomStack;
    private static final ItemStack DEFAULT_LOG = new ItemStack(Block.wood);
    private static final ItemStack DEFAULT_SLAB = new ItemStack(Block.wood);
    public ItemStack woodLog = DEFAULT_LOG.copy(), woodSlab = DEFAULT_SLAB.copy();
    
    public FzOrientation orientation = FzOrientation.FACE_UP_POINT_NORTH;
    public Type type = Type.NORMAL;
    
    private static final int maxStackDrop = 64*64*2;
    
    public static enum Type {
        NORMAL, SILKY, HOPPING, LARGER, STICKY, CREATIVE;
        
        private static Type[] value_list = values();
        public static Type valueOf(int ordinal) {
            if (ordinal < 0 || ordinal >= value_list.length) {
                return NORMAL;
            }
            return value_list[ordinal];
        }
        
        public static final int TYPE_COUNT = values().length;
    }
    private int last_mentioned_count = -1;
    
    //Factoryish stuff
    @Override
    public FactoryType getFactoryType() {
        return FactoryType.DAYBARREL;
    }
    
    @Override
    public BlockClass getBlockClass() {
        return BlockClass.Barrel;
    }
    
    @Override
    byte getExtraInfo() {
        return (byte) orientation.ordinal();
    }
    
    @Override
    void useExtraInfo(byte b) {
        orientation = FzOrientation.getOrientation(b);
    }

    @Override
    byte getExtraInfo2() {
        return (byte) type.ordinal();
    }
    
    @Override
    void useExtraInfo2(byte b) {
        type = Type.valueOf(b);
    }
    
    
    
    //Saving
    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        putData(new DataOutNBT(tag));
    }
    
    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        putData(new DataInNBT(tag));
        last_mentioned_count = getItemCount();
    }
    
    void putData(DataHelper data) {
        try {
            if (data.isReader()) {
                item = new ItemStack(0, 0, 0);
            }
            item = data.as(Share.VISIBLE, "item").putItemStack(item);
            int count = data.as(Share.VISIBLE, "count").putInt(getItemCount());
            orientation = data.as(Share.VISIBLE, "dir").putFzOrientation(orientation);
            if (data.isReader()) {
                setItemCount(count);
            }
            woodLog = data.as(Share.VISIBLE, "log").putItemStack(woodLog);
            woodSlab = data.as(Share.VISIBLE, "slab").putItemStack(woodSlab);
            type = data.as(Share.VISIBLE, "type").putEnum(type);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    
    
    //Barrel-type Code
    @Override
    public boolean canUpdate() {
        return type == Type.HOPPING;
    }
    
    @Override
    void doLogic() {
        if (type != Type.HOPPING) {
            return;
        }
        needLogic();
        //TODO: Hop!
    }
    
    public int getItemCount() {
        if (item == null) {
            return 0;
        }
        if (type == Type.CREATIVE) {
            return 32*item.getMaxStackSize();
        }
        if (topStack == null || !itemMatch(topStack)) {
            topStack = item.copy();
            topStack.stackSize = 0;
        }
        if (bottomStack == null || !itemMatch(bottomStack)) {
            bottomStack = item.copy();
            bottomStack.stackSize = 0;
        }
        int ret = bottomStack.stackSize + middleCount + topStack.stackSize;
        return ret;
    }
    
    public int getMaxSize() {
        if (item == null) {
            return 0;
        }
        int size = item.getMaxStackSize()*64;
        if (type == Type.LARGER) {
            size *= 2;
        }
        return size;
    }
    
    private boolean itemMatch(ItemStack is) {
        if (is == null || item == null) {
            return false;
        }
        return FactorizationUtil.couldMerge(item, is);
    }
    
    boolean taint(ItemStack is) {
        if (is == null && item == null) {
            return true;
        }
        if (is == null) {
            return false;
        }
        if (item == null) {
            item = is.copy();
            item.stackSize = 0;
            return true;
        }
        return false;
    }
    
    boolean isTop(ForgeDirection d) {
        return d == orientation.top;
    }
    
    boolean isBottom(ForgeDirection d) {
        return d == orientation.top.getOpposite();
    }
    
    boolean isBack(ForgeDirection d) {
        return d == orientation.facing.getOpposite();
    }
    
    boolean isBackOrBottom(ForgeDirection d) {
        return d == orientation.facing.getOpposite() || d == orientation.top.getOpposite();
    }
    
    public void setItemCount(int val) {
        topStack = bottomStack = null;
        middleCount = val;
        changeItemCount(0);
    }
    
    public void changeItemCount(int delta) {
        middleCount = getItemCount() + delta;
        if (middleCount < 0) {
            Core.logSevere("Tried to set the item count to negative value " + middleCount + " at " + getCoord());
            middleCount = 0;
            item = null;
        }
        if (middleCount > getMaxSize()) {
            Core.logSevere("Factorization barrel size " + middleCount + " is larger than the maximum, " + getMaxSize() + " at " + getCoord());
        }
        topStack = bottomStack = null;
        updateStacks();
        updateClients(MessageType.BarrelCount);
        onInventoryChanged();
    }
    
    @Override
    public void onPlacedBy(EntityPlayer player, ItemStack is, int side, float hitX, float hitY, float hitZ) {
        ForgeDirection facing = ForgeDirection.getOrientation(side);
        double u = 0.5, v = 0.5; //We pick the axiis based on which side gets clicked
        switch (facing) {
        case DOWN:
            u = 1 - hitX;
            v = hitZ;
            break;
        case UP:
            u = hitX;
            v = hitZ;
            break;
        case NORTH:
            u = hitX;
            v = hitY;
            break;
        case SOUTH:
            u = 1 - hitX;
            v = hitY;
            break;
        case WEST:
            u = 1 - hitZ;
            v = hitY;
            break;
        case EAST:
            u = hitZ;
            v = hitY;
            break;
        }
        u -= 0.5;
        v -= 0.5;
        double angle = Math.toDegrees(Math.atan2(v, u)) + 180;
        angle = (angle + 45) % 360;
        int pointy = (int) (angle/90);
        pointy = (pointy + 1) % 4;
        
        FzOrientation fo = FzOrientation.fromDirection(facing);
        for (int X = 0; X < pointy; X++) {
            fo = fo.getNextRotationOnFace();
        }
        if (FactorizationUtil.determineOrientation(player) >= 2 /* player isn't looking straight down */
                && side < 2 /* and the side is the bottom */) {
            side = FactorizationUtil.determineOrientation(player);
            fo = FzOrientation.fromDirection(ForgeDirection.getOrientation(side).getOpposite());
            FzOrientation perfect = fo.pointTopTo(ForgeDirection.UP);
            if (perfect != FzOrientation.UNKNOWN) {
                fo = perfect;
            }
        }
        orientation = fo;
        loadFromStack(is);
    }
    
    @Override
    public void loadFromStack(ItemStack is) {
        super.loadFromStack(is);
        woodLog = getLog(is);
        woodSlab = getSlab(is);
        type = getUpgrade(is);
    }
    
    
    
    //Network stuff
    
    Packet getPacket(int messageType) {
        if (messageType == NetworkFactorization.MessageType.BarrelItem) {
            return Core.network.TEmessagePacket(getCoord(), messageType, NetworkFactorization.nullItem(item), getItemCount());
        } else if (messageType == NetworkFactorization.MessageType.BarrelCount) {
            return Core.network.TEmessagePacket(getCoord(), messageType, getItemCount());
        } else {
            new IllegalArgumentException("bad MessageType: " + messageType).printStackTrace();
            return null;
        }
    }
    
    void updateClients(int messageType) {
        if (worldObj == null || worldObj.isRemote) {
            return;
        }
        broadcastMessage(null, getPacket(messageType));
    }
    
    @Override
    public Packet getDescriptionPacket() {
        int count = getItemCount();
        ItemStack theItem = item;
        return getDescriptionPacketWith(MessageType.BarrelDescription,
                NetworkFactorization.nullItem(theItem),
                count,
                woodLog,
                woodSlab,
                (byte) orientation.ordinal(),
                (byte) type.ordinal());
    }
    
    @Override
    public boolean handleMessageFromServer(int messageType, DataInputStream input) throws IOException {
        if (super.handleMessageFromServer(messageType, input)) {
            return true;
        }
        if (messageType == MessageType.BarrelDescription) {
            item = FactorizationUtil.readStack(input);
            setItemCount(input.readInt());
            woodLog = FactorizationUtil.readStack(input);
            woodSlab = FactorizationUtil.readStack(input);
            orientation = FzOrientation.getOrientation(input.readByte());
            type = Type.valueOf(input.readByte());
            return true;
        }
        if (messageType == MessageType.BarrelCount) {
            setItemCount(input.readInt());
            return true;
        }
        if (messageType == MessageType.BarrelItem) {
            item = FactorizationUtil.readStack(input);
            setItemCount(input.readInt());
            return true;
        }
        return false;
    }
    
    void cleanBarrel() {
        if (getItemCount() == 0) {
            topStack = bottomStack = item = null;
            middleCount = 0;
        }
    }
    
    //Inventory code
    
    @Override
    public void onInventoryChanged() {
        updateStacks();
        int c = getItemCount();
        if (c != last_mentioned_count) {
            if (last_mentioned_count*c == 0) {
                //One of them was 0
                updateClients(MessageType.BarrelItem);
            } else {
                updateClients(MessageType.BarrelCount);
            }
            last_mentioned_count = c;
        }
    }
    
    @Override
    public int getSizeInventory() {
        return 2;
    }
    
    private void updateStacks() {
        if (item == null) {
            topStack = bottomStack = null;
            middleCount = 0;
            return;
        }
        int count = getItemCount();
        if (type == Type.STICKY) {
            count--;
            if (count < 0) {
                return;
            }
        }
        int upperLine = getMaxSize() - item.getMaxStackSize();
        topStack = item.copy();
        bottomStack = item.copy();
        if (count > upperLine) {
            topStack.stackSize = count - upperLine;
            count -= topStack.stackSize;
        }
        else {
            topStack.stackSize = 0;
        }
        bottomStack.stackSize = Math.min(item.getMaxStackSize(), count);
        count -= bottomStack.stackSize;
        middleCount = count;
        if (type == Type.STICKY) {
            middleCount++;
        }
    }
    
    @Override
    public ItemStack getStackInSlot(int i) {
        cleanBarrel();
        if (i == 0) {
            return topStack;
        }
        if (i == 1) {
            return bottomStack;
        }
        return null;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack is) {
        if (is != null && !taint(is)) {
            Core.logWarning("Bye bye, %s", is);
            Thread.dumpStack();
            return;
        }
        if (slot == 0) {
            topStack = is;
        } else if (slot == 1) {
            bottomStack = is;
        } else {
            Core.logWarning("Say goodbye, %s !", is);
            Thread.dumpStack();
        }
    }

    @Override
    public String getInvName() {
        return "Barrel";
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack is) {
        if (i != 0) {
            return false;
        }
        if (item == null) {
            return true;
        }
        if (is != null && is.isItemDamaged()) {
            return false;
        }
        return itemMatch(is);
    }
    
    @Override
    public boolean canExtractItem(int slot, ItemStack itemstack, int side) {
        ForgeDirection d = ForgeDirection.getOrientation(side);
        return isTop(d.getOpposite());
    }

    
    private static final int[] top_slot = new int[] {0}, bottom_slot = new int[] {1}, no_slots = new int[] {};
    @Override
    public int[] getAccessibleSlotsFromSide(int i) {
        ForgeDirection d = ForgeDirection.getOrientation(i);
        if (isTop(d)) {
            return top_slot;
        }
        if (isBackOrBottom(d)) {
            return bottom_slot;
        }
        return no_slots;
    }
    
    //Interaction
    
    long lastClick = -1000; //NOTE: This really should be player-specific!

    //* 			Left-Click		Right-Click
    //* No Shift:	Remove stack	Add item
    //* Shift:		Remove 1 item	Use item
    //* Double:						Add all but 1 item

    @Override
    public boolean activate(EntityPlayer entityplayer, ForgeDirection side) {
        // right click: put an item in
        if (entityplayer.worldObj.isRemote) {
            return true;
        }
        if (worldObj.getTotalWorldTime() - lastClick < 10 && item != null) {
            addAllItems(entityplayer);
            return true;
        }
        lastClick = worldObj.getTotalWorldTime();
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
                Notify.send(entityplayer, getCoord(), "No storing damaged items");
            } else {
                info(entityplayer);
            }
            return true;
        }

        boolean veryNew = taint(is);

        if (!itemMatch(is)) {
            if (Core.getTranslationKey(is.getItem()).equals(Core.getTranslationKey(item))) {
                Notify.send(entityplayer, getCoord(), "That item is different");
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
        if (veryNew) {
            updateClients(MessageType.BarrelItem);
        }
        if (is.stackSize == 0) {
            entityplayer.inventory.setInventorySlotContents(handslot, null);
        }
        return true;
    }
    
    void addAllItems(EntityPlayer entityplayer) {
        ItemStack hand = entityplayer.inventory.getStackInSlot(entityplayer.inventory.currentItem);
        if (hand != null) {
            taint(hand);
        }
        if (hand != null && !itemMatch(hand)) {
            if (Core.getTranslationKey(hand).equals(Core.getTranslationKey(item))) {
                Notify.send(entityplayer, getCoord(), "That item is different");
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
    
    
    private static int last_hit_side = -1;
    @ForgeSubscribe
    public void clickEvent(PlayerInteractEvent event) {
        if (event.entityPlayer.worldObj.isRemote) {
            return;
        }
        last_hit_side = event.face;
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
        if (to_remove > 1 && to_remove == getItemCount()) {
            to_remove--;
        }
        ejectItem(makeStack(to_remove), false, entityplayer, last_hit_side);
        changeItemCount(-to_remove);
        cleanBarrel();
        last_hit_side = -1;
    }
    
    void info(EntityPlayer entityplayer) {
        if (item == null && getItemCount() == 0) {
            Notify.send(entityplayer, getCoord(), "Empty");
        } else if (getItemCount() >= getMaxSize()) {
            Notify.withItem(item);
            Notify.send(entityplayer, getCoord(), "Full of {ITEM_NAME}{ITEM_INFOS_NEWLINE}");
        } else {
            String count = "" + getItemCount();
            if (type == Type.CREATIVE) {
                count = "Infinite";
            }
            Notify.withItem(item);
            Notify.send(entityplayer, getCoord(), "%s {ITEM_NAME}{ITEM_INFOS_NEWLINE}", count);
        }
    }
    
    private ItemStack makeStack(int count) {
        if (item == null) {
            return null;
        }
        ItemStack ret = item.copy();
        ret.stackSize = count;
        assert ret.stackSize > 0 && ret.stackSize <= item.getMaxStackSize();
        return ret;
    }


    //Misc junk
    
    @Override
    public int getComparatorValue(ForgeDirection side) {
        int count = getItemCount();
        if (count == 0) {
            return 0;
        }
        int max = getMaxSize();
        if (count == max) {
            return 15;
        }
        float v = count/(float)max;
        return (int) Math.max(1, v*14);
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public Icon getIcon(ForgeDirection dir) {
        if (dir.offsetY != 0) {
            return woodSlab.getItem().getIcon(woodSlab, 0);
        }
        Item theItem = woodLog.getItem();
        if (theItem instanceof ItemBlock) {
            Block b = Block.blocksList[((ItemBlock) theItem).getBlockID()];
            return b.getIcon(2, woodLog.getItemDamage());
        }
        return theItem.getIcon(woodLog, 2);
    }
    
    @Override
    public void dropContents() {
        if (type == Type.CREATIVE) {
            return;
        }
        if (item == null || getItemCount() <= 0 ) {
            return;
        }
        int count = getItemCount();
        for (int i = 0; i < maxStackDrop; i++) {
            int to_drop;
            to_drop = Math.min(item.getMaxStackSize(), count);
            count -= to_drop;
            ejectItem(makeStack(to_drop), getItemCount() > 64 * 16, null, -1);
            if (count <= 0) {
                break;
            }
        }
        topStack = null;
        middleCount = 0;
        bottomStack = null;
    }
    
    public boolean canLose() {
        return item == null ? false : getItemCount() > maxStackDrop*item.getMaxStackSize();
    }
    
    public static ItemStack makeBarrel(Type type, ItemStack log, ItemStack slab) {
        ItemStack barrel_item = new ItemStack(Core.registry.daybarrel);
        barrel_item = addUpgrade(barrel_item, type);
        NBTTagCompound tag = FactorizationUtil.getTag(barrel_item);
        tag.setTag("log", FactorizationUtil.item2tag(log));
        tag.setTag("slab", FactorizationUtil.item2tag(slab));
        barrel_item.setItemDamage(type.ordinal());
        return barrel_item;
    }
    
    public static ArrayList<ItemStack> barrel_items = new ArrayList();
    private static ItemStack make(Type type, ItemStack log, ItemStack slab) {
        ItemStack ret = makeBarrel(type, log, slab);
        barrel_items.add(ret);
        return ret;
    }
    
    static {
        make(Type.CREATIVE, new ItemStack(Block.blockDiamond), new ItemStack(Block.dragonEgg));
    }
    
    private static ItemStack silkTouch = Item.enchantedBook.getEnchantedItemStack(new EnchantmentData(Enchantment.silkTouch, 1));
    public static void makeRecipe(ItemStack log, ItemStack slab) {
        ItemStack normal = make(Type.NORMAL, log, slab);
        Core.registry.recipe(normal,
                "W-W",
                "W W",
                "WWW",
                'W', log,
                '-', slab);
        
        //Note: Don't add creative. That'd be bad.
        //And don't add normal, since that'd be silly.
        Core.registry.recipe(make(Type.SILKY, log, slab),
                "#",
                "B",
                "#",
                '#', silkTouch,
                'B', normal);
        Core.registry.recipe(make(Type.HOPPING, log, slab),
                "V",
                "B",
                "V",
                'V', Block.hopperBlock,
                'B', normal);
        Core.registry.recipe(make(Type.LARGER, log, slab),
                "B",
                "V",
                "B",
                'B', normal,
                'V', Block.hopperBlock);
        Core.registry.recipe(make(Type.STICKY, log, slab),
                "O",
                "B",
                'O', Item.slimeBall,
                'B', normal);
    }
    
    static Type getUpgrade(ItemStack is) {
        NBTTagCompound tag = is.getTagCompound();
        if (tag == null) {
            return Type.NORMAL;
        }
        String name = tag.getString("type");
        if (name == null || name == "") {
            return Type.NORMAL;
        }
        try {
            return Type.valueOf(name);
        } catch (IllegalArgumentException e) {
            Core.logWarning("%s has invalid barrel Type %s", is, name);
            e.printStackTrace();
            return Type.NORMAL;
        }
    }
    
    public static ItemStack getLog(ItemStack is) {
        return get(is, "log", DEFAULT_LOG);
    }
    
    public static ItemStack getSlab(ItemStack is) {
        return get(is, "slab", DEFAULT_SLAB);
    }
    
    private static ItemStack get(ItemStack is, String name, ItemStack default_) {
        NBTTagCompound tag = is.getTagCompound();
        if (tag == null) {
            return default_.copy();
        }
        tag = tag.getCompoundTag(name);
        if (tag == null) {
            return default_.copy();
        }
        ItemStack ret = ItemStack.loadItemStackFromNBT(tag);
        if (ret == null) {
            return default_.copy();
        }
        return ret;
    }
    
    static ItemStack addUpgrade(ItemStack barrel, Type upgrade) {
        if (upgrade == Type.NORMAL) {
            return barrel;
        }
        barrel = barrel.copy();
        NBTTagCompound tag = FactorizationUtil.getTag(barrel);
        tag.setString("type", upgrade.toString());
        return barrel;
    }
    
    @Override
    public boolean rotate(ForgeDirection axis) {
        if (axis == orientation.facing || axis.getOpposite() == orientation.facing) {
            orientation = orientation.getNextRotationOnFace();
            return true;
        }
        if (axis == orientation.top || axis.getOpposite() == orientation.top) {
            orientation = orientation.getNextRotationOnTop();
            return true;
        }
        return false;
    }
    
    @Override
    public ItemStack getDroppedBlock() {
        ItemStack is = makeBarrel(type, woodLog, woodSlab);
        return is;
    }
}
