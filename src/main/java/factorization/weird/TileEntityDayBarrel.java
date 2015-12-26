package factorization.weird;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Coord;
import factorization.api.FzOrientation;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.common.FactoryType;
import factorization.notify.Notice;
import factorization.notify.NoticeUpdater;
import factorization.rendersorting.ISortableRenderer;
import factorization.rendersorting.RenderSorter;
import factorization.shared.*;
import factorization.shared.NetworkFactorization.MessageType;
import factorization.util.*;
import factorization.util.InvUtil.FzInv;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.ChunkEvent;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;

public class TileEntityDayBarrel extends TileEntityFactorization implements ISortableRenderer<TileEntityDayBarrel> {
    public ItemStack item;
    private ItemStack topStack;
    private int middleCount;
    private ItemStack bottomStack;
    private static final ItemStack DEFAULT_LOG = new ItemStack(Blocks.log);
    private static final ItemStack DEFAULT_SLAB = new ItemStack(Blocks.planks);
    public ItemStack woodLog = DEFAULT_LOG.copy(), woodSlab = DEFAULT_SLAB.copy();
    int display_list = -1;
    boolean should_use_display_list = true;
    
    public FzOrientation orientation = FzOrientation.FACE_UP_POINT_NORTH;
    public Type type = Type.NORMAL;
    Object notice_target = this;
    
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
    public void putData(DataHelper data) throws IOException {
        item = data.as(Share.VISIBLE, "item").putItemStack(item);
        int count;
        try {
            count = data.as(Share.VISIBLE, "count").putInt(getItemCount());
        } catch (Throwable t) {
            t.printStackTrace();
            count = 0;
        }
        orientation = data.as(Share.VISIBLE, "dir").putFzOrientation(orientation);
        if (data.isReader()) {
            setItemCount(count);
        }
        woodLog = data.as(Share.VISIBLE, "log").putItemStack(woodLog);
        woodSlab = data.as(Share.VISIBLE, "slab").putItemStack(woodSlab);
        type = data.as(Share.VISIBLE, "type").putEnum(type);
        if (woodLog == null) {
            woodLog = DEFAULT_LOG;
        }
        if (woodSlab == null) {
            woodSlab = DEFAULT_SLAB;
        }
        if (data.isReader() && data.isNBT()) {
            last_mentioned_count = getItemCount();
        }
    }
    
    
    
    //Barrel-type Code
    @Override
    public boolean canUpdate() {
        return type == Type.HOPPING;
    }
    
    @Override
    protected void doLogic() {
        if (type != Type.HOPPING) {
            return;
        }
        needLogic();
        
        if (orientation == FzOrientation.UNKNOWN) {
            return;
        }
        if (notice_target == this && worldObj.getBlockPowerInput(xCoord, yCoord, zCoord) > 0) {
            return;
        }
        boolean youve_changed_jim = false;
        int itemCount = getItemCount();
        if (itemCount < getMaxSize()) {
            Coord here = getCoord();
            here.adjust(orientation.top);
            IInventory upi = here.getTE(IInventory.class);
            FzInv upinv = InvUtil.openInventory(upi, orientation.top.getOpposite());
            
            if (upinv != null) {
                ItemStack got = upinv.pull(item, 1, true);
                if (got != null) {
                    upi.markDirty();
                    taint(got);
                    changeItemCount(1);
                    updateStacks();
                    youve_changed_jim = true;
                }
            }
        }
        if (itemCount > 0) {
            Coord here = getCoord();
            here.adjust(orientation.top.getOpposite());
            IInventory downi = here.getTE(IInventory.class);
            FzInv downinv = InvUtil.openInventory(downi, orientation.top);
            
            if (downinv != null) {
                ItemStack bottom_item = getStackInSlot(1);
                if (bottom_item != null) {
                    ItemStack toPush = bottom_item.splitStack(1);
                    ItemStack got = downinv.push(toPush);
                    if (got == null) {
                        downi.markDirty();
                        updateStacks();
                        cleanBarrel();
                        youve_changed_jim = true;
                    } else {
                        bottom_item.stackSize++;
                    }
                }
            }
        }
        if (youve_changed_jim) {
            markDirty();
        }
    }
    
    @Override
    protected int getLogicSpeed() {
        return 8; //To match vanilla hoppers
    }
    
    @Override
    public void neighborChanged() {
        super.neighborChanged();
        if (type == Type.HOPPING) {
            needLogic();
        }
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
    
    public int getItemCountSticky() {
        int count = getItemCount();
        if (type == Type.STICKY) {
            count--;
            return Math.max(0, count);
        }
        return count;
    }
    
    public int getMaxSize() {
        int size = 64*64;
        if (item != null) {
            size = item.getMaxStackSize()*64;
        }
        if (type == Type.LARGER) {
            size *= 2;
        }
        return size;
    }
    
    public boolean itemMatch(ItemStack is) {
        if (is == null || item == null) {
            return false;
        }
        return ItemUtil.couldMerge(item, is);
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
        return ItemUtil.couldMerge(item, is);
    }
    
    boolean isTop(ForgeDirection d) {
        return d == orientation.top;
    }
    
    boolean isTopOrBack(ForgeDirection d) {
        return d == orientation.top || d == orientation.facing.getOpposite();
    }
    
    boolean isBottom(ForgeDirection d) {
        return d == orientation.top.getOpposite();
    }
    
    boolean isBack(ForgeDirection d) {
        return d == orientation.facing.getOpposite();
    }
    
    public void setItemCount(int val) {
        topStack = bottomStack = null;
        middleCount = val;
        changeItemCount(0);
    }
    
    private boolean spammed = false;
    public void changeItemCount(int delta) {
        middleCount = getItemCount() + delta;
        if (middleCount < 0) {
            if (!spammed) {
                Core.logSevere("Tried to set the item count to negative value " + middleCount + " at " + getCoord());
                Thread.dumpStack();
                spammed = true;
            }
            middleCount = 0;
            item = null;
        }
        if (middleCount == 0) {
            topStack = bottomStack = item = null;
            updateClients(MessageType.BarrelCount);
            markDirty();
            return;
        }
        if (topStack == null) {
            topStack = item.copy();
        }
        if (bottomStack == null) {
            bottomStack = item.copy();
        }
        topStack.stackSize = bottomStack.stackSize = 0;
        updateStacks();
        updateClients(MessageType.BarrelCount);
        markDirty();
    }
    
    @Override
    public void onPlacedBy(EntityPlayer player, ItemStack is, int side, float hitX, float hitY, float hitZ) {
        orientation = SpaceUtil.getOrientation(player, side, hitX, hitY, hitZ);
        loadFromStack(is);
        needLogic();
    }
    
    @Override
    public void loadFromStack(ItemStack is) {
        super.loadFromStack(is);
        woodLog = getLog(is);
        woodSlab = getSlab(is);
        type = getUpgrade(is);
        if (type == Type.SILKY && is.hasTagCompound()) {
            NBTTagCompound tag = is.getTagCompound();
            int loadCount = tag.getInteger("SilkCount");
            if (loadCount != 0) {
                ItemStack loadItem = getSilkedItem(is);
                if (loadItem != null) {
                    item = loadItem;
                    setItemCount(loadCount);
                }
            }
        }
    }
    
    public static ItemStack getSilkedItem(ItemStack is) {
        if (is == null || !is.hasTagCompound()) {
            return null;
        }
        NBTTagCompound tag = is.getTagCompound();
        if (tag.hasKey("SilkItem")) {
            return ItemStack.loadItemStackFromNBT(is.getTagCompound().getCompoundTag("SilkItem"));
        }
        return null;
    }
    
    public static boolean isNested(ItemStack is) {
        return getSilkedItem(is) != null;
    }
    
    
    //Network stuff
    
    FMLProxyPacket getPacket(MessageType messageType) {
        if (messageType == NetworkFactorization.MessageType.BarrelItem) {
            return Core.network.TEmessagePacket(getCoord(), messageType, NetworkFactorization.nullItem(item), getItemCount());
        } else if (messageType == NetworkFactorization.MessageType.BarrelCount) {
            return Core.network.TEmessagePacket(getCoord(), messageType, getItemCount());
        } else {
            new IllegalArgumentException("bad MessageType: " + messageType).printStackTrace();
            return null;
        }
    }
    
    void updateClients(MessageType messageType) {
        if (getWorldObj() == null || getWorldObj().isRemote) {
            return;
        }
        broadcastMessage(null, getPacket(messageType));
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public boolean handleMessageFromServer(MessageType messageType, ByteBuf input) throws IOException {
        if (super.handleMessageFromServer(messageType, input)) {
            return true;
        }
        if (messageType == MessageType.BarrelDescription) {
            item = DataUtil.readStack(input);
            setItemCount(input.readInt());
            woodLog = DataUtil.readStack(input);
            woodSlab = DataUtil.readStack(input);
            orientation = FzOrientation.getOrientation(input.readByte());
            type = Type.valueOf(input.readByte());
            freeDisplayList();
            return true;
        }
        if (messageType == MessageType.BarrelCount) {
            setItemCount(input.readInt());
            freeDisplayList();
            return true;
        }
        if (messageType == MessageType.BarrelItem) {
            item = DataUtil.readStack(input);
            setItemCount(input.readInt());
            freeDisplayList();
            return true;
        }
        if (messageType == MessageType.BarrelDoubleClickHack) {
            Minecraft mc = Minecraft.getMinecraft();
            mc.playerController.currentItemHittingBlock = mc.thePlayer.getHeldItem();
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
    public void markDirty() {
        super.markDirty();
        cleanBarrel();
        updateStacks();
        int c = getItemCount();
        if (c != last_mentioned_count) {
            if (last_mentioned_count*c <= 0) {
                //One of them was 0
                updateClients(MessageType.BarrelItem);
            } else {
                updateClients(MessageType.BarrelCount);
            }
            last_mentioned_count = c;
        }
        if (type == Type.HOPPING) {
            needLogic();
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
        if (count == 0) {
            topStack = bottomStack = null;
            middleCount = 0;
            return;
        }
        if (bottomStack == null) {
            bottomStack = item.copy();
            bottomStack.stackSize = 0;
        }
        int upperLine = getMaxSize() - item.getMaxStackSize();
        if (type == Type.STICKY) {
            count--;
            if (count < 0) {
                return;
            }
            upperLine--;
        }
        if (topStack == null) {
            topStack = item.copy();
        }
        if (count > upperLine) {
            topStack.stackSize = count - upperLine;
            count -= topStack.stackSize;
        } else {
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
        updateStacks();
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
            if (!spammed) {
                Core.logWarning("Bye bye, %s", is);
                Thread.dumpStack();
                spammed = true;
            }
            return;
        }
        if (slot == 0) {
            topStack = is;
        } else if (slot == 1) {
            bottomStack = is;
        } else {
            if (!spammed) {
                Core.logSevere("Say goodbye, %s !", is);
                Thread.dumpStack();
                spammed = true;
            }
        }
        worldObj.markTileEntityChunkModified(xCoord, yCoord, zCoord, this);
    }

    @Override
    public String getInventoryName() {
        return "Barrel";
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack is) {
        if (i != 0) {
            return false;
        }
        if (item == null) {
            if (isNested(is)) {
                return false;
            }
            return true;
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
        if (isTopOrBack(d)) {
            return top_slot;
        }
        if (isBottom(d)) {
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

        ItemStack held = entityplayer.getHeldItem();
        if (held == null) {
            info(entityplayer);
            return true;
        }
        
        if (!worldObj.isRemote && isNested(held) && (item == null || itemMatch(held))) {
            new Notice(notice_target, "No.").send(entityplayer);
            return true;
        }
        
        NBTTagCompound tag = held.getTagCompound();
        if (tag != null && tag.hasKey("noFzBarrel")) {
            return false;
        }

        boolean veryNew = taint(held);

        if (!itemMatch(held)) {
            if (LangUtil.getTranslationKey(held.getItem()).equals(LangUtil.getTranslationKey(item))) {
                new Notice(notice_target, "That item is different").send(entityplayer);
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
        int take = Math.min(free, held.stackSize);
        held.stackSize -= take;
        changeItemCount(take);
        if (veryNew) {
            updateClients(MessageType.BarrelItem);
        }
        if (held.stackSize == 0) {
            entityplayer.setCurrentItemOrArmor(0, null);
        }
        return true;
    }
    
    void addAllItems(EntityPlayer entityplayer) {
        ItemStack held = entityplayer.getHeldItem();
        if (held != null) {
            taint(held);
        }
        /*if (held != null && !itemMatch(held)) {
            if (Core.getTranslationKey(held).equals(Core.getTranslationKey(item))) {
                new Notice(notice_target, "That item is different").send(entityplayer);
            } else {
                info(entityplayer);
            }
            return;
        }*/
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
            if (is == held && toAdd > 1) {
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
    @SubscribeEvent
    public void clickEvent(PlayerInteractEvent event) {
        if (event.entityPlayer.worldObj.isRemote) {
            return;
        }
        last_hit_side = event.face;
    }

    static boolean isStairish(Coord c) {
        Block b = c.getBlock();
        if (b == null) {
            return false;
        }
        if (b instanceof BlockRailBase) {
            return true;
        }
        AxisAlignedBB ab = c.getCollisionBoundingBoxFromPool();
        ArrayList<AxisAlignedBB> list = new ArrayList();
        b.addCollisionBoxesToList(c.w, c.x, c.y, c.z, ab, list, null);
        for (AxisAlignedBB bb : list) {
            if (bb.maxY - c.y <= 0.51) {
                return true;
            }
        }
        return false;
    }
    
    boolean punt(EntityPlayer player) {
        int distance = PlayerUtil.getPuntStrengthInt(player);
        if (distance <= 0) {
            return false;
        }
        ForgeDirection dir = ForgeDirection.getOrientation(last_hit_side).getOpposite();
        if (dir == ForgeDirection.UNKNOWN) {
            return false;
        }
        Coord src = getCoord();
        Coord next = src;
        FzOrientation newOrientation = orientation;
        boolean doRotation = dir.offsetY == 0;
        ForgeDirection rotationAxis = ForgeDirection.UNKNOWN;
        if (doRotation) {
            rotationAxis = dir.getRotation(ForgeDirection.UP);
        }
        if (player.isSneaking() && distance > 1) {
            distance = 1;
        }
        int spillage = distance;
        int doubleRolls = 0;
        for (int i = 0; i < distance; i++) {
            if (i > 6) {
                break;
            }
            boolean must_rise_or_fail = false;
            Coord peek = next.add(dir);
            if (!peek.blockExists()) {
                break;
            }
            Coord below = peek.add(ForgeDirection.DOWN);
            int rotateCount = 1;
            if (!peek.isReplacable()) {
                if (!isStairish(peek)) {
                    break;
                }
                Coord above = peek.add(ForgeDirection.UP);
                if (!above.isAir() /* Not going to replace snow in this case */) {
                    break;
                }
                next = above;
                spillage += 3;
                rotateCount++;
                doubleRolls++;
            } else if (below.isReplacable() && i != distance - 1) {
                next = below;
                spillage++;
            } else {
                next = peek;
            }
            if (!doRotation) {
                rotateCount = 0;
            }
            //When we roll a barrel, the side we punch should face up
            for (int r = rotateCount; r > 0; r--) {
                ForgeDirection nTop = newOrientation.top.getRotation(rotationAxis);
                ForgeDirection nFace = newOrientation.facing.getRotation(rotationAxis);
                newOrientation = FzOrientation.fromDirection(nFace).pointTopTo(nTop);
            }
        }
        if (src.equals(next)) {
            return false;
        }
        if (!doRotation && orientation.top == ForgeDirection.UP && dir == ForgeDirection.UP) {
            spillage = 0;
        }
        if (doubleRolls % 2 == 1) {
            Sound.barrelPunt2.playAt(src);
        } else {
            Sound.barrelPunt.playAt(src);
        }
        src.rmTE();
        src.setAir();
        if (newOrientation != FzOrientation.UNKNOWN) {
            this.orientation = newOrientation;
        }
        this.validate();
        next.setId(getBlockClass().block);
        next.setTE(this);
        last_hit_side = -1;
        player.addExhaustion(0.5F);
        ItemStack is = player.getHeldItem();
        if (is != null && is.isItemStackDamageable() && worldObj.rand.nextInt(4) == 0) {
            is.damageItem(distance, player);
            if (is.stackSize <= 0) {
                player.destroyCurrentEquippedItem();
            }
        }
        //spillItems(spillage); // Meh!
        return true;
    }
    
    @Override
    public void click(EntityPlayer entityplayer) {
        // left click: remove a stack, or punt if properly equipped
        if (entityplayer.worldObj.isRemote) {
            return;
        }
        if (punt(entityplayer)) {
            return;
        }
        if (getItemCount() == 0 || item == null) {
            info(entityplayer);
            return;
        }
        ItemStack origHeldItem = entityplayer.getHeldItem();
        if (ForgeHooks.canToolHarvestBlock(Blocks.log, 0, origHeldItem)) {
            return;
        }
        
        int to_remove = Math.min(item.getMaxStackSize(), getItemCount());
        if (entityplayer.isSneaking() && to_remove >= 1) {
            to_remove = 1;
        }
        if (to_remove > 1 && to_remove == getItemCount()) {
            to_remove--;
        }
        Entity ent = ItemUtil.giveItem(entityplayer, new Coord(this), makeStack(to_remove), ForgeDirection.getOrientation(last_hit_side));
        if (ent != null && ent.isDead && !(entityplayer instanceof FakePlayer)) {
            ItemStack newHeld = entityplayer.getHeldItem();
            if (newHeld != origHeldItem) {
                broadcastMessage(entityplayer, MessageType.BarrelDoubleClickHack);
            }
        }
        changeItemCount(-to_remove);
        cleanBarrel();
        last_hit_side = -1;
    }
    
    void info(final EntityPlayer entityplayer) {
        new Notice(notice_target, new NoticeUpdater() {
            @Override
            public void update(Notice msg) {
                int itemCount = getItemCount();
                
                if (item == null && getItemCount() == 0) {
                    msg.setMessage("Empty");
                } else if (getItemCount() >= getMaxSize()) {
                    msg.withItem(item).setMessage("Full of {ITEM_NAME}{ITEM_INFOS_NEWLINE}");
                } else {
                    String count = "" + getItemCount();
                    if (type == Type.CREATIVE) {
                        count = "Infinite";
                    }
                    msg.withItem(item).setMessage("%s {ITEM_NAME}{ITEM_INFOS_NEWLINE}", count);
                }
            }
        }).send(entityplayer);
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
    public IIcon getIcon(ForgeDirection dir) {
        if (dir.offsetY != 0) {
            Block ws = DataUtil.getBlock(woodSlab);
            if (ws != null) {
                return ws.getIcon(0, woodSlab.getItemDamage());
            }
            return woodSlab.getItem().getIcon(woodSlab, 0);
        }
        Block wl = DataUtil.getBlock(woodLog);
        if (wl != null) {
            return wl.getIcon(2, woodLog.getItemDamage());
        }
        return woodLog.getItem().getIcon(woodLog, 0);
    }
    
    @Override
    public void dropContents() {
        if (type == Type.CREATIVE || (type == Type.SILKY && broken_with_silk_touch)) {
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
            ItemUtil.giveItem(null, new Coord(this), makeStack(to_drop), ForgeDirection.UNKNOWN);
            if (count <= 0) {
                break;
            }
        }
        topStack = null;
        middleCount = 0;
        bottomStack = null;
    }
    
    public boolean canLose() {
        return item != null && getItemCount() > maxStackDrop * item.getMaxStackSize();
    }
    
    public static ItemStack makeBarrel(Type type, ItemStack log, ItemStack slab) {
        ItemStack barrel_item = new ItemStack(Core.registry.daybarrel);
        barrel_item = addUpgrade(barrel_item, type);
        NBTTagCompound tag = ItemUtil.getTag(barrel_item);
        tag.setTag("log", DataUtil.item2tag(log));
        tag.setTag("slab", DataUtil.item2tag(slab));
        int dmg = DataUtil.getName(log).hashCode() * 16 + log.getItemDamage();
        dmg %= 1000;
        dmg *= 10;
        dmg += type.ordinal();
        barrel_item.setItemDamage(dmg);
        return barrel_item;
    }
    
    public static ArrayList<ItemStack> barrel_items = new ArrayList();
    private static ItemStack make(Type type, ItemStack log, ItemStack slab) {
        ItemStack ret = makeBarrel(type, log, slab);
        barrel_items.add(ret);
        return ret;
    }
    
    static {
        make(Type.CREATIVE, new ItemStack(Blocks.bedrock), new ItemStack(Blocks.diamond_block));
    }
    
    public static void makeRecipe(Object objLog, Object objSlab) {
        ItemStack log, slab;
        if (objLog instanceof ItemStack) {
            log = (ItemStack) objLog;
        } else {
            log = ItemUtil.getFirstOre((String) objLog);
            if (log == null) return; // Eek!
        }
        if (objSlab instanceof ItemStack) {
            slab = (ItemStack) objSlab;
        } else {
            slab = ItemUtil.getFirstOre((String) objSlab);
            if (slab == null) return; // Meep!
        }
        ItemStack normal = make(Type.NORMAL, log, slab);
        Core.registry.oreRecipe(normal,
                "W-W",
                "W W",
                "WWW",
                'W', objLog,
                '-', objSlab);
    }
    
    public static Type getUpgrade(ItemStack is) {
        if (is == null) {
            return Type.NORMAL;
        }
        NBTTagCompound tag = is.getTagCompound();
        if (tag == null) {
            return Type.NORMAL;
        }
        String name = tag.getString("type");
        if (name == null || name.equals("")) {
            return Type.NORMAL;
        }
        try {
            return Type.valueOf(name);
        } catch (IllegalArgumentException e) {
            Core.logWarning("%s has invalid barrel Type %s. Resetting it.", is, name);
            e.printStackTrace();
            tag.removeTag("type");
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
        return DataUtil.tag2item(tag, default_);
    }
    
    static ItemStack addUpgrade(ItemStack barrel, Type upgrade) {
        if (upgrade == Type.NORMAL) {
            return barrel;
        }
        barrel = barrel.copy();
        NBTTagCompound tag = ItemUtil.getTag(barrel);
        tag.setString("type", upgrade.toString());
        return barrel;
    }
    
    @Override
    public boolean rotate(ForgeDirection axis) {
        if (axis == ForgeDirection.UNKNOWN) {
            return false;
        }
        if (axis == orientation.facing || axis.getOpposite() == orientation.facing) {
            orientation = orientation.getNextRotationOnFace();
            return true;
        }
        if (axis == orientation.top || axis.getOpposite() == orientation.top) {
            orientation = orientation.getNextRotationOnTop();
            return true;
        }
        orientation = FzOrientation.fromDirection(axis);
        return true;
    }
    
    @Override
    public ItemStack getDroppedBlock() {
        ItemStack is = makeBarrel(type, woodLog, woodSlab);
        if (type == Type.SILKY && item != null && getItemCount() > 0) {
            NBTTagCompound tag = ItemUtil.getTag(is);
            tag.setInteger("SilkCount", getItemCount());
            NBTTagCompound si = new NBTTagCompound();
            item.writeToNBT(si);
            tag.setTag("SilkItem", si);
            tag.setLong("rnd", hashCode() + worldObj.getTotalWorldTime());
        }
        return is;
    }
    
    boolean broken_with_silk_touch = false;
    
    @Override
    protected boolean removedByPlayer(EntityPlayer player, boolean willHarvest) {
        if (cancelRemovedByPlayer(player)) return false;
        broken_with_silk_touch = EnchantmentHelper.getSilkTouchModifier(player);
        return super.removedByPlayer(player, willHarvest);
    }
    
    private boolean cancelRemovedByPlayer(EntityPlayer player) {
        if (item == null) {
            return false;
        }
        if (player == null) return false;
        if (!player.capabilities.isCreativeMode) return false;
        if (player.isSneaking()) return false;
        if (player.worldObj.isRemote) {
            Core.proxy.sendBlockClickPacket();
        } else {
            click(player);
        }
        return true;
    }
    
    static final ArrayList<Integer> finalizedDisplayLists = new ArrayList();
    
    public static void addFinalizedDisplayList(int display_list) {
        if (display_list <= 0) return;
        synchronized (finalizedDisplayLists) {
            finalizedDisplayLists.add(display_list);
        }
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    protected void finalize() throws Throwable {
        super.finalize();
        if (display_list != -1) {
            if (Core.dev_environ) {
                Core.logFine("Barrel display list released via finalization"); // dev environ, it's fine!
            }
            addFinalizedDisplayList(display_list);
            display_list = -1;
        }
    }
    
    @SideOnly(Side.CLIENT)
    final void freeDisplayList() {
        if (display_list == -1) {
            return;
        }
        GLAllocation.deleteDisplayLists(display_list);
        display_list = -1;
    }
    
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void removeUnloadedDisplayLists(ChunkEvent.Unload event) {
        if (!event.world.isRemote) return;
        for (TileEntity te : (Iterable<TileEntity>)event.getChunk().chunkTileEntityMap.values()) {
            if (te instanceof TileEntityDayBarrel) {
                TileEntityDayBarrel me = (TileEntityDayBarrel) te;
                me.freeDisplayList();
            }
        }
    }
    
    private static int iterateDelay = 0;
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void iterateForFinalizedBarrels(ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (iterateDelay > 0) {
            iterateDelay--;
            return;
        }
        iterateDelay = 60*20;
        synchronized (finalizedDisplayLists) {
            for (Integer i : finalizedDisplayLists) {
                if (i == null) continue;
                GLAllocation.deleteDisplayLists(i);
            }
            finalizedDisplayLists.clear();
        }
    }
    
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void renderHilightArrow(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        if (player == null) return;
        if (!Minecraft.isGuiEnabled()) return;
        ItemStack is = player.getHeldItem();
        if (is == null) return;
        if (is.getItem() != Core.registry.daybarrel) return;
        MovingObjectPosition mop = mc.objectMouseOver;
        if (mop == null || mop.hitVec == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;
        Vec3 vec = mop.hitVec;
        FzOrientation orientation = SpaceUtil.getOrientation(player, mop.sideHit, (float) (vec.xCoord - mop.blockX), (float) (vec.yCoord - mop.blockY), (float) (vec.zCoord - mop.blockZ));
        if (orientation.top.offsetY == 1) {
            /*
             * The purpose of this is two-fold:
             * 		- It renders at the wrong spot when pointing upwards on a vertical face
             * 		- You totally don't really need it in this case
             */
            return;
        }
        GL11.glPushMatrix();
        {
            EntityLivingBase camera = Minecraft.getMinecraft().renderViewEntity;
            double cx = camera.lastTickPosX + (camera.posX - camera.lastTickPosX) * (double) event.partialTicks;
            double cy = camera.lastTickPosY + (camera.posY - camera.lastTickPosY) * (double) event.partialTicks;
            double cz = camera.lastTickPosZ + (camera.posZ - camera.lastTickPosZ) * (double) event.partialTicks;
            GL11.glTranslated(-cx, -cy, -cz);
        }
        ForgeDirection fd = ForgeDirection.getOrientation(mop.sideHit);
        GL11.glTranslatef(mop.blockX + fd.offsetX, mop.blockY + fd.offsetY, mop.blockZ + fd.offsetZ);
        GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.4F);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glLineWidth(2.0F);
        
        {
            ForgeDirection face = orientation.facing;
            if (SpaceUtil.sign(face) == 1) {
                GL11.glTranslated(face.offsetX, face.offsetY, face.offsetZ);
            }
            float d = -2F;
            GL11.glTranslatef(d*fd.offsetX, d*fd.offsetY, d*fd.offsetZ);
            GL11.glTranslated(
                    0.5*(1 - Math.abs(face.offsetX)), 
                    0.5*(1 - Math.abs(face.offsetY)), 
                    0.5*(1 - Math.abs(face.offsetZ))
                    );
            
            GL11.glBegin(GL11.GL_LINE_LOOP);
            float mid_x = orientation.facing.offsetX;
            float mid_y = orientation.facing.offsetY;
            float mid_z = orientation.facing.offsetZ;
            
            float top_x = mid_x + orientation.top.offsetX/2F;
            float top_y = mid_y + orientation.top.offsetY/2F;
            float top_z = mid_z + orientation.top.offsetZ/2F;
            
            float bot_x = mid_x - orientation.top.offsetX/2F;
            float bot_y = mid_y - orientation.top.offsetY/2F;
            float bot_z = mid_z - orientation.top.offsetZ/2F;
            
            ForgeDirection r = orientation.facing.getRotation(orientation.top);
            float right_x = r.offsetX/2F;
            float right_y = r.offsetY/2F;
            float right_z = r.offsetZ/2F;
            
            
            //GL11.glVertex3f(mid_x, mid_y, mid_z);
            GL11.glVertex3f(top_x, top_y, top_z);
            GL11.glVertex3f(mid_x + right_x, mid_y + right_y, mid_z + right_z);
            d = 0.25F;
            GL11.glVertex3f(mid_x + right_x*d, mid_y + right_y*d, mid_z + right_z*d);
            GL11.glVertex3f(bot_x + right_x*d, bot_y + right_y*d, bot_z + right_z*d);
            d = -0.25F;
            GL11.glVertex3f(bot_x + right_x*d, bot_y + right_y*d, bot_z + right_z*d);
            GL11.glVertex3f(mid_x + right_x*d, mid_y + right_y*d, mid_z + right_z*d);
            GL11.glVertex3f(mid_x - right_x, mid_y - right_y, mid_z - right_z);
            GL11.glEnd();
        }
        
        GL11.glPopMatrix();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
    }

    public boolean isWooden() {
        Block log = DataUtil.getBlock(woodLog);
        return log != null && log.getMaterial() == Material.wood;
    }

    public int getFlamability() {
        // The creative barrel I give you can't burn, so won't check for CREATIVE.
        return isWooden() ? 20 : 0;
    }

    @Override
    public int compareRenderer(TileEntityDayBarrel other) {
        return RenderSorter.compareItemRender(item, other.item, IItemRenderer.ItemRenderType.INVENTORY);
    }
}
