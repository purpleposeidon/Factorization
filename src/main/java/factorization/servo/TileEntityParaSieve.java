package factorization.servo;

import java.util.Arrays;
import java.util.HashMap;

import net.minecraft.entity.Entity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraftforge.common.util.ForgeDirection;
import cpw.mods.fml.common.event.FMLModIdMappingEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.GameRegistry.UniqueIdentifier;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Coord;
import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.shared.BlockClass;
import factorization.shared.Core;
import factorization.shared.FzUtil;
import factorization.shared.FzUtil.FzInv;
import factorization.shared.TileEntityFactorization;

public class TileEntityParaSieve extends TileEntityFactorization implements ISidedInventory {
    public ItemStack[] filters = new ItemStack[8];
    private boolean putting_nbt = false;
    private byte redstone_cache = -1;
    
    static short[] itemId2modIndex = new short[1024*2];
    
    TileEntity cached_te = null;
    Entity cached_ent = null;
    
    void dirtyCache() {
        cached_te = null;
        cached_ent = null;
    }
    
    public TileEntityParaSieve() {
        facing_direction = 2;
    }
    
    @Override
    public FactoryType getFactoryType() {
        return FactoryType.PARASIEVE;
    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.Machine;
    }
    
    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        putting_nbt = true;
        writeSlotsToNBT(tag);
        putting_nbt = false;
    }
    
    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        putting_nbt = true;
        readSlotsFromNBT(tag);
        putting_nbt = false;
    }
    
    @Override
    public void dropContents() {
        putting_nbt = true;
        super.dropContents();
        putting_nbt = false;
    }
    
    public ForgeDirection getFacing() {
        return ForgeDirection.getOrientation(facing_direction).getOpposite();
    }
    
    @Override
    protected boolean canFaceVert() {
        return true;
    }
    
    static Coord hereCache = new Coord(null, 0, 0, 0);
    private boolean isPowered() {
        if (redstone_cache == -1) {
            hereCache.set(this);
            redstone_cache = (byte) (hereCache.isPowered() ? 1 : 0);
        }
        return redstone_cache == 1;
    }
    
    @Override
    public void neighborChanged() {
        redstone_cache = -1;
        dirtyCache();
    }
    
    static boolean itemInRange(ItemStack a, ItemStack b, ItemStack stranger) {
        if (stranger == null) {
            return false;
        }
        if (a == null || b == null) {
            if (a != null) {
                return FzUtil.couldMerge(a, stranger);
            }
            if (b != null) {
                return FzUtil.couldMerge(b, stranger);
            }
            return false;
        }
        if (a.getItem() != b.getItem()) {
            //Compare on mods
            short aId = itemId2modIndex[FzUtil.getId(a)];
            short bId = itemId2modIndex[FzUtil.getId(b)];
            if (aId == bId && aId != 0) {
                return itemId2modIndex[FzUtil.getId(stranger)] == aId;
            }
            //Compare on Item class
            Class ca = a.getItem().getClass(), cb = b.getItem().getClass();
            Class c_stranger = stranger.getItem().getClass();
            if (ca == cb) {
                return ca == c_stranger;
            }
            //Broadest match: return true if the stranger starts with the common prefix
            String na = ca.getName();
            String nb = cb.getName();
            String n_stranger = c_stranger.getName();
            int end = Math.min(na.length(), nb.length());
            int end_stranger = n_stranger.length();
            for (int i = 0; i < end; i++) {
                char x = na.charAt(i);
                char y = nb.charAt(i);
                if (x == y) {
                    if (end_stranger <= i || n_stranger.charAt(i) != x) {
                        return false;
                    }
                } else {
                    return true;
                }
            }
            return true; //all 3 names were identical
        }
        if (a != stranger) {
            return false; //It doesn't match! How could we have been so silly as to not notice?
        }
        int mda = a.getItemDamage(), mdb = b.getItemDamage(), md_stranger = stranger.getItemDamage();
        //Only check tag compounds if both items have one.
        if (a.hasTagCompound() == b.hasTagCompound()) {
            if (a.hasTagCompound()) {
                //We aren't going to go all the way with this; that'd be too difficult & expensive
                if (!a.getTagCompound().equals(stranger.getTagCompound())) {
                    return false;
                }
            } else if (stranger.hasTagCompound()) {
                return false; //no a tag, no b tag, but stranger tag
            }
        }
        if (mda < mdb) {
            return mda <= md_stranger && md_stranger <= mdb;
        } else if (mda > mdb) {
            return mda >= md_stranger && md_stranger >= mdb;
        } else {
            return mda == md_stranger;
        }
    }
    
    boolean itemPassesFilter(ItemStack stranger) {
        boolean empty = true;
        boolean p = isPowered();
        for (int i = 0; i < filters.length; i += 2) {
            ItemStack a = filters[i], b = filters[i + 1];
            if (a == null && b == null) {
                continue;
            }
            empty = false;
            if (itemInRange(a, b, stranger)) {
                return true ^ p;
            }
        }
        return empty ^ p;
    }
    
    private byte self_recursion = 0;
    private static ThreadLocal<Integer> stack_recursion = new ThreadLocal<Integer>() {
        @Override protected Integer initialValue() {return 0;};
    };
    
    protected boolean _beginRecursion() {
        int sr = stack_recursion.get() + 1;
        stack_recursion.set(sr);
        self_recursion++;
        if (self_recursion > 1 || sr > 6) {
            return true;
        }
        return false;
    }
    
    protected void endRecursion() {
        self_recursion = (byte) Math.max(0, self_recursion - 1);
        int sr = stack_recursion.get();
        stack_recursion.set(Math.max(0, sr - 1));
    }
    
    AxisAlignedBB target_area = AxisAlignedBB.getBoundingBox(0, 0, 0, 0, 0, 0);
    AxisAlignedBB getTargetArea() {
        final ForgeDirection f = getFacing();
        target_area.minX = xCoord + f.offsetX;
        target_area.minY = yCoord + f.offsetY;
        target_area.minZ = zCoord + f.offsetZ;
        target_area.maxX = target_area.minX + 1;
        target_area.maxY = target_area.minY + 1;
        target_area.maxZ = target_area.minZ + 1;
        return target_area;
    }
    
    boolean isEntityInRange(Entity ent) {
        if (ent == null) return false;
        return ent.getBoundingBox().intersectsWith(getTargetArea());
    }
    
    IInventory getRecursiveTarget() {
        if (_beginRecursion() || putting_nbt || getWorldObj() == null || getWorldObj().isRemote) {
            return null;
        }
        ForgeDirection facing = getFacing();
        if (facing == ForgeDirection.UNKNOWN) {
            return null;
        }
        if (cached_te != null) {
            if (!cached_te.isInvalid()) {
                return FzUtil.openDoubleChest((IInventory) cached_te, true);
            }
            cached_te = null;
        } else if (cached_ent != null) {
            if (!cached_ent.isDead && cached_ent.boundingBox.intersectsWith(getTargetArea())) {
                return (IInventory) cached_ent;
            }
            cached_ent = null;
        }
        TileEntity te = worldObj.getTileEntity(xCoord + facing.offsetX, yCoord + facing.offsetY, zCoord + facing.offsetZ);
        if (te instanceof IInventory) {
            cached_te = te;
            return FzUtil.openDoubleChest((IInventory) te, true);
        }
        for (Entity ent : (Iterable<Entity>)worldObj.getEntitiesWithinAABB(IInventory.class, getTargetArea())) {
            if (ent instanceof IInventory) {
                cached_ent = ent;
                return (IInventory) ent;
            }
        }
        return null;
    }
    
    @Override
    public int getSizeInventory() {
        if (putting_nbt) {
            return filters.length;
        }
        try {
            IInventory target = getRecursiveTarget();
            if (target == null) {
                return filters.length;
            }
            return filters.length + target.getSizeInventory();
        } finally {
            endRecursion();
        }
    }

    @Override
    public ItemStack getStackInSlot(int i) {
        if (i < filters.length) {
            return filters[i];
        }
        try {
            IInventory target = getRecursiveTarget();
            if (target == null) {
                return null;
            }
            return target.getStackInSlot(i - filters.length);
        } finally {
            endRecursion();
        }
    }

    @Override
    public void setInventorySlotContents(int i, ItemStack itemstack) {
        if (i < filters.length) {
            filters[i] = itemstack;
            return;
        }
        try {
            IInventory target = getRecursiveTarget();
            if (target == null) {
                return;
            }
            target.setInventorySlotContents(i - filters.length, itemstack);
        } finally {
            endRecursion();
        }
    }

    @Override
    public String getInventoryName() {
        return "Parasieve";
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack itemstack) {
        if (i < filters.length) {
            return true;
        }
        try {
            IInventory target = getRecursiveTarget();
            if (target == null) {
                return false;
            }
            return target.isItemValidForSlot(i - filters.length, itemstack) && itemPassesFilter(itemstack);
        } finally {
            endRecursion();
        }
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        try {
            IInventory target = getRecursiveTarget();
            if (target == null) {
                return new int[0];
            }
            side = facing_direction;
            if (target instanceof ISidedInventory) {
                int[] slotList = ((ISidedInventory)target).getAccessibleSlotsFromSide(side);
                int[] ret = java.util.Arrays.copyOf(slotList, slotList.length);
                for (int i = 0; i < ret.length; i++) {
                    ret[i] += filters.length;
                }
                return ret;
            } else {
                int len = target.getSizeInventory();
                int[] ret = new int[len];
                for (int i = 0; i < len; i++) {
                    ret[i] = i + filters.length;
                }
                return ret;
            }
        } finally {
            endRecursion();
        }
    }
    
    @Override
    public boolean canInsertItem(int slot, ItemStack itemstack, int side) {
        if (slot < filters.length) {
            return true;
        }
        try {
            IInventory target = getRecursiveTarget();
            if (target == null) {
                return true;
            }
            if (target instanceof ISidedInventory) {
                return ((ISidedInventory) target).canInsertItem(slot - filters.length, itemstack, getFacing().getOpposite().ordinal()) && itemPassesFilter(itemstack);
            }
            return itemPassesFilter(itemstack);
        } finally {
            endRecursion();
        }
    }
    
    @Override
    public boolean canExtractItem(int slot, ItemStack itemstack, int side) {
        if (slot < filters.length) {
            return true;
        }
        try {
            IInventory target = getRecursiveTarget();
            if (target == null) {
                return true;
            }
            if (target instanceof ISidedInventory) {
                return ((ISidedInventory) target).canExtractItem(slot - filters.length, itemstack, facing_direction) && itemPassesFilter(itemstack);
            }
            return itemPassesFilter(itemstack);
        } finally {
            endRecursion();
        }
    }
    
    @Override
    protected void doLogic() { }

    @Override
    public boolean canUpdate() {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(ForgeDirection dir) {
        ForgeDirection face = getFacing();
        if (dir == face) {
            return BlockIcons.parasieve_front;
        } else if (dir == face.getOpposite()) {
            return BlockIcons.parasieve_back;
        } else {
            return BlockIcons.parasieve_side;
        }
    }
    
    @Override
    public int getComparatorValue(ForgeDirection side) {
        try {
            if (_beginRecursion()) {
                return 11;
            }
            boolean empty = true;
            for (int fidx = 0; fidx < filters.length; fidx++) {
                if (filters[fidx] != null) {
                    empty = false;
                }
            }
            if (empty) {
                return getCoord().add(getFacing()).getComparatorOverride(getFacing().getOpposite());
            }
            FzInv inv = FzUtil.openInventory(getCoord().add(getFacing()).getTE(IInventory.class), getFacing().getOpposite());
            if (inv == null) {
                return getCoord().add(getFacing()).getComparatorOverride(getFacing().getOpposite());
            }
            int custom_val = 0;
            {
                for (int fidx = 0; fidx < filters.length; fidx += 2) {
                    final ItemStack low = filters[fidx];
                    if (low == null) continue;
                    final ItemStack high = filters[fidx+1];
                    if (!FzUtil.identical(low, high)) continue;
                    int min = Math.min(low.stackSize, high.stackSize);
                    int max = Math.max(low.stackSize, high.stackSize);
                    int count = 0;
                    for (int i = 0; i < inv.size(); i++) {
                        ItemStack is = inv.get(i);
                        if (FzUtil.identical(low, is)) {
                            count += is.stackSize;
                        }
                    }
                    int res;
                    if (count < min) {
                        res = 0;
                    } else if (count > max) {
                        res = 0xF;
                    } else {
                        res = 1 + 0xE*(count - min)/max;
                    }
                    custom_val = Math.max(custom_val, res);
                }
                if (custom_val >= 0xF) {
                    return 0xF;
                }
            }
            
            int filledSlots = 0;
            float fullness = 0;
            for (int i = 0; i < inv.size(); ++i) {
                ItemStack is = inv.get(i);
                if (is == null || !itemPassesFilter(is)) {
                    continue;
                }
                filledSlots++;
                fullness += is.stackSize / (float) Math.min(inv.under.getInventoryStackLimit(), is.getMaxStackSize());
            }
            if (filledSlots == 0) {
                return custom_val;
            }
            fullness /= (float) inv.size();
            return Math.max(custom_val, MathHelper.floor_float(fullness * 14.0F) + 1);
        } finally {
            endRecursion();
        }
        
    }
    
    @Override
    public ForgeDirection[] getValidRotations() {
        return full_rotation_array;
    }
    
    @Override
    public boolean rotate(ForgeDirection axis) {
        dirtyCache();
        byte ao = (byte) axis.ordinal();
        if (ao == facing_direction) {
            return false;
        }
        facing_direction = ao;
        return true;
    }
    
    @Override
    public void markDirty() {
        super.markDirty();
        try {
            IInventory inv = getRecursiveTarget();
            if (inv == null) {
                return;
            }
            inv.markDirty();
        } finally {
            endRecursion();
        }
    }
    
    @Override
    public void onNeighborTileChanged(int tilex, int tiley, int tilez) {
        //NORELEASE [maybe]: seems like sometimes the comparator doesn't update?
        //Does it happen if the comparator isn't pointing in-line or something?
        ForgeDirection facing = getFacing();
        boolean isOurs = xCoord + facing.offsetX == tilex &&  yCoord + facing.offsetY == tiley &&  zCoord + facing.offsetZ == tilez;
        if (!isOurs) {
            return;
        }
        try {
            IInventory inv = getRecursiveTarget();
            if (inv == null) {
                return;
            }
            super.markDirty();
        } finally {
            endRecursion();
        }
    }
    
    @Override
    public void representYoSelf() {
        super.representYoSelf();
        classifyItems();
    }
    
    @Override
    public void mappingsChanged(FMLModIdMappingEvent event) {
        classifyItems();
    }
    
    void classifyItems() {
        Core.logFine("[parasieve] Classifying items");
        HashMap<String, Short> modMap = new HashMap();
        short seen = 1;
        Arrays.fill(itemId2modIndex, (short) 0);
        for (Item item : (Iterable<Item>) Item.itemRegistry) {
            if (item == null) {
                continue;
            }
            UniqueIdentifier ui = GameRegistry.findUniqueIdentifierFor(item);
            String modName = null;
            if (ui != null) {
                modName = ui.modId;
            }
            if (modName == null) {
                modName = "vanilla?";
            }
            Short val = modMap.get(modName);
            if (val == null) {
                modMap.put(modName, seen);
                val = seen;
                seen++;
            }
            int id = FzUtil.getId(item);
            if (id >= itemId2modIndex.length) {
                itemId2modIndex = Arrays.copyOf(itemId2modIndex, id + 1024);
            }
            itemId2modIndex[id] = val;
        }
        Core.logFine("[parasieve] Done");
    }
}
