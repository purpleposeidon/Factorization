package factorization.crafting;

import org.lwjgl.opengl.GL11;

import factorization.shared.Core;
import factorization.shared.FactorizationGui;

public class GuiMixer extends FactorizationGui {
    TileEntityMixer mixer;

    public GuiMixer(ContainerMixer cont) {
        super(cont);
        mixer = cont.mixer;
    }

    protected void drawGuiContainerForegroundLayer() {
        fontRendererObj.drawString(mixer.getInvName(), 60, 6, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float var1, int var2, int var3) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        Core.bindGuiTexture("mixer");
        int var5 = (this.width - this.xSize) / 2;
        int var6 = (this.height - this.ySize) / 2;
        this.drawTexturedModalRect(var5, var6, 0, 0, this.xSize, this.ySize);
        int var7;
        var7 = mixer.getMixProgressScaled(24);
        this.drawTexturedModalRect(var5 + 79, var6 + 34, 176, 14, var7 + 1, 16);
    }

}
