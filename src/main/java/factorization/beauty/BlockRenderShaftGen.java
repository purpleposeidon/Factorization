package factorization.beauty;

import factorization.api.FzOrientation;
import factorization.api.Quaternion;
import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.shared.BlockRenderHelper;
import factorization.shared.FactorizationBlockRender;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.util.EnumFacing;

public class BlockRenderShaftGen extends FactorizationBlockRender {
    @Override
    public boolean render(RenderBlocks rb) {
        BlockRenderHelper block = BlockRenderHelper.instance;
        boolean on = true;
        EnumFacing shaft = EnumFacing.UP;
        if (world_mode) {
            Tessellator.instance.setBrightness(block.getMixedBrightnessForBlock(w, x, y, z));
            TileEntityShaftGen gen = (TileEntityShaftGen) te;
            on = gen.on;
            shaft = gen.shaft_direction.getOpposite();
        }
        IIcon top = BlockIcons.beauty$shaft_gen_top;
        IIcon side = on ? BlockIcons.beauty$shaft_gen_side_on : BlockIcons.beauty$shaft_gen_side;
        IIcon bottom = on ? BlockIcons.beauty$shaft_gen_bottom_on : BlockIcons.beauty$shaft_gen_bottom;
        block.setBlockBoundsOffset(0, 0, 0);
        block.useTextures(top, bottom, side, side, side, side);
        if (world_mode) {
            block.beginWithMirroredUVs();
            FzOrientation fzo = FzOrientation.fromDirection(shaft);
            block.rotateCenter(Quaternion.fromOrientation(fzo));
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
