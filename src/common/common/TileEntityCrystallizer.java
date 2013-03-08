package factorization.common;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;

import com.google.common.base.Preconditions;

import factorization.common.NetworkFactorization.MessageType;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;

public class TileEntityCrystallizer extends TileEntityFactorization {
    ItemStack inputs[] = new ItemStack[6];
    ItemStack output;

    public ItemStack growing_crystal, solution;
    public int heat, progress;
    public final static int topHeat = 300;
    
    @Override
    public Icon getIcon(ForgeDirection dir) {
        switch (dir) {
        case UP: return BlockIcons.cauldron_top;
        default: return BlockIcons.cauldron_side;
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        writeSlotsToNBT(tag);
        tag.setInteger("heat", heat);
        tag.setInteger("progress", progress);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        readSlotsFromNBT(tag);
        heat = tag.getInteger("heat");
        progress = tag.getInteger("progress");
    }

    @Override
    public int getSizeInventory() {
        return inputs.length + 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        needLogic();
        if (slot == inputs.length) {
            return output;
        }
        return inputs[slot];
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack is) {
        needLogic();
        if (slot == inputs.length) {
            output = is;
            return;
        }
        inputs[slot] = is;
    }

    @Override
    public String getInvName() {
        return "Crystallizer";
    }

    @Override
    public int getStartInventorySide(ForgeDirection side) {
        if (side == ForgeDirection.UP || side == ForgeDirection.DOWN) {
            return inputs.length;
        }
        return 0;
    }

    @Override
    public int getSizeInventorySide(ForgeDirection side) {
        if (side == ForgeDirection.UP || side == ForgeDirection.DOWN) {
            return 1;
        }
        return inputs.length;
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.CRYSTALLIZER;
    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.Machine;
    }

    int pickInputSlot(ItemStack must_match) {
        int max_size, slot;
        slot = -1;
        max_size = -99;
        for (int i = 0; i < inputs.length; i++) {
            if (must_match != null && inputs[i] != null && !FactorizationUtil.identical(must_match, inputs[i])) {
                continue;
            }
            int here_size = FactorizationUtil.getStackSize(inputs[i]);
            if (here_size > max_size) {
                max_size = here_size;
                slot = i;
            }
        }
        return slot;
    }

    public int getProgressRemaining() {
        //20 ticks per second; 60 seconds per minute; 60 minutes per day
        return ((20 * 60 * 20) / getLogicSpeed()) - progress;
    }

    public float getProgress() {
        return ((float) progress) / (getProgressRemaining() + progress);
    }

    boolean needHeat() {
        if (heat >= topHeat) {
            return false;
        }
        return getMatchingRecipe() != null;
    }

    void empty() {
        growing_crystal = null;
        solution = null;
        shareState();
    }

    @Override
    void doLogic() {
        if (heat <= 0) {
            current_state = 1;
            empty();
            return;
        }
        CrystalRecipe match = getMatchingRecipe();
        if (match == null) {
            heat = Math.max(heat - 3, 0);
            progress = (int) Math.min(progress * 0.005 - 1, 0);
            current_state = 2;
            empty();
            return;
        }
        if (growing_crystal == null) {
            growing_crystal = match.output;
            solution = match.solution;
            share_delay = 0;
            current_state = 3;
        }
        if (heat < topHeat) {
            current_state = 4;
            shareState();
            return;
        }
        //we're hot enough. Do progress
        needLogic();
        if (progress == 0) {
            share_delay = 0;
            current_state = 5;
        }
        progress += 1;
        if (getProgressRemaining() <= 0 || Core.cheat) {
            heat = Core.cheat ? topHeat - 20 : 0;
            progress = 0;
            match.apply(this);
            share_delay = 0;
            current_state = 6;
        }
        shareState();
    }

    int share_delay = 20 * 30;
    int current_state = -1, last_state = -1;

    void shareState() {
        share_delay--;
        if (share_delay <= 0 || current_state != last_state) {
            share_delay = 20 * 15;
            broadcastMessage(null, getAuxillaryInfoPacket());
            last_state = current_state;
        }
    }

    int countMaterial(ItemStack toMatch) {
        int count = 0;
        for (ItemStack is : inputs) {
            if (is == null) {
                continue;
            } else  if (toMatch.getItemDamage() == -1 && is.itemID == toMatch.itemID) {
                count += is.stackSize;
            } else  if (FactorizationUtil.identical(is, toMatch)) {
                count += is.stackSize;
            }
        }
        return count;
    }

    public static ArrayList<CrystalRecipe> recipes = new ArrayList();
    
    public static class CrystalRecipe {
        public ItemStack input, output, solution;
        public float output_count;
        public int inverium_count;

        public CrystalRecipe(ItemStack input, ItemStack output, float output_count,
                ItemStack solution, int inverium_count) {
            this.input = input;
            this.output = output;
            this.output_count = output_count;
            this.solution = solution;
            this.inverium_count = inverium_count;
        }

        boolean matches(TileEntityCrystallizer crys) {
            if (crys.countMaterial(new ItemStack(Core.registry.inverium, 1, -1)) < inverium_count) {
                return false;
            }
            if (crys.output != null) {
                if (!FactorizationUtil.identical(crys.output, output)) {
                    return false;
                }
                if (crys.output.stackSize + output_count > crys.output.getMaxStackSize()) {
                    return false;
                }
            }
            if (solution != null) {
                if (crys.countMaterial(solution) < solution.stackSize) {
                    return false;
                }
            }
            if (input != null) {
                return crys.countMaterial(input) >= input.stackSize;
            } else {
                return true;
            }
        }
        
        private void applyTo(TileEntityCrystallizer crys, int slot) {
            int delta = (int) output_count;
            if (delta != output_count && rand.nextFloat() < (output_count - delta)) {
                delta++;
            }
            if (crys.output != null && crys.output.stackSize + delta > crys.output.getMaxStackSize()) {
                return;
            }
            ItemStack is = input.copy();
            while (is.stackSize > 0) {
                crys.inputs[slot].stackSize--;
                crys.inputs[slot] = FactorizationUtil.normalize(crys.inputs[slot]);
                is.stackSize--;
            }
            if (crys.output == null) {
                crys.output = output.copy();
                assert output.stackSize == 0: "output stack size is specified in the output_count";
                crys.output.stackSize = 0;
            }
            crys.output.stackSize += delta;
            int dead_inverium = inverium_count;
            for (int inverium_slot = 0; inverium_slot < crys.inputs.length; inverium_slot++) {
                if (dead_inverium == 0) {
                    break;
                }
                ItemStack inverium = crys.inputs[inverium_slot];
                if (inverium == null || inverium.getItem() != Core.registry.inverium) {
                    continue;
                }
                int toRemove = Math.min(dead_inverium, inverium.stackSize);
                inverium.stackSize -= toRemove;
                dead_inverium -= toRemove;
                if (inverium.stackSize <= 0) {
                    crys.inputs[inverium_slot] = null;
                }
            }
        }

        void apply(TileEntityCrystallizer crys) {
            ItemStack inverium = new ItemStack(Core.registry.inverium, 0, -1);
            for (int i = 0; i < crys.inputs.length; i++) {
                ItemStack is = crys.inputs[i];
                if (is != null && FactorizationUtil.identical(input, is)) {
                    if (crys.countMaterial(inverium) >= inverium_count) {
                        applyTo(crys, i);
                    }
                }
            }
        }
    }

    public static void addRecipe(ItemStack input, ItemStack output, float output_count, ItemStack solution,
            int inverium_count) {
        if (output.stackSize != 1) {
            throw new RuntimeException("Stacksize should be 1");
        }
        if (output_count == 0) {
            throw new RuntimeException("output_count is 0");
        }
        output = output.copy();
        output.stackSize = 0;
        recipes.add(new CrystalRecipe(input, output, output_count, solution, inverium_count));
    }

    CrystalRecipe getMatchingRecipe() {
        for (CrystalRecipe r : recipes) {
            if (r.matches(this)) {
                return r;
            }
        }
        return null;
    }

    ItemStack null2fake(ItemStack is) {
        if (is == null) {
            return Core.registry.crystallizer_item;
        }
        return is;
    }

    ItemStack unfake(ItemStack is) {
        //err, why is this here...
        if (is.isItemEqual(Core.registry.crystallizer_item) /* no NBT okay */) {
            return null;
        }
        return is;
    }

    @Override
    public Packet getAuxillaryInfoPacket() {
        return getDescriptionPacketWith(MessageType.CrystallizerInfo, null2fake(growing_crystal), null2fake(solution), progress);
    }

    @Override
    public boolean handleMessageFromServer(int messageType, DataInput input) throws IOException {
        if (super.handleMessageFromServer(messageType, input)) {
            return true;
        }
        if (messageType == MessageType.CrystallizerInfo) {
            growing_crystal = unfake(FactorizationHack.loadItemStackFromDataInput(input));
            solution = unfake(FactorizationHack.loadItemStackFromDataInput(input));
            progress = input.readInt();
            return true;
        }
        return false;
    }
    
    @Override
    public double func_82115_m() {
        return 576; //24²
    }
}
