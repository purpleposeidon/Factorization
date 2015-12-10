package factorization.colossi;

import factorization.shared.EmptyRender;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.BossStatus;

public class ColossusControllerRenderer extends EmptyRender {
    public ColossusControllerRenderer(RenderManager renderManager) {
        super(renderManager);
    }

    @Override
    public void doRender(Entity ent, double cameraX, double cameraY, double cameraZ, float yaw, float partial) {
        ColossusController controller = (ColossusController) ent;
        BossStatus.setBossStatus(controller, false);
    }
}
