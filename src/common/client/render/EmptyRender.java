package factorization.client.render;

import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.client.renderer.entity.Render;

public class EmptyRender extends Render {

    @Override
    public void doRender(Entity var1, double var2, double var4, double var6, float var8, float var9) {
        //renderManager.getEntityClassRenderObject(Entity.class).doRender(var1, var2, var4, var6, var8, var9);
    }
    
    @Override
    protected ResourceLocation getEntityTexture(Entity entity) { return null; }
}
