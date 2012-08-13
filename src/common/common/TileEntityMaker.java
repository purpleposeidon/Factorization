package factorization.common;

import java.io.DataInput;
import java.io.IOException;

import net.minecraft.src.Block;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.Item;
import net.minecraft.src.ItemBlock;
import net.minecraft.src.ItemStack;
import net.minecraft.src.NBTTagCompound;
import net.minecraftforge.common.ISidedInventory;
import net.minecraftforge.common.Orientation;
import static net.minecraftforge.common.Orientation.*;
import factorization.common.NetworkFactorization.MessageType;

public class TileEntityMaker extends TileEntityFactorization implements
        ISidedInventory {
    // Save these guys
    public int fuel = 0; // pagecount; increased by putting
    // paper/books/bookcases into craft
    public boolean targets[] = new boolean[9]; // slots that should be filled
    ItemStack input;
    ItemStack craft; // Should be empty, fuel, or ItemCraft
    ItemStack output;

    // settings
    private final int input_slot = 0, craft_slot = 1, output_slot = 2;

    public TileEntityMaker() {
        super();
        targets[4] = true;
    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.Machine;
    }

    @Override
    public int getSizeInventory() {
        return 3;
    }

    @Override
    public ItemStack getStackInSlot(int i) {
        switch (i) {
        case input_slot:
            return input;
        case craft_slot:
            return craft;
        case output_slot:
            return output;
        default:
            return null;
        }
    }

    @Override
    public void setInventorySlotContents(int i, ItemStack itemstack) {
        switch (i) {
        case input_slot:
            input = itemstack;
            break;
        case craft_slot:
            craft = itemstack;
            break;
        case output_slot:
            output = itemstack;
            break;
        }
        onInventoryChanged();
    }

    @Override
    public String getInvName() {
        return "Craft Maker";
    }

    public void setTargets(int id, boolean flag) {
        if (targets[id] != flag) {
            targets[id] = flag;
            broadcastTarget(null, id);
        }
    }

    boolean insertItem(int targetSlot, boolean doTarget) {
        if (doTarget == false) {
            return true;
        }
        if (craft == null) {
            return false;
        }

        ItemCraft ic = (ItemCraft) craft.getItem();

        if (input == null) {
            return ic.isSlotSet(craft, targetSlot);
        }

        ItemStack toAdd = input.copy();
        toAdd.stackSize = 1;
        if (ic.addItem(craft, targetSlot, toAdd)) {
            input.stackSize--;
            if (input.stackSize < 1) {
                input = null;
            }
            return true;
        }
        if (ic.isSlotSet(craft, targetSlot)) {
            return true;
        }
        return false;
    }

    void changeFuel(int delta) {
        fuel += delta;
        broadcastFuel(null);
    }

    void handleFuel() {
        if (craft != null) {
            // burn any paper
            // a plank is worth 6
            // a stick is worth 1.5
            // a bookcase has a bunch of books, from somewhere???
            // In any case, a stack of output for a bookcase is Nice.
            int value = 0;
            if (craft.getItem() == Item.paper) {
                value = 1;
            }
            if (craft.getItem() == Item.painting) {
                value = 13;
            }
            if (craft.getItem() == Item.map) {
                value = 8;
            }
            if (craft.getItem() == Item.book) {
                value = 9;
            }
            if (craft.getItem() instanceof ItemBlock) {
                ItemBlock c = (ItemBlock) craft.getItem();
                if (c.getBlockID() == Block.bookShelf.blockID) {
                    value = 64;
                }
            }
            if (value != 0) {
                changeFuel(value * craft.stackSize);
                craft = null; // burn everything at once
            }
        }
    }

    void doLogic() {
        handleFuel();

        boolean have_craft = craft != null
                && craft.getItem() instanceof ItemCraft;
        boolean is_armed = input != null && input.stackSize > 0
                && !(input.getItem() instanceof ItemCraft) && output == null;
        boolean could_move = craft != null && output == null;

        boolean haveFlag = false;
        for (boolean target : targets) {
            haveFlag |= target;
        }
        if (!haveFlag) {
            is_armed = false;
        }

        if (craft == null && fuel != 0 && is_armed) {
            // create a new blank packet
            have_craft = true;
            craft = new ItemStack(Core.registry.item_craft);
            changeFuel(-1);
        }

        if (have_craft && (is_armed || could_move)) {
            boolean success = true;
            for (int index = 0; index < 9; index++) {
                success &= insertItem(index, targets[index]);
            }
            if (success) {
                output = craft;
                craft = null;
                drawActive(3);
            }
        }

    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("fuel", fuel);
        for (int i = 0; i < 9; i++) {
            tag.setBoolean("target" + i, targets[i]);
        }
        saveItem("input", tag, input);
        saveItem("craft", tag, craft);
        saveItem("output", tag, output);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        fuel = tag.getInteger("fuel");
        for (int i = 0; i < 9; i++) {
            targets[i] = tag.getBoolean("target" + i);
        }
        input = readItem("input", tag);
        craft = readItem("craft", tag);
        output = readItem("output", tag);
    }

    @Override
    public int getStartInventorySide(Orientation side) {
        switch (side) {
        case DOWN:
            return craft_slot;
        case UP:
            return input_slot;
        default:
            return output_slot;
        }
    }

    @Override
    public int getSizeInventorySide(Orientation side) {
        return 1;
    }

    @Override
    void sendFullDescription(EntityPlayer player) {
        super.sendFullDescription(player);
        broadcastFuel(player);
        for (int i = 0; i < 9; i++) {
            broadcastTarget(player, i);
        }
    }

    void broadcastFuel(EntityPlayer who) {
        broadcastMessage(who, MessageType.MakerFuel, fuel);
    }

    void broadcastTarget(EntityPlayer who, int slot) {
        broadcastMessage(who, MessageType.MakerTarget, slot, targets[slot]);
    }

    boolean handleMessageFromAny(int messageType, DataInput input)
            throws IOException {
        if (messageType == MessageType.MakerTarget) {
            int target_id = input.readInt();
            boolean state = input.readBoolean();
            if (Core.isCannonical()) {
                setTargets(target_id, state);
            } else {
                // we might be receiving a message from someone else
                // don't want to bounce it back to the server!
                targets[target_id] = state;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean handleMessageFromServer(int messageType, DataInput input)
            throws IOException {
        // target target_slot_id state
        // fuel page_count
        if (super.handleMessageFromServer(messageType, input)) {
            return true;
        }
        if (handleMessageFromAny(messageType, input)) {
            return true;
        }
        if (messageType == MessageType.MakerFuel) {
            fuel = input.readInt();
            return true;
        }
        return false;
    }

    @Override
    public boolean handleMessageFromClient(int messageType, DataInput input)
            throws IOException {
        if (super.handleMessageFromClient(messageType, input)) {
            return true;
        }
        if (handleMessageFromAny(messageType, input)) {
            return true;
        }
        return false;
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.MAKER;
    }

    @Override
    void makeNoise() {
        Sound.makerUse.playAt(this);
    }
}
