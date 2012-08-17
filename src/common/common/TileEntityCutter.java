package factorization.common;

import net.minecraft.src.ItemStack;
import net.minecraft.src.NBTTagCompound;
import net.minecraftforge.common.ISidedInventory;
import net.minecraftforge.common.ForgeDirection;
import static net.minecraftforge.common.ForgeDirection.*;

public class TileEntityCutter extends TileEntityFactorization implements ISidedInventory {
    // Save these
    ItemStack head;
    ItemStack[] splits;

    // Runtime state
    boolean active;

    public TileEntityCutter() {
        splits = new ItemStack[8];
        active = false;
    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.Machine;
    }

    @Override
    public int getSizeInventory() {
        return 9;
    }

    @Override
    public ItemStack getStackInSlot(int i) {
        if (i == 8) {
            return head;
        }
        if (0 <= i && i < 8) {
            return splits[i];
        }
        return null;
    }

    @Override
    public void setInventorySlotContents(int i, ItemStack itemstack) {
        if (i == 8) {
            head = itemstack;
        } else if (0 <= i && i < 8) {
            splits[i] = itemstack;
        }
        onInventoryChanged();
    }

    @Override
    public String getInvName() {
        return "Stack Cutter";
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    void doLogic() {
        boolean did_something = true;
        int changes = -1;

        while (did_something) {
            changes++;
            if (changes == 1024) {
                break;
            }
            if (head == null || head.stackSize <= 0) {
                break;
            }
            did_something = false;
            for (int i = 0; i < 8; i++) {
                if (head == null || head.stackSize <= 0) {
                    break;
                }
                if (splits[i] == null) {
                    splits[i] = head.copy();
                    splits[i].stackSize = 1;
                    head.stackSize--;
                    did_something = true;
                    continue;
                }
                if (splits[i].stackSize >= splits[i].getMaxStackSize()) {
                    continue;
                }
                if (!splits[i].isItemEqual(head)) {
                    continue;
                }
                splits[i].stackSize++;
                head.stackSize--;
                did_something = true;

            }
        }
        if (head != null && head.stackSize <= 0) {
            head = null;
        }
        if (changes > 0) {
            active = true;
            if (changes < 1024) {
                onInventoryChanged();
            }
        }
    }

    @Override
    public int getStartInventorySide(ForgeDirection side) {
        if (side == UP) {
            // top
            return 8;
        }
        return 0;
    }

    @Override
    public int getSizeInventorySide(ForgeDirection side) {
        if (side == UP) {
            // top
            return 1;
        }
        return 8;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        if (tag.hasKey("head")) {
            head = ItemStack.loadItemStackFromNBT(tag.getCompoundTag("head"));
        }
        readSlotsFromNBT(tag);
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        if (head != null) {
            tag.setTag("head", head.writeToNBT(new NBTTagCompound()));
        }
        writeSlotsToNBT(tag);
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.CUTTER;
    }
}
