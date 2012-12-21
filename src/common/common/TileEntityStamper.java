package factorization.common;

import java.util.ArrayList;
import java.util.Iterator;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.ISidedInventory;
import net.minecraftforge.common.ForgeDirection;
import static net.minecraftforge.common.ForgeDirection.*;


public class TileEntityStamper extends TileEntityFactorization {
    // save these naughty juvenile males
    ItemStack input;
    ItemStack output;
    ArrayList<ItemStack> outputBuffer;

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.Machine;
    }

    @Override
    public int getSizeInventory() {
        return 2;
    }

    @Override
    public int getStartInventorySide(ForgeDirection side) {
        switch (side) {
        case UP:
        case DOWN:
            return 0;
        default:
            return 1;
        }
    }

    @Override
    public int getSizeInventorySide(ForgeDirection side) {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int i) {
        needLogic(); //Hey there, Builcraft. Fix your shit!
        switch (i) {
        case 0:
            return input;
        case 1:
            return output;
        default:
            return null;
        }
    }

    @Override
    public void setInventorySlotContents(int i, ItemStack itemstack) {
        if (i == 0) {
            input = itemstack;
        }
        if (i == 1) {
            output = itemstack;
        }
        onInventoryChanged();
    }
    
    @Override
    public String getInvName() {
        return "Stamper";
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        saveItem("input", tag, input);
        saveItem("output", tag, output);
        if (outputBuffer != null && outputBuffer.size() > 0) {
            NBTTagList buffer = new NBTTagList();
            for (ItemStack item : outputBuffer) {
                if (item == null) {
                    continue;
                }
                NBTTagCompound btag = new NBTTagCompound();
                item.writeToNBT(btag);
                buffer.appendTag(btag);
            }
            tag.setTag("buffer", buffer);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        input = readItem("input", tag);
        output = readItem("output", tag);
        if (tag.hasKey("buffer")) {
            NBTTagList buffer = tag.getTagList("buffer");
            int bufferSize = buffer.tagCount();
            if (bufferSize > 0) {
                outputBuffer = new ArrayList<ItemStack>();
                for (int i = 0; i < bufferSize; i++) {
                    outputBuffer.add(ItemStack
                            .loadItemStackFromNBT((NBTTagCompound) buffer
                                    .tagAt(i)));
                }
            }
        }
    }

    boolean canMerge(ArrayList<ItemStack> items) {
        if (items == null) {
            return true;
        }
        if (output == null) {
            return true;
        }
        for (ItemStack item : items) {
            if (item == null) {
                continue;
            }
            if (!output.isItemEqual(item)) {
                return false;
            }
            if (output.stackSize + item.stackSize > output.getMaxStackSize()) {
                return false;
            }
        }
        return true;
    }

    @Override
    void doLogic() {
        int input_count = (input == null) ? 0 : input.stackSize;
        boolean can_add = output == null
                || output.stackSize < output.getMaxStackSize();
        if (outputBuffer == null && can_add && input != null
                && input.stackSize > 0) {
            ItemStack toCraft;

            if (input.getItem() instanceof ItemCraft) {
                toCraft = input;
                ItemCraft ic = (ItemCraft) input.getItem();
            } else {
                // try to craft a single item
                // Center's the best place for it
                ItemCraft ic = Core.registry.item_craft;
                toCraft = new ItemStack(ic);
                ItemStack craftInput = input.copy();
                craftInput.stackSize = 1;
                ic.addItem(toCraft, 4, craftInput, this);
            }

            ArrayList<ItemStack> fakeResult = Core.registry.item_craft.craftAt(toCraft, true, this);

            if (canMerge(fakeResult)) {
                //really craft
                ArrayList<ItemStack> craftResult = Core.registry.item_craft.craftAt(toCraft, false, this);
                input.stackSize--;
                outputBuffer = craftResult;
                needLogic();
                drawActive(3);
            }
        }

        if (input != null && input.stackSize <= 0) {
            input = null;
        }

        if (outputBuffer != null) {
            // put outputBuffer into output
            Iterator<ItemStack> it = outputBuffer.iterator();
            while (it.hasNext()) {
                ItemStack here = it.next();
                if (here == null) {
                    it.remove();
                    continue;
                }
                if (output == null) {
                    output = here;
                    it.remove();
                    needLogic();
                    continue;
                }
                if (output.isItemEqual(here)) {
                    needLogic();
                    int can_take = output.getMaxStackSize() - output.stackSize;
                    if (here.stackSize > can_take) {
                        output.stackSize += can_take;
                        here.stackSize -= can_take; // will be > 0, keep in list
                        break; // output's full
                    }
                    output.stackSize += here.stackSize;
                    it.remove();
                }
            }
        }

        if (outputBuffer != null && outputBuffer.size() == 0) {
            // It got emptied. Maybe we can fit something else in?
            outputBuffer = null;
            needLogic();
        }
        int new_input_count = (input == null) ? 0 : input.stackSize;
        if (input_count != new_input_count) {
            needLogic();
        }
        if (need_logic_check) {
            pulse();
        }
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.STAMPER;
    }

    @Override
    void makeNoise() {
        Sound.stamperUse.playAt(this);
    }
    
    @Override
    public boolean power() {
        return draw_active > 0;
    }
    
    @Override
    int getLogicSpeed() {
        return 16;
    }
}
