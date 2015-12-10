package factorization.shared;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

public class EmptyRender extends Render<Entity> {

    public EmptyRender(RenderManager renderManager) {
        super(renderManager);
    }

    @Override
    public void doRender(Entity var1, double var2, double var4, double var6, float var8, float var9) {
        //renderManager.getEntityClassRenderObject(Entity.class).doRender(var1, var2, var4, var6, var8, var9);
    }
    
    @Override
    protected ResourceLocation getEntityTexture(Entity entity) { return null; }
}
