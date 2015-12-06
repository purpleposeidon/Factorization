package factorization.oreprocessing;

import factorization.api.IFurnaceHeatable;
import factorization.api.crafting.CraftingManagerGeneric;
import factorization.api.crafting.IVexatiousCrafting;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.common.FactoryType;
import factorization.shared.BlockClass;
import factorization.shared.Core;
import factorization.shared.NetworkFactorization.MessageType;
import factorization.shared.TileEntityFactorization;
import factorization.util.DataUtil;
import factorization.util.ItemUtil;
import factorization.util.SpaceUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class TileEntitySlagFurnace extends TileEntityFactorization implements IFurnaceHeatable {
    ItemStack inv[] = new ItemStack[4];
    public int furnaceBurnTime;
    public int currentFuelItemBurnTime;
    public int furnaceCookTime;

    @Override
    public int getFieldCount() {
        return 2;
    }

    @Override
    public int getField(int id) {
        if (id == 0) return furnaceBurnTime;
        if (id == 1) return currentFuelItemBurnTime;
        if (id == 2) return furnaceCookTime;
        return 0;
    }

    @Override
    public void setField(int id, int value) {
        if (id == 0) furnaceBurnTime = value;
        if (id == 1) currentFuelItemBurnTime = value;
        if (id == 2) furnaceCookTime = value;
    }

    @Override
    public void clear() {
        for (int i = 0; i < inv.length; i++) inv[i] = null;
        furnaceCookTime = furnaceBurnTime = currentFuelItemBurnTime = 0;
    }

    static final int inputSlotIndex = 0, fuelSlotIndex = 1, outputSlotIndex = 2;

    @Override
    public int getSizeInventory() {
        return 4;
    }

    @Override
    public void onPlacedBy(EntityPlayer player, ItemStack is, EnumFacing side, float hitX, float hitY, float hitZ) {
        super.onPlacedBy(player, is, side, hitX, hitY, hitZ);
        facing_direction = (byte) SpaceUtil.determineFlatOrientation(player).ordinal();
    }

    @Override
    public ItemStack getStackInSlot(int i) {
        return inv[i];
    }

    @Override
    public void setInventorySlotContents(int i, ItemStack is) {
        inv[i] = is;
        markDirty();
    }

    @Override
    public IChatComponent getDisplayName() {
        return new ChatComponentTranslation("factorization.slagfurnace");
    }

    private static final int[] INPUT_s = {inputSlotIndex, inputSlotIndex + 1}, FUEL_s = {fuelSlotIndex}, OUTPUT_s = {outputSlotIndex, outputSlotIndex + 1};

    @Override
    public int[] getSlotsForFace(EnumFacing side) {
        if (side == null) return INPUT_s;
        switch (side) {
        case DOWN: return OUTPUT_s;
        case UP: return INPUT_s;
        default: return FUEL_s;
        }
    }
    
    @Override
    public boolean isItemValidForSlot(int slotIndex, ItemStack itemstack) {
        if (slotIndex == 0) {
            return true;
        }
        if (slotIndex == 1) {
            return TileEntityFurnace.isItemFuel(itemstack);
        }
        return false;
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
    public void putData(DataHelper data) throws IOException {
        super.putData(data);
        putSlots(data);
        furnaceBurnTime = data.as(Share.VISIBLE, "burnTime").putInt(furnaceBurnTime);
        furnaceCookTime = data.as(Share.VISIBLE, "cookTime").putInt(furnaceCookTime);
    }

    public boolean isBurning() {
        return furnaceBurnTime > 0;
    }

    boolean prevBurnState = false;
    @Override
    public void update() {
        if (worldObj.isRemote) {
            return;
        }
        if (furnaceBurnTime > 0) {
            furnaceBurnTime--;
        }

        boolean invChanged = false;

        if (this.furnaceBurnTime <= 0 && this.canSmelt()) {
            this.currentFuelItemBurnTime = this.furnaceBurnTime = TileEntityFurnace.getItemBurnTime(this.inv[fuelSlotIndex]) / 2;

            if (this.furnaceBurnTime > 0)
            {
                invChanged = true;

                if (this.inv[fuelSlotIndex] != null)
                {
                    --this.inv[fuelSlotIndex].stackSize;

                    if (this.inv[fuelSlotIndex].stackSize == 0) {
                        this.inv[fuelSlotIndex] = this.inv[fuelSlotIndex].getItem().getContainerItem(inv[fuelSlotIndex]);
                    }
                }
            }
            if (current_recipe != null) {
                current_recipe.onCraftingStart(this);
            }
        }

        if (this.isBurning() && this.canSmelt()) {
            ++this.furnaceCookTime;

            if (this.furnaceCookTime >= 200 || Core.cheat) {
                this.furnaceCookTime = 0;
                this.smeltItem();
                invChanged = true;
            }
        }
        else {
            this.furnaceCookTime = 0;
        }

        boolean burning = isBurning();
        if (prevBurnState != burning) {
            draw_active = (byte) (burning ? 0 : -1);
            drawActive(1);
            prevBurnState = burning;
        }

        if (invChanged) {
            markDirty();
        }
    }

    IVexatiousCrafting<TileEntitySlagFurnace> current_recipe = null;
    public boolean canSmelt() {
        if (this.inv[inputSlotIndex] == null) {
            return false;
        }
        current_recipe = recipes.find(this);
        if (current_recipe == null) {
            return false;
        }
        return current_recipe.isUnblocked(this);
    }

    int getRandomSize(float f) {
        int i = (int) f;
        if (f - i > worldObj.rand.nextFloat()) {
            i += 1;
        }
        return i;
    }

    void smeltItem() {
        if (!canSmelt()) {
            return;
        }
        IVexatiousCrafting<TileEntitySlagFurnace> res = recipes.find(this);
        res.onCraftingComplete(this);
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

    @Override
    public boolean acceptsHeat() {
        return canSmelt();
    }

    @Override
    public void giveHeat() {
        TileEntitySlagFurnace furnace = this;
        final boolean needStart = !isStarted();
        final int topBurnTime = 200;
        if (furnace.furnaceBurnTime < topBurnTime) {
            furnace.furnaceBurnTime += 1;
        } else {
            furnace.furnaceCookTime += 1;
            furnace.furnaceCookTime = Math.min(furnace.furnaceCookTime, 200 - 1);
        }
    }

    @Override
    public boolean hasLaggyStart() {
        return true;
    }

    @Override
    public boolean isStarted() {
        return isBurning();
    }

    public static final CraftingManagerGeneric<TileEntitySlagFurnace> recipes = CraftingManagerGeneric.get(TileEntitySlagFurnace.class);

    public static class SmeltingResult implements IVexatiousCrafting<TileEntitySlagFurnace> {
        public ItemStack input;
        public float prob1, prob2;
        public ItemStack output1, output2;

        public SmeltingResult(ItemStack input, float prob1, ItemStack output1, float prob2, ItemStack output2) {
            this.input = input;
            this.prob1 = prob1;
            this.prob2 = prob2;
            this.output1 = output1;
            this.output2 = output2;
        }

        @Override
        public boolean matches(TileEntitySlagFurnace machine) {
            return ItemUtil.wildcardSimilar(input, machine.inv[inputSlotIndex]);
        }

        @Override
        public void onCraftingStart(TileEntitySlagFurnace machine) {

        }

        @Override
        public void onCraftingComplete(TileEntitySlagFurnace machine) {
            ItemStack[] furnaceItemStacks = machine.inv;
            if (furnaceItemStacks[outputSlotIndex + 0] == null) {
                furnaceItemStacks[outputSlotIndex + 0] = ItemStack.copyItemStack(output1);
                furnaceItemStacks[outputSlotIndex + 0].stackSize = 0;
            }
            if (furnaceItemStacks[outputSlotIndex + 1] == null) {
                furnaceItemStacks[outputSlotIndex + 1] = ItemStack.copyItemStack(output2);
                furnaceItemStacks[outputSlotIndex + 1].stackSize = 0;
            }
            ItemStack fo0 = furnaceItemStacks[outputSlotIndex + 0];
            ItemStack fo1 = furnaceItemStacks[outputSlotIndex + 1];
            fo0.stackSize += machine.getRandomSize(prob1);
            fo1.stackSize += machine.getRandomSize(prob2);
            if (fo0.stackSize > fo0.getMaxStackSize()) {
                fo0.stackSize = fo0.getMaxStackSize();
            }
            if (fo1.stackSize > fo1.getMaxStackSize()) {
                fo1.stackSize = fo1.getMaxStackSize();
            }
            if (fo0.stackSize <= 0) {
                furnaceItemStacks[outputSlotIndex + 0] = null;
            }
            if (fo1.stackSize <= 0) {
                furnaceItemStacks[outputSlotIndex + 1] = null;
            }

            furnaceItemStacks[inputSlotIndex].stackSize -= 1;
            if (furnaceItemStacks[inputSlotIndex].stackSize == 0) {
                furnaceItemStacks[inputSlotIndex] = null;
            }
        }

        boolean checkFit(ItemStack output, ItemStack res, int resSize) {
            if (output == null) {
                return true;
            }
            if (!ItemUtil.couldMerge(output, res)) {
                return false;
            }
            if (output.stackSize + resSize <= output.getMaxStackSize()) {
                return true;
            }
            return false;
        }

        @Override
        public boolean isUnblocked(TileEntitySlagFurnace machine) {
            return checkFit(machine.inv[outputSlotIndex + 0], output1, (int) prob1) && checkFit(machine.inv[outputSlotIndex + 1], output2, (int) prob2);
        }
    }

    public static class SlagRecipes {
        public static ArrayList<SmeltingResult> smeltingResults = (ArrayList<SmeltingResult>) (ArrayList) (recipes.list); // Compatibility!

        static ItemStack obj2is(Object o) {
            if (o instanceof ItemStack) {
                return ItemStack.copyItemStack((ItemStack) o);
            }
            if (o instanceof Block) {
                Block b = (Block) o;
                Item it = DataUtil.getItem(b);
                if (it == null) return null;
                return new ItemStack(it);
            }
            if (o instanceof Item) {
                return new ItemStack((Item) o);
            }
            return null;
        }

        public static void register(Object o_input, float prob1, Object o_output1, float prob2, Object o_output2) {
            ItemStack input = obj2is(o_input), output1 = obj2is(o_output1), output2 = obj2is(o_output2);
            input.stackSize = 1;
            SmeltingResult recipe = new SmeltingResult(input, prob1, output1, prob2, output2);
            recipes.add(recipe);
        }
    }

    @Override
    public boolean handleMessageFromServer(MessageType messageType, ByteBuf input) throws IOException {
        if (super.handleMessageFromServer(messageType, input)) {
            if (messageType == MessageType.DrawActive) {
                getCoord().updateLight();
            }
            return true;
        }
        return false;
    }
    
    @Override
    public boolean rotate(EnumFacing axis) {
        if (axis.getDirectionVec().getY() != 0) {
            return false;
        }
        byte newd = (byte) axis.ordinal();
        if (newd != facing_direction) {
            facing_direction = newd;
            return true;
        }
        return false;
    }
    
    @Override
    public void spawnDisplayTickParticles(Random rand) {
        if (draw_active <= 0) {
            return;
        }
        int direction = facing_direction;
        World w = worldObj;
        float px = pos.getX() + 0.5F;
        float py = pos.getY() + 0.0F + rand.nextFloat() * 6.0F / 16.0F;
        float pz = pos.getZ() + 0.5F;
        float d = 0.52F;
        float rng = rand.nextFloat() * 0.6F - 0.3F;
        
        if (direction == 4) {
            w.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, px - d, py, pz + rng, 0, 0, 0);
            w.spawnParticle(EnumParticleTypes.FLAME, px - d, py, pz + rng, 0, 0, 0);
        } else if (direction == 5) {
            w.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, px + d, py, pz + rng, 0, 0, 0);
            w.spawnParticle(EnumParticleTypes.FLAME, px + d, py, pz + rng, 0, 0, 0);
        } else if (direction == 2) {
            w.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, px + rng, py, pz - d, 0, 0, 0);
            w.spawnParticle(EnumParticleTypes.FLAME, px + rng, py, pz - d, 0, 0, 0);
        } else if (direction == 3) {
            w.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, px + rng, py, pz + d, 0, 0, 0);
            w.spawnParticle(EnumParticleTypes.FLAME, px + rng, py, pz + d, 0, 0, 0);
        }
    }

    @Override
    public int getDynamicLight() {
        return isBurning() ? 13 : 0;
    }
}
