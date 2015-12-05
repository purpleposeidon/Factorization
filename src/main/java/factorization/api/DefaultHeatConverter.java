package factorization.api;

import net.minecraft.block.BlockFurnace;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

class DefaultHeatConverter implements HeatConverters.IHeatConverter {
    @Override
    public IFurnaceHeatable convert(World w, BlockPos pos) {
        TileEntity te = w.getTileEntity(pos);
        if (te instanceof IFurnaceHeatable) return (IFurnaceHeatable) te;
        if (te instanceof TileEntityFurnace) return new FurnaceHeating((TileEntityFurnace) te);
        return null;
    }

    private static class FurnaceHeating implements IFurnaceHeatable {
        final TileEntityFurnace furnace;
        FurnaceHeating(TileEntityFurnace furnace) {
            this.furnace = furnace;
        }

        @Override
        public boolean acceptsHeat() {
            // copy of private function for TileEntityFurnace.canSmelt. Lame. AT it?
            final ItemStack inputItem = furnace.getStackInSlot(0);
            if (inputItem == null) return false;
            ItemStack smeltOutput = FurnaceRecipes.instance().getSmeltingResult(inputItem);
            if (smeltOutput == null) return false;
            final ItemStack outputSlot = furnace.getStackInSlot(2);
            if (outputSlot == null) return true;
            if (!outputSlot.isItemEqual(smeltOutput) /* no NBT okay (vanilla source) */ ) return false;
            int result = outputSlot.stackSize + smeltOutput.stackSize;
            return (result <= furnace.getInventoryStackLimit() && result <= smeltOutput.getMaxStackSize());
        }

        @Override
        public void giveHeat() {
            final boolean needStart = !isStarted();
            final int topBurnTime = 200;
            if (furnace.furnaceBurnTime < topBurnTime) {
                furnace.furnaceBurnTime += 1;
                if (needStart) {
                    BlockFurnace.setState(furnace.furnaceBurnTime > 0, furnace.getWorld(), furnace.getPos());
                }
            } else {
                furnace.cookTime += 1;
                furnace.cookTime = Math.min(furnace.cookTime, 200 - 1);
            }
        }

        @Override
        public boolean hasLaggyStart() {
            return true;
        }

        @Override
        public boolean isStarted() {
            return furnace.isBurning();
        }
    }
}
