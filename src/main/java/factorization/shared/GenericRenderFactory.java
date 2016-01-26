package factorization.shared;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.client.registry.IRenderFactory;

public class GenericRenderFactory<T extends Entity> implements IRenderFactory<T> {
    final Class<? extends Render<T>> renderClass;

    public GenericRenderFactory(Class<? extends Render<T>> renderClass) {
        this.renderClass = renderClass;
    }

    @Override
    public Render<? super T> createRenderFor(RenderManager manager) {
        try {
            Render<? super T> ret = renderClass.newInstance();
            Core.loadBus(ret);
            return ret;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }
}
