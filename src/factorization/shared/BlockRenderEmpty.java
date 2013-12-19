package factorization.shared;

import net.minecraft.client.renderer.RenderBlocks;

public class BlockRenderEmpty extends FactorizationBlockRender {
    FactoryType for_type;
    public BlockRenderEmpty(FactoryType for_type) {
        super(for_type);
        this.for_type = for_type;
    }
    @Override
    public void render(RenderBlocks rb) { }

    @Override
    public FactoryType getFactoryType() {
        return for_type;
    }

}
