package factorization.common;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;

import net.minecraft.src.Block;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.TileEntityFurnace;
import factorization.api.Coord;
import factorization.common.NetworkFactorization.MessageType;
import net.minecraftforge.common.ForgeDirection;
import static net.minecraftforge.common.ForgeDirection.*;



public class TileEntitySlagFurnace extends TileEntityFactorization {
    ItemStack furnaceItemStacks[] = new ItemStack[4];
    public int furnaceBurnTime;
    public int currentFuelItemBurnTime;
    public int furnaceCookTime;

    static final int input = 0, fuel = 1, output = 2;

    @Override
    public int getSizeInventory() {
        return 4;
    }

    @Override
    public ItemStack getStackInSlot(int i) {
        return furnaceItemStacks[i];
    }

    @Override
    public void setInventorySlotContents(int i, ItemStack is) {
        furnaceItemStacks[i] = is;
    }

    @Override
    public String getInvName() {
        return "Slag Furnace";
    }

    @Override
    public int getStartInventorySide(ForgeDirection side) {
        switch (side) {
        case DOWN:
            return fuel; //bottom: fuel
        case UP:
            return input; //top: input
        default:
            return output; //side: output
        }
    }

    @Override
    public int getSizeInventorySide(ForgeDirection side) {
        if (side == DOWN || side == UP) {
            return 1; //bottom/top: fuel/input
        }
        return 2;
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.SLAGFURNACE;
    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.MachineLightable;
    }

    @Override
    public void doLogic() {
        //Not gonna use
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        readSlotsFromNBT(tag);
        furnaceBurnTime = tag.getInteger("burnTime");
        furnaceCookTime = tag.getInteger("cookTime");
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        writeSlotsToNBT(tag);
        tag.setInteger("burnTime", furnaceBurnTime);
        tag.setInteger("cookTime", furnaceCookTime);
    }

    public boolean isBurning() {
        return furnaceBurnTime > 0;
    }

    @Override
    public void updateEntity() {
        boolean burnState = isBurning();
        if (furnaceBurnTime > 0) {
            furnaceBurnTime--;
        }
        if (worldObj.isRemote) {
            return;
        }

        boolean invChanged = false;

        if (this.furnaceBurnTime <= 0 && this.canSmelt()) {
            this.currentFuelItemBurnTime = this.furnaceBurnTime = TileEntityFurnace.getItemBurnTime(this.furnaceItemStacks[fuel]) / 2;

            if (this.furnaceBurnTime > 0)
            {
                invChanged = true;

                if (this.furnaceItemStacks[fuel] != null)
                {
                    --this.furnaceItemStacks[fuel].stackSize;

                    if (this.furnaceItemStacks[fuel].stackSize == 0) {
                        this.furnaceItemStacks[fuel] = this.furnaceItemStacks[fuel].getItem().getContainerItemStack(furnaceItemStacks[fuel]);
                    }
                }
            }
        }

        if (this.isBurning() && this.canSmelt()) {
            ++this.furnaceCookTime;

            if (this.furnaceCookTime >= 200) {
                this.furnaceCookTime = 0;
                this.smeltItem();
                invChanged = true;
            }
        }
        else {
            this.furnaceCookTime = 0;
        }

        if (burnState != isBurning() || (isBurning() && draw_active != 1)) {
            draw_active = -1;
            drawActive(furnaceBurnTime > 0 ? 2 : 0);
            Coord here = getCoord();
            here.dirty();
            here.updateLight();
        }

        if (invChanged) {
            onInventoryChanged();
        }
    }

    boolean checkFit(ItemStack output, ItemStack res, int resSize) {
        if (output == null) {
            return true;
        }
        if (!output.isItemEqual(res)) {
            return false;
        }
        if (output.stackSize + resSize <= output.getMaxStackSize()) {
            return true;
        }
        return false;
    }

    boolean canSmelt() {
        if (this.furnaceItemStacks[input] == null) {
            return false;
        }
        SmeltingResult res = SlagRecipes.getSlaggingResult(this.furnaceItemStacks[input]);
        if (res == null) {
            return false;
        }

        return checkFit(furnaceItemStacks[output + 0], res.output1, (int) res.prob1)
                && checkFit(furnaceItemStacks[output + 1], res.output2, (int) res.prob2);
    }

    int getRandomSize(float f) {
        int i = (int) f;
        if (f - i > rand.nextFloat()) {
            i += 1;
        }
        return i;
    }

    void smeltItem() {
        if (!canSmelt()) {
            return;
        }
        SmeltingResult res = SlagRecipes.getSlaggingResult(this.furnaceItemStacks[input]);
        if (furnaceItemStacks[output + 0] == null) {
            furnaceItemStacks[output + 0] = ItemStack.copyItemStack(res.output1);
            furnaceItemStacks[output + 0].stackSize = 0;
        }
        if (furnaceItemStacks[output + 1] == null) {
            furnaceItemStacks[output + 1] = ItemStack.copyItemStack(res.output2);
            furnaceItemStacks[output + 1].stackSize = 0;
        }
        ItemStack fo0 = furnaceItemStacks[output + 0];
        ItemStack fo1 = furnaceItemStacks[output + 1];
        fo0.stackSize += getRandomSize(res.prob1);
        fo1.stackSize += getRandomSize(res.prob2);
        if (fo0.stackSize > fo0.getMaxStackSize()) {
            fo0.stackSize = fo0.getMaxStackSize();
        }
        if (fo1.stackSize > fo1.getMaxStackSize()) {
            fo1.stackSize = fo1.getMaxStackSize();
        }
        if (fo0.stackSize <= 0) {
            furnaceItemStacks[output + 0] = null;
        }
        if (fo1.stackSize <= 0) {
            furnaceItemStacks[output + 1] = null;
        }

        furnaceItemStacks[input].stackSize -= 1;
        if (furnaceItemStacks[input].stackSize == 0) {
            furnaceItemStacks[input] = null;
        }

    }

    public int getCookProgressScaled(int par1) {
        return this.furnaceCookTime * par1 / 200;
    }

    public int getBurnTimeRemainingScaled(int par1) {
        if (this.currentFuelItemBurnTime == 0) {
            this.currentFuelItemBurnTime = 200;
        }

        return this.furnaceBurnTime * par1 / this.currentFuelItemBurnTime;
    }

    static class SmeltingResult {
        //Java doesn't need tuples or anything at all like that -- Oh wait, yes it does.
        ItemStack input;
        float prob1, prob2;
        ItemStack output1, output2;

        SmeltingResult(ItemStack input, float prob1, ItemStack output1, float prob2,
                ItemStack output2) {
            this.input = input;
            this.prob1 = prob1;
            this.prob2 = prob2;
            this.output1 = output1;
            this.output2 = output2;
            //what a waste of time.
        }
    }

    static class SlagRecipes {

        static ArrayList<SmeltingResult> smeltingResults = new ArrayList();

        static void register(Object o_input, float prob1, Object o_output1, float prob2,
                Object o_output2) {
            ItemStack input = obj2is(o_input), output1 = obj2is(o_output1), output2 = obj2is(o_output2);
            input = ItemStack.copyItemStack(input);
            input.stackSize = 1;
            SmeltingResult value = new SmeltingResult(input, prob1, output1, prob2, output2);
            smeltingResults.add(value);
        }

        static ItemStack obj2is(Object o) {
            if (o instanceof ItemStack) {
                return (ItemStack) o;
            }
            if (o instanceof Block) {
                Block b = (Block) o;
                return new ItemStack(Item.itemsList[b.blockID]);
            }
            if (o instanceof Item) {
                return new ItemStack((Item) o);
            }
            return null;
        }

        static SmeltingResult getSlaggingResult(ItemStack input) {
            for (SmeltingResult res : smeltingResults) {
                if (res.input.isItemEqual(input)) {
                    return res;
                }
            }
            return null;
        }
    }

    @Override
    public boolean handleMessageFromServer(int messageType, DataInput input) throws IOException {
        boolean r = super.handleMessageFromServer(messageType, input);
        if (messageType == MessageType.DrawActive) {
            getCoord().updateLight();
        }
        return r;
    }
}
