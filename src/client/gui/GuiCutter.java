package factorization.client.gui;

import net.minecraft.src.GuiContainer;

import org.lwjgl.opengl.GL11;

import factorization.common.ContainerFactorization;
import factorization.common.Core;
import factorization.common.TileEntityCutter;

public class GuiCutter extends GuiContainer {
    TileEntityCutter cutter;

    public GuiCutter(ContainerFactorization container) {
        super(container);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float f, int i, int j) {
        int k = mc.renderEngine.getTexture(Core.texture_dir + "cuttergui.png");
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.renderEngine.bindTexture(k);
        int l = (width - xSize) / 2;
        int i1 = (height - ySize) / 2;
        drawTexturedModalRect(l, i1, 0, 0, xSize, ySize);
    }
}
