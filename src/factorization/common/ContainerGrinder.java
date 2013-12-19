package factorization.common;

import factorization.shared.TileEntityFactorization;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ICrafting;

public class ContainerGrinder extends ContainerFactorization {
    int lastProgress = -1;
    public TileEntityGrinder grinder;
    public ContainerGrinder(EntityPlayer entityplayer, TileEntityFactorization grinder) {
        super(entityplayer, grinder);
        this.grinder = (TileEntityGrinder) grinder; 
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
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
