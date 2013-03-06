package factorization.client.gui;

import net.minecraft.client.gui.inventory.GuiContainer;

import org.lwjgl.opengl.GL11;

import factorization.common.ContainerFactorization;
import factorization.common.Core;
import factorization.common.FactoryType;

public class GuiStamper extends GuiContainer {
    ContainerFactorization factContainer;

    public GuiStamper(ContainerFactorization container) {
        super(container);
        factContainer = container;
    }

    protected void drawGuiContainerForegroundLayer() {
        fontRenderer.drawString(factContainer.factory.getInvName(), 60, 12 /* 12 */, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float f, int i, int j) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.renderEngine.bindTextureFile(Core.texture_dir + "stampergui.png");
        int l = (width - xSize) / 2;
        int i1 = (height - ySize) / 2;
        drawTexturedModalRect(l, i1, 0, 0, xSize, ySize);
        FactoryType t = factContainer.factory.getFactoryType();
        if (t == FactoryType.STAMPER) {
            drawTexturedModalRect(l + 26, i1 + 45, 178, 19, 16, 16);
        }
        if (t == FactoryType.PACKAGER) {
            drawTexturedModalRect(l + 11, i1 + 45, 178, 35, 19, 10);
        }
    }

}
