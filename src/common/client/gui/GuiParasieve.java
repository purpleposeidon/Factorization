package factorization.client.gui;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;

import org.lwjgl.opengl.GL11;

import factorization.common.ContainerFactorization;
import factorization.common.Core;
import factorization.common.TileEntityParaSieve;

public class GuiParasieve extends GuiContainer {
    ContainerFactorization factContainer;
    TileEntityParaSieve proto;
    public GuiParasieve(Container container) {
        super(container);
        factContainer = (ContainerFactorization) container;
        proto = (TileEntityParaSieve) factContainer.factory;
        xSize = 175;
        ySize = 148;
    }

    protected void drawGuiContainerForegroundLayer() {
        fontRenderer.drawString(proto.getInvName(), 6, 6, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float var1, int var2, int var3) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        Core.bindGuiTexture("parasievegui");
        int var5 = (this.width - this.xSize) / 2;
        int var6 = (this.height - this.ySize) / 2;
        this.drawTexturedModalRect(var5, var6, 0, 0, this.xSize, this.ySize);
        drawGuiContainerForegroundLayer();
    }
}
