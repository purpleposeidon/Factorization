package factorization.oreprocessing;

import factorization.api.IFurnaceHeatable;
import factorization.api.crafting.CraftingManagerGeneric;
import factorization.api.crafting.IVexatiousCrafting;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.shared.BlockClass;
import factorization.shared.Core;
import factorization.shared.NetworkFactorization;
import factorization.shared.NetworkFactorization.MessageType;
import factorization.shared.TileEntityFactorization;
import factorization.util.DataUtil;
import factorization.util.ItemUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;

import java.io.IOException;

public class TileEntityCrystallizer extends TileEntityFactorization implements IFurnaceHeatable {
    public ItemStack inputs[] = new ItemStack[6];
    public ItemStack output;

    public ItemStack growing_crystal, solution;
    public int heat, progress;

    public static final int default_crystallization_time = Core.cheat ? 20 * 3 : 20 * 60 * 20;
    public static final int default_heating_amount = 300;

    public int cool_time = default_crystallization_time;
    public int heating_amount = default_heating_amount;

    IVexatiousCrafting<TileEntityCrystallizer> active_recipe;
    
    @Override
    public IIcon getIcon(ForgeDirection dir) {
        switch (dir) {
            case UP: return BlockIcons.crystallizer.top;
            case DOWN: return BlockIcons.crystallizer.bottom;
            default: return BlockIcons.crystallizer.side;
        }
    }

    @Override
    public void putData(DataHelper data) throws IOException {
        super.putData(data);
        putSlots(data);
        heat = data.as(Share.PRIVATE, "heat").putInt(heat);
        progress = data.as(Share.VISIBLE, "progress").putInt(progress);
        cool_time = data.as(Share.VISIBLE, "cool_time").putInt(cool_time);
        if (data.isReader() && data.isNBT() && cool_time == 0) {
            cool_time = default_crystallization_time;
        }
        heating_amount = data.as(Share.VISIBLE, "heating_amount").putInt(heating_amount);
        if (data.isReader() && data.isNBT() && heating_amount > 0) {
            heating_amount = default_heating_amount;
        }
        growing_crystal = data.as(Share.VISIBLE, "growing_crystal").putItemStack(growing_crystal);
        solution = data.as(Share.VISIBLE, "solution").putItemStack(solution);
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
        if (slot == inputs.length) {
            output = is;
        } else {
            inputs[slot] = is;
        }
        markDirty();
    }

    @Override
    public String getInventoryName() {
        return "Crystallizer";
    }

    private static final int[] INPUTS_s = {0, 1, 2, 3, 4, 5}, OUTPUT_s = {6};
    
    @Override
    public int[] getAccessibleSlotsFromSide(int s) {
        ForgeDirection side = ForgeDirection.getOrientation(s);
        if (side == ForgeDirection.DOWN) {
            return OUTPUT_s;
        }
        return INPUTS_s;
    }
    
    @Override
    public boolean isItemValidForSlot(int slotIndex, ItemStack itemstack) {
        return slotIndex < inputs.length;
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
            if (must_match != null && inputs[i] != null && !ItemUtil.couldMerge(must_match, inputs[i])) {
                continue;
            }
            int here_size = ItemUtil.getStackSize(inputs[i]);
            if (here_size > max_size) {
                max_size = here_size;
                slot = i;
            }
        }
        return slot;
    }

    public int getProgressRemaining() {
        //20 ticks per second; 60 seconds per minute; 20 minutes per day
        return (cool_time / getLogicSpeed()) - progress;
    }

    public float getProgress() {
        return ((float) progress) / (getProgressRemaining() + progress);
    }

    public boolean needHeat() {
        if (heat >= heating_amount) {
            return false;
        }
        return active_recipe != null;
    }

    void empty() {
        growing_crystal = null;
        solution = null;
        shareState();
    }

    @Override
    protected void doLogic() {
        if (dirtied) {
            dirtied = false;
            IVexatiousCrafting<TileEntityCrystallizer> match = recipes.find(this);
            if (match != active_recipe) {
                active_recipe = match;
                if (active_recipe != null) {
                    heat = 0;
                }
            }
        }
        if (active_recipe == null) {
            heat = Math.max(heat - 3, 0);
            progress = (int) Math.min(progress * 0.005 - 1, 0);
            current_state = 2;
            empty();
            return;
        }
        if (heat == 0) {
            active_recipe.onCraftingStart(this);
            getCoord().syncTE();
        }
        if (heat < heating_amount) {
            current_state = 4;
            shareState();
            return;
        }
        //we're hot enough. Do progress
        needLogic();
        if (progress == 0) {
            //match.onCraftingStart(this);
            share_delay = 0;
            current_state = 5;
        }
        progress += 1;
        if (getProgressRemaining() <= 0 || Core.cheat) {
            heat = Core.cheat ? heat * 6 / 10 : 0;
            progress = 0;
            active_recipe.onCraftingComplete(this);
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
            broadcastMessage(null, getDescriptionPacket());
            last_state = current_state;
        }
    }

    int countMaterial(ItemStack toMatch) {
        int count = 0;
        for (ItemStack is : inputs) {
            if (is == null) {
                continue;
            } else if (ItemUtil.wildcardSimilar(toMatch, is)) {
                count += is.stackSize;
            }
        }
        return count;
    }

    public static final CraftingManagerGeneric<TileEntityCrystallizer> recipes = CraftingManagerGeneric.get(TileEntityCrystallizer.class);

    @Override
    public boolean acceptsHeat() {
        return needHeat();
    }

    @Override
    public void giveHeat() {
        heat++;
    }

    @Override
    public boolean hasLaggyStart() {
        return false;
    }

    @Override
    public boolean isStarted() {
        return heat + progress > 0;
    }

    public static class CrystalRecipe implements IVexatiousCrafting<TileEntityCrystallizer> {
        public ItemStack input, output, solution;
        public float output_count;
        public int heat_amount = default_heating_amount, cool_time = default_crystallization_time;

        public CrystalRecipe(ItemStack input, ItemStack output, float output_count, ItemStack solution) {
            this.input = input;
            this.output = output;
            this.output_count = output_count;
            this.solution = solution;
        }

        @Override
        public boolean matches(TileEntityCrystallizer crys) {
            if (crys.output != null) {
                if (!ItemUtil.couldMerge(crys.output, output)) {
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

        @Override
        public void onCraftingStart(TileEntityCrystallizer machine) {
            machine.heating_amount = this.heat_amount;
            machine.cool_time = this.cool_time;
            machine.growing_crystal = output.copy();
            machine.growing_crystal.stackSize = 1;
            machine.solution = solution;
        }

        @Override
        public void onCraftingComplete(TileEntityCrystallizer crys) {
            for (int i = 0; i < crys.inputs.length; i++) {
                ItemStack is = crys.inputs[i];
                if (is != null && ItemUtil.wildcardSimilar(input, is)) {
                    applyTo(crys, i);
                }
            }
        }

        @Override
        public boolean isUnblocked(TileEntityCrystallizer crys) {
            if (crys.output.stackSize + output_count > crys.output.getMaxStackSize()) {
                return false;
            }
            return true;
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
                crys.inputs[slot] = ItemUtil.normalize(crys.inputs[slot]);
                is.stackSize--;
            }
            if (crys.output == null) {
                crys.output = output.copy();
                assert output.stackSize == 0: "output stack size is specified in the output_count";
                crys.output.stackSize = 0;
            }
            crys.output.stackSize += delta;
            crys.output = ItemUtil.normalize(crys.output);
        }
    }

    public static void addRecipe(ItemStack input, ItemStack output, float output_count, ItemStack solution) {
        if (output.stackSize != 1) {
            throw new RuntimeException("Stacksize should be 1");
        }
        if (output_count == 0) {
            throw new RuntimeException("output_count is 0");
        }
        output = output.copy();
        output.stackSize = 0;
        recipes.add(new CrystalRecipe(input, output, output_count, solution));
    }

    ItemStack null2fake(ItemStack is) {
        if (is == null) {
            return Core.registry.crystallizer_item;
        }
        return is;
    }

    @Override
    public boolean handleMessageFromServer(MessageType messageType, ByteBuf input) throws IOException {
        if (super.handleMessageFromServer(messageType, input)) {
            return true;
        }
        if (messageType == MessageType.CrystallizerInfo) {
            growing_crystal = NetworkFactorization.denullItem(DataUtil.readStack(input));
            solution = NetworkFactorization.denullItem(DataUtil.readStack(input));
            progress = input.readInt();
            return true;
        }
        return false;
    }
    
    @Override
    public double getMaxRenderDistanceSquared() {
        return 576; //24²
    }

    boolean dirtied = true;

    @Override
    public void markDirty() {
        super.markDirty();
        this.dirtied = true;
    }
}
