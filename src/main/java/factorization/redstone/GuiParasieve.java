package factorization.redstone;

import factorization.redstone.TileEntityParaSieve;
import org.lwjgl.opengl.GL11;

import factorization.common.ContainerFactorization;
import factorization.shared.Core;
import factorization.shared.FactorizationGui;

public class GuiParasieve extends FactorizationGui {
    TileEntityParaSieve proto;
    public GuiParasieve(ContainerFactorization container) {
        super(container);
        proto = (TileEntityParaSieve) factContainer.factory;
        xSize = 175;
        ySize = 148;
    }
    
    @Override
    protected void drawGuiContainerBackgroundLayer(float var1, int var2, int var3) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        Core.bindGuiTexture("parasievegui");
        int var5 = (this.width - this.xSize) / 2;
        int var6 = (this.height - this.ySize) / 2;
        this.drawTexturedModalRect(var5, var6, 0, 0, this.xSize, this.ySize);
    }
}
