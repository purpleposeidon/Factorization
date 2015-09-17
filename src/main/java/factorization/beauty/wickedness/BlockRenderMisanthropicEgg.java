package factorization.beauty.wickedness;

import factorization.api.Coord;
import factorization.beauty.wickedness.api.EvilBit;
import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.shared.BlockRenderHelper;
import factorization.shared.FactorizationBlockRender;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.util.IIcon;

public class BlockRenderMisanthropicEgg extends FactorizationBlockRender {
    @Override
    public boolean render(RenderBlocks rb) {
        TileEntityMisanthropicEgg egg;
        EvilBit eeeeviiill = null;
        if (world_mode) {
            egg = (TileEntityMisanthropicEgg) te;
            eeeeviiill = egg.evilBit;
        } else {
            egg = (TileEntityMisanthropicEgg) FactoryType.MISANTHROPIC_EGG.getRepresentative();
            if (is.hasTagCompound() && egg != null) {
                egg.loadFromStack(is);
                eeeeviiill = egg.evilBit;
            }
        }
        int color1 = 0xFF0000;
        int color2 = 0x202020;
        if (eeeeviiill != null) {
            color1 = eeeeviiill.getMainColor();
            color2 = eeeeviiill.getSecondColor();
        }
        boolean ret = drawShell(rb, BlockIcons.beauty$egg_shell, color1) & drawShell(rb, BlockIcons.beauty$egg_spot, color2);
        BlockRenderHelper.instance.resetColors();
        return ret;
    }

    boolean drawShell(RenderBlocks rb, IIcon icon, int color) {
        final BlockRenderHelper block = BlockRenderHelper.instance;
        int bottom = 0;
        boolean anyDraw = false;
        block.useTexture(icon);
        block.setColor(color);

        final Coord at = getCoord();
        for (int layer = 0; layer < 8; ++layer) {
            byte XZ = 0;
            byte height = 1;

            if (layer == 0) {
                XZ = 2;
            } else if (layer == 1) {
                XZ = 3;
            } else if (layer == 2) {
                XZ = 4;
            } else if (layer == 3) {
                XZ = 5;
                height = 2;
            } else if (layer == 4) {
                XZ = 6;
                height = 3;
            } else if (layer == 5) {
                XZ = 7;
                height = 5;
            } else if (layer == 6) {
                XZ = 6;
                height = 2;
            } else if (layer == 7) {
                XZ = 3;
            }

            double XZpix = XZ / 16.0;
            double Y0pix = 1.0 - bottom / 16.0;
            double Y1pix = 1.0 - (bottom + height) / 16.0;
            bottom += height;
            block.setBlockBounds((float) (0.5 - XZpix), (float) Y1pix, (float) (0.5 - XZpix), (float) (0.5 + XZpix), (float) Y0pix, (float) (0.5 + XZpix));
            if (world_mode) {
                anyDraw |= block.render(rb, at);
            } else {
                block.renderForInventory(rb);
            }
        }
        return anyDraw;
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.MISANTHROPIC_EGG;
    }
}
