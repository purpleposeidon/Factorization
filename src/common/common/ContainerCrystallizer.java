package factorization.common;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ICrafting;

public class ContainerCrystallizer extends ContainerFactorization {
    TileEntityCrystallizer crys;

    public ContainerCrystallizer(EntityPlayer entityplayer, TileEntityFactorization factory) {
        super(entityplayer, factory);
        crys = (TileEntityCrystallizer) factory;
        invdy += 23;
    }

    int last_heat, last_progress;

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        for (ICrafting crafter : (Iterable<ICrafting>) crafters) {
            if (crys.heat != last_heat) {
                crafter.sendProgressBarUpdate(this, 0, crys.heat);
            }
            if (crys.progress != last_progress) {
                crafter.sendProgressBarUpdate(this, 1, crys.progress);
            }
        }
        last_progress = crys.progress;
        last_heat = crys.heat;
    }

    @Override
    public void updateProgressBar(int id, int val) {
        super.updateProgressBar(id, val);
        if (id == 0) {
            crys.heat = val;
        }
        if (id == 1) {
            crys.progress = val;
        }
    }
}
