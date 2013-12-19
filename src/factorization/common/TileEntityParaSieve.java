package factorization.common;

import static factorization.shared.TileEntityCommon.full_rotation_array;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.entity.Entity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Icon;
import net.minecraft.util.MathHelper;
import net.minecraftforge.common.ForgeDirection;

import org.bouncycastle.util.Arrays;

import cpw.mods.fml.common.registry.GameData;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.GameRegistry.UniqueIdentifier;
import cpw.mods.fml.common.registry.ItemData;
import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Coord;
import factorization.shared.BlockClass;
import factorization.shared.Core;
import factorization.shared.FactoryType;
import factorization.shared.FzUtil;
import factorization.shared.FzUtil.FzInv;
import factorization.shared.TileEntityFactorization;

public class TileEntityParaSieve extends TileEntityFactorization implements ISidedInventory {
    ItemStack[] filters = new ItemStack[8];
    private boolean putting_nbt = false;
    private byte redstone_cache = -1;
    
    static short[] itemId2modIndex = new short[Item.itemsList.length];
    
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
        if (a.itemID != b.itemID) {
            //Compare on mods
            short aId = itemId2modIndex[a.itemID];
            short bId = itemId2modIndex[b.itemID];
            if (aId == bId && aId != 0) {
                return itemId2modIndex[stranger.itemID] == aId;
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
        if (a.itemID != stranger.itemID) {
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
        for (int i = 0; i < filters.length/2; i++) {
            ItemStack a = filters[i*2], b = filters[i*2 + 1];
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
        if (_beginRecursion() || putting_nbt || worldObj == null || worldObj.isRemote) {
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
        TileEntity te = worldObj.getBlockTileEntity(xCoord + facing.offsetX, yCoord + facing.offsetY, zCoord + facing.offsetZ);
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
    public String getInvName() {
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
                int[] ret = Arrays.clone(((ISidedInventory)target).getAccessibleSlotsFromSide(side));
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
    public Icon getIcon(ForgeDirection dir) {
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
            for (int i = 0; i < filters.length; i++) {
                if (filters[i] != null) {
                    empty = false;
                    break;
                }
            }
            if (empty) {
                return getCoord().add(getFacing()).getComparatorOverride(getFacing().getOpposite());
            }
            FzInv inv = FzUtil.openInventory(getCoord().add(getFacing()).getTE(IInventory.class), getFacing().getOpposite());
            if (inv == null) {
                return getCoord().add(getFacing()).getComparatorOverride(getFacing().getOpposite());
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
                return 0;
            }
            fullness /= (float) inv.size();
            return MathHelper.floor_float(fullness * 14.0F) + 1;
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
    public void onInventoryChanged() {
        super.onInventoryChanged();
        try {
            IInventory inv = getRecursiveTarget();
            if (inv == null) {
                return;
            }
            inv.onInventoryChanged();
        } finally {
            endRecursion();
        }
    }
    
    @Override
    public void onNeighborTileChanged(int tilex, int tiley, int tilez) {
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
            super.onInventoryChanged();
        } finally {
            endRecursion();
        }
    }
    
    @Override
    public void representYoSelf() {
        super.representYoSelf();
        Core.logFine("[parasieve] Classifying items");
        HashMap<String, Short> modMap = new HashMap();
        short seen = 1;
        Map<Integer, ItemData> dataMap = null;
        try {
            dataMap = (Map<Integer, ItemData>) ReflectionHelper.getPrivateValue(GameData.class, null, "idMap");
        } catch (Throwable e) {
            e.printStackTrace();
        }
        for (Item item : Item.itemsList) {
            if (item == null) {
                continue;
            }
            UniqueIdentifier ui = GameRegistry.findUniqueIdentifierFor(item);
            String modName = null;
            if (ui != null) {
                modName = ui.modId;
            }
            if (modName == null && dataMap != null) {
                ItemData id = dataMap.get(item.itemID);
                modName = id.getModId();
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
            itemId2modIndex[item.itemID] = val;
        }
        Core.logFine("[parasieve] Done");
    }
}
