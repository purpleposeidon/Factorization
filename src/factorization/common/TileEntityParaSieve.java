package factorization.common;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;

import org.bouncycastle.util.Arrays;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class TileEntityParaSieve extends TileEntityFactorization implements ISidedInventory {
    ItemStack[] filters = new ItemStack[8];
    private boolean putting_nbt = false;
    
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
    
    public ForgeDirection getFacing() {
        return ForgeDirection.getOrientation(facing_direction).getOpposite();
    }
    
    @Override
    boolean canFaceVert() {
        return true;
    }
    
    boolean itemPassesFilter(ItemStack stranger) {
        for (int i = 0; i < filters.length/2; i++) {
            ItemStack a = filters[i*2], b = filters[i*2 + 1];
            if (a == b && b == null) {
                continue;
            }
            if (FactorizationUtil.itemInRange(a, b, stranger)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean is_recursing = false;
    private static ThreadLocal<Integer> recursion_count = new ThreadLocal<Integer>() { @Override
    protected Integer initialValue() {return 0;};};
    
    protected boolean beginRecursion() {
        recursion_count.set(recursion_count.get() + 1);
        if (is_recursing || recursion_count.get() > 6) {
            return true;
        }
        is_recursing = true;
        return false;
    }
    
    protected void endRecursion() {
        is_recursing = false;
        recursion_count.set(Math.min(0, recursion_count.get() - 1));
    }
    
    IInventory getTarget() {
        try {
            if (beginRecursion() || putting_nbt || worldObj == null || worldObj.isRemote) {
                return null;
            }
            ForgeDirection facing = getFacing();
            if (facing == ForgeDirection.UNKNOWN) {
                return null;
            }
            //return getCoord().add(facing).getTE(IInventory.class);
            TileEntity te = worldObj.getBlockTileEntity(xCoord + facing.offsetX, yCoord + facing.offsetY, zCoord + facing.offsetZ);
            if (te instanceof IInventory) {
                if (te instanceof TileEntityParaSieve) {
                    return null;
                }
                return FactorizationUtil.openDoubleChest((IInventory) te, true);
            }
            return null;
        } finally {
            endRecursion();
        }
    }
    
    @Override
    public int getSizeInventory() {
        if (putting_nbt) {
            return filters.length;
        }
        IInventory target = getTarget();
        if (target == null) {
            return filters.length;
        }
        return filters.length + target.getSizeInventory();
    }

    @Override
    public ItemStack getStackInSlot(int i) {
        if (i < filters.length) {
            return filters[i];
        }
        IInventory target = getTarget();
        if (target == null) {
            return null;
        }
        return target.getStackInSlot(i - filters.length);
        
    }

    @Override
    public void setInventorySlotContents(int i, ItemStack itemstack) {
        if (i < filters.length) {
            filters[i] = itemstack;
            return;
        }
        IInventory target = getTarget();
        if (target == null) {
            return;
        }
        target.setInventorySlotContents(i - filters.length, itemstack);
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
        IInventory target = getTarget();
        if (target == null) {
            return false;
        }
        return target.isItemValidForSlot(i - filters.length, itemstack) && itemPassesFilter(itemstack);
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        IInventory target = getTarget();
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
    }
    
    @Override
    public boolean canInsertItem(int slot, ItemStack itemstack, int side) {
        if (slot < filters.length) {
            return true;
        }
        IInventory target = getTarget();
        if (target == null) {
            return true;
        }
        if (target instanceof ISidedInventory) {
            return ((ISidedInventory) target).canInsertItem(slot - filters.length, itemstack, side) && itemPassesFilter(itemstack);
        }
        return itemPassesFilter(itemstack);
    }
    
    @Override
    public boolean canExtractItem(int slot, ItemStack itemstack, int side) {
        if (slot < filters.length) {
            return true;
        }
        IInventory target = getTarget();
        if (target == null) {
            return true;
        }
        if (target instanceof ISidedInventory) {
            return ((ISidedInventory) target).canExtractItem(slot - filters.length, itemstack, facing_direction) && itemPassesFilter(itemstack);
        }
        return itemPassesFilter(itemstack);
    }
    
    @Override
    void doLogic() { }

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
            if (beginRecursion()) {
                return 11;
            }
            return getCoord().add(getFacing()).getComparatorOverride(getFacing().getOpposite());
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
        byte ao = (byte) axis.ordinal();
        if (ao == facing_direction) {
            return false;
        }
        facing_direction = ao;
        return true;
    }
}
