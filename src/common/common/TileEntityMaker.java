package factorization.common;

import java.io.DataInput;
import java.io.IOException;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.ISidedInventory;
import net.minecraftforge.common.ForgeDirection;
import static net.minecraftforge.common.ForgeDirection.*;
import factorization.api.Coord;
import factorization.common.NetworkFactorization.MessageType;

public class TileEntityMaker extends TileEntityFactorization implements
        ISidedInventory {
    // Save these guys
    public boolean targets[] = new boolean[9]; // slots that should be filled
    ItemStack input;
    ItemStack paper;
    ItemStack craft; // Should be ItemCraft
    ItemStack output;

    // settings
    private final int input_slot = 0, paper_slot = 1, craft_slot = 2, output_slot = 3;

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
        return 4;
    }

    @Override
    public ItemStack getStackInSlot(int i) {
        switch (i) {
        case input_slot:
            return input;
        case paper_slot:
            return paper;
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
        case paper_slot:
            paper = itemstack;
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
        if (ic.addItem(craft, targetSlot, toAdd, this)) {
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
    
    boolean havePaper() {
        if (paper == null) {
            return false;
        }
        if (paper.getItem() == Item.paper) {
            return true;
        }
        return false;
    }
    
    void eatPaper() {
        assert paper.getItem() == Item.paper;
        paper.stackSize--;
        paper = FactorizationUtil.normalize(paper);
    }
    
    @Override
    int getLogicSpeed() {
        return 16;
    }

    void doLogic() {
        if (paper != null && craft == null && paper.getItem() instanceof ItemCraft) {
            //move craft packet to the correct slot
            craft = paper;
            paper = null;
        }
        if (paper == null && craft != null && craft.getItem() == Item.paper) {
            //move paper to the correct slot
            paper = craft;
            craft = null;
        }
        if (paper != null && paper.getItem() == Item.paper && craft != null && craft.getItem() == Item.paper) {
            //pull in overflow paper
            int free = paper.getMaxStackSize() - paper.stackSize;
            if (free > 0) {
                free = Math.min(free, craft.stackSize);
                craft.stackSize -= free;
                paper.stackSize += free;
                craft = FactorizationUtil.normalize(craft);
            }
        }
        boolean have_craft = craft != null && craft.getItem() instanceof ItemCraft;
        boolean is_armed = input != null && input.stackSize > 0 && !(input.getItem() instanceof ItemCraft) && output == null;
        boolean could_move = craft != null && output == null;

        boolean haveFlag = false;
        for (boolean target : targets) {
            haveFlag |= target;
        }
        if (!haveFlag) {
            is_armed = false;
        }

        if (is_armed && havePaper() && craft == null) {
            // create a new blank packet
            craft = new ItemStack(Core.registry.item_craft);
            have_craft = true;
            eatPaper();
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
                pulse();
            }
        }

    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        for (int i = 0; i < 9; i++) {
            tag.setBoolean("target" + i, targets[i]);
        }
        saveItem("input", tag, input);
        saveItem("craft", tag, craft);
        saveItem("output", tag, output);
        saveItem("paper", tag, paper);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        for (int i = 0; i < 9; i++) {
            targets[i] = tag.getBoolean("target" + i);
        }
        input = readItem("input", tag);
        craft = readItem("craft", tag);
        output = readItem("output", tag);
        paper = readItem("paper", tag);
    }

    @Override
    public int getStartInventorySide(ForgeDirection side) {
        switch (side) {
        case DOWN:
            return paper_slot;
        case UP:
            return input_slot;
        default:
            return output_slot;
        }
    }

    @Override
    public int getSizeInventorySide(ForgeDirection side) {
        return 1;
    }

    @Override
    void sendFullDescription(EntityPlayer player) {
        super.sendFullDescription(player);
        for (int i = 0; i < 9; i++) {
            broadcastTarget(player, i);
        }
    }
    
    void broadcastTarget(EntityPlayer who, int slot) {
        broadcastMessage(who, MessageType.MakerTarget, slot, targets[slot]);
    }

    boolean handleMessageFromAny(int messageType, DataInput input)
            throws IOException {
        if (messageType == MessageType.MakerTarget) {
            int target_id = input.readInt();
            boolean state = input.readBoolean();
            if (!worldObj.isRemote) {
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
