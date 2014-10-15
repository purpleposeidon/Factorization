package factorization.colossi;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.BossStatus;
import factorization.shared.EmptyRender;

public class ColossusControllerRenderer extends EmptyRender {
    @Override
    public void doRender(Entity ent, double var2, double var4, double var6, float var8, float var9) {
        ColossusController controller = (ColossusController) ent;
        BossStatus.setBossStatus(controller, true);
    }
}
