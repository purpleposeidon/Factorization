package factorization.shared;

import factorization.common.FactoryType;
import net.minecraft.client.renderer.RenderBlocks;

public class BlockRenderEmpty extends FactorizationBlockRender {
    FactoryType for_type;
    public BlockRenderEmpty(FactoryType for_type) {
        super(for_type);
        this.for_type = for_type;
    }
    @Override
    public boolean render(RenderBlocks rb) { return false; }

    @Override
    public FactoryType getFactoryType() {
        return for_type;
    }

}
