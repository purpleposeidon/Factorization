package factorization.common;

import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ICrafting;

public class ContainerGrinder extends ContainerFactorization {
    int lastProgress = -1;
    public TileEntityGrinder grinder;
    public ContainerGrinder(EntityPlayer entityplayer, TileEntityFactorization grinder) {
        super(entityplayer, grinder.getFactoryType());
        this.grinder = (TileEntityGrinder) grinder; 
    }

    @Override
    public void updateCraftingResults() {
        super.updateCraftingResults();
        for (ICrafting crafter : (Iterable<ICrafting>) crafters) {
            if (grinder.progress != lastProgress) {
                crafter.sendProgressBarUpdate(this, 0, grinder.progress);
            }
        }
        lastProgress = grinder.progress;
    }
    
    @Override
    public void updateProgressBar(int index, int val) {
        if (index == 0) {
            grinder.progress = val;
        }
    }
}
