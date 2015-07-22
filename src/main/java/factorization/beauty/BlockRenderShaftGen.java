package factorization.beauty;

import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.shared.BlockRenderHelper;
import factorization.shared.FactorizationBlockRender;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;

public class BlockRenderShaftGen extends FactorizationBlockRender {
    @Override
    public boolean render(RenderBlocks rb) {
        BlockRenderHelper block = BlockRenderHelper.instance;
        boolean on = true;
        if (world_mode) {
            TileEntityShaftGen gen = (TileEntityShaftGen) te;
            on = gen.on;
        }
        IIcon top = BlockIcons.beauty$shaft_gen_top;
        IIcon side = on ? BlockIcons.beauty$shaft_gen_side_on : BlockIcons.beauty$shaft_gen_side;
        IIcon bottom = on ? BlockIcons.beauty$shaft_gen_bottom_on : BlockIcons.beauty$shaft_gen_bottom;
        block.setBlockBoundsOffset(0, 0, 0);
        block.useTextures(top, bottom, side, side, side, side);
        if (world_mode) {
            block.beginWithMirroredUVs();
            block.renderRotated(Tessellator.instance, x, y, z);
        } else {
            block.renderForInventory(rb);
        }
        return true;
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.SHAFT_GEN;
    }
}
