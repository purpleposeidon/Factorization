package factorization.oreprocessing;

import net.minecraft.client.gui.inventory.GuiContainer;

import org.lwjgl.opengl.GL11;

import factorization.common.ContainerFactorization;
import factorization.shared.Core;

public class GuiGrinder extends GuiContainer {
    TileEntityGrinder grinder;

    public GuiGrinder(ContainerFactorization cont) {
        super(cont);
        grinder = (TileEntityGrinder) cont.factory;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float var1, int var2, int var3) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        Core.bindGuiTexture("grinder");
        int var5 = (this.width - this.xSize) / 2;
        int var6 = (this.height - this.ySize) / 2;
        this.drawTexturedModalRect(var5, var6, 0, 0, this.xSize, this.ySize);
        int var7;
        var7 = grinder.getGrindProgressScaled(24);
        this.drawTexturedModalRect(var5 + 79, var6 + 34, 176, 14, var7 + 1, 16);
    }

}
