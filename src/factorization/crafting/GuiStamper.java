package factorization.crafting;

import org.lwjgl.opengl.GL11;

import factorization.common.ContainerFactorization;
import factorization.common.FactoryType;
import factorization.shared.Core;
import factorization.shared.FactorizationGui;

public class GuiStamper extends FactorizationGui {
    public GuiStamper(ContainerFactorization container) {
        super(container);
    }
    
    @Override
    protected void drawGuiContainerBackgroundLayer(float f, int i, int j) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        Core.bindGuiTexture("stampergui");
        int l = (width - xSize) / 2;
        int i1 = (height - ySize) / 2;
        drawTexturedModalRect(l, i1, 0, 0, xSize, ySize);
        FactoryType t = factContainer.factory.getFactoryType();
        if (t == FactoryType.STAMPER) {
            drawTexturedModalRect(l + 26, i1 + 45, 178, 19, 16, 16);
        }
        if (t == FactoryType.PACKAGER) {
            drawTexturedModalRect(l + 11, i1 + 45, 178, 35, 25, 10);
        }
    }

}
