package factorization.common;

import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ICrafting;

public class ContainerMixer extends ContainerFactorization {
    public TileEntityMixer mixer;
    public ContainerMixer(EntityPlayer entityplayer, TileEntityFactorization tef) {
        super(entityplayer, tef.getFactoryType());
        mixer = (TileEntityMixer) tef;
    }

    int last_progress = -100;
    @Override
    public void updateCraftingResults() {
        super.updateCraftingResults();
        for (ICrafting crafter : (Iterable<ICrafting>) crafters) {
            if (mixer.progress != last_progress) {
                crafter.updateCraftingInventoryInfo(this, 0, mixer.progress);	
            }
        }
        last_progress = mixer.progress;
    }
    
    @Override
    public void updateProgressBar(int key, int val) {
        super.updateProgressBar(key, val);
        if (key == 0) {
            mixer.progress = val;
        }
    }
}
