package factorization.common;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ICrafting;

public class ContainerMixer extends ContainerFactorization {
    public TileEntityMixer mixer;
    public ContainerMixer(EntityPlayer entityplayer, TileEntityFactorization tef) {
        super(entityplayer, tef);
        mixer = (TileEntityMixer) tef;
    }

    int last_progress = -100;
    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        for (ICrafting crafter : (Iterable<ICrafting>) crafters) {
            if (mixer.progress != last_progress) {
                crafter.sendProgressBarUpdate(this, 0, mixer.progress);	
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
