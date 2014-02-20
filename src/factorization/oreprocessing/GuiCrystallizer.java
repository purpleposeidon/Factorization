package factorization.oreprocessing;

import org.lwjgl.opengl.GL11;

import factorization.common.ContainerFactorization;
import factorization.shared.Core;
import factorization.shared.FactorizationGui;

public class GuiCrystallizer extends FactorizationGui {
    TileEntityCrystallizer crys;

    public GuiCrystallizer(ContainerFactorization container) {
        super(container);
        crys = (TileEntityCrystallizer) factContainer.factory;
        xSize = 175;
        ySize = 188;
    }
    
    @Override
    protected void drawGuiContainerForegroundLayer(int foo, int bar) {
        fontRendererObj.drawString(factContainer.factory.getInvName(), 8, 6, 0x404040);
        //"inventory" doesn't fit.
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float var1, int var2, int var3) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        Core.bindGuiTexture("crystal");
        int var5 = (this.width - this.xSize) / 2;
        int var6 = (this.height - this.ySize) / 2;
        this.drawTexturedModalRect(var5, var6, 0, 0, this.xSize, this.ySize);

        int progress = (int) (crys.getProgress() * 90);
        this.drawTexturedModalRect(var5 + 43, var6 + 89, 0, 192, progress, 16);

        float h = crys.heat / (float) TileEntityCrystallizer.topHeat;
        int heat = (int) ((1 - h) * 13);
        for (int dx : new int[] { 54, 109 }) {
            this.drawTexturedModalRect(var5 + dx, var6 + 75 + heat, 176, 0 + heat, 14, 13 - heat);
        }
    }

}
