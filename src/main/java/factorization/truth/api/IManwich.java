package factorization.truth.api;

import net.minecraft.entity.player.EntityPlayer;

public interface IManwich {
    String getManwichDomain(EntityPlayer player);
    void recommendManwich(EntityPlayer player);
    int hasManual(EntityPlayer player);
}
