package factorization.common;

import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ICrafting;

public class ContainerSlagFurnace extends ContainerFactorization {
    int lastBurnTime = -1, lastFuelItemBurnTime = -1, lastCookTime = -1;
    TileEntitySlagFurnace furnace;

    public ContainerSlagFurnace(EntityPlayer entityplayer, TileEntityFactorization factory) {
        super(entityplayer, factory);
        furnace = (TileEntitySlagFurnace) factory;
    }

    @Override
    public void updateCraftingResults() {
        super.updateCraftingResults();
        //		System.out.println(crafters.size());
        for (ICrafting crafter : (Iterable<ICrafting>) crafters) {
            if (furnace.furnaceBurnTime != lastBurnTime) {
                crafter.sendProgressBarUpdate(this, 0, furnace.furnaceBurnTime);
                //				System.out.println(crafter + " burn time is now " + furnace.furnaceBurnTime);
            }
            if (furnace.furnaceCookTime != lastCookTime) {
                crafter.sendProgressBarUpdate(this, 1, furnace.furnaceCookTime);
            }
            if (furnace.currentFuelItemBurnTime != lastFuelItemBurnTime) {
                crafter.sendProgressBarUpdate(this, 2, furnace.currentFuelItemBurnTime);
            }
        }
        lastBurnTime = furnace.furnaceBurnTime;
        lastCookTime = furnace.furnaceCookTime;
        lastFuelItemBurnTime = furnace.currentFuelItemBurnTime;
    }

    @Override // -- stupid server
    public void updateProgressBar(int index, int val) {
        //		System.out.println("Updated progress bar! " + index + " to " + val);
        switch (index) {
        case 0:
            furnace.furnaceBurnTime = val;
            break;
        case 1:
            furnace.furnaceCookTime = val;
            break;
        case 2:
            furnace.currentFuelItemBurnTime = val;
            break;
        }
    }
}
