package factorization.colossi;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.BossStatus;
import factorization.shared.EmptyRender;

public class ColossusControllerRenderer extends EmptyRender {
    @Override
    public void doRender(Entity ent, double cameraX, double cameraY, double cameraZ, float yaw, float partial) {
        ColossusController controller = (ColossusController) ent;
        BossStatus.setBossStatus(controller, false);
    }
}
