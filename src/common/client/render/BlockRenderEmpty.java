package factorization.client.render;

import net.minecraft.client.renderer.RenderBlocks;
import factorization.common.FactoryType;

public class BlockRenderEmpty extends FactorizationBlockRender {
    FactoryType for_type;
    public BlockRenderEmpty(FactoryType for_type) {
        super(for_type);
        this.for_type = for_type;
    }
    @Override
    void render(RenderBlocks rb) { }

    @Override
    FactoryType getFactoryType() {
        return for_type;
    }

}
