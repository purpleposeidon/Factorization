package factorization.client.gui;

import org.lwjgl.opengl.GL11;

import factorization.common.ContainerFactorization;
import factorization.common.Core;
import factorization.common.TileEntitySlagFurnace;

public class GuiSlag extends FactorizationGui {
    TileEntitySlagFurnace furnace;

    public GuiSlag(ContainerFactorization cont) {
        super(cont);
        furnace = (TileEntitySlagFurnace) cont.factory;
    }

    protected void drawGuiContainerForegroundLayer() {
        fontRenderer.drawString(factContainer.factory.getInvName(), 60, 6, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float var1, int var2, int var3) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        Core.bindGuiTexture("slagfurnacegui");
        int var5 = (this.width - this.xSize) / 2;
        int var6 = (this.height - this.ySize) / 2;
        this.drawTexturedModalRect(var5, var6, 0, 0, this.xSize, this.ySize);
        int var7;

        if (furnace.isBurning())
        {
            var7 = this.furnace.getBurnTimeRemainingScaled(12);
            this.drawTexturedModalRect(var5 + 56, var6 + 36 + 12 - var7, 176, 12 - var7, 14, var7 + 2);
        }

        var7 = this.furnace.getCookProgressScaled(24);
        this.drawTexturedModalRect(var5 + 79, var6 + 34, 176, 14, var7 + 1, 16);
    }

}
