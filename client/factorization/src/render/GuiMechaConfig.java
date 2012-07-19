package factorization.src.render;

import net.minecraft.src.Container;
import net.minecraft.src.GuiContainer;

import org.lwjgl.opengl.GL11;

import factorization.src.ContainerMechaModder;
import factorization.src.Core;

public class GuiMechaConfig extends GuiContainer {
	ContainerMechaModder cont;

	public GuiMechaConfig(Container cont) {
		super(cont);
		this.cont = (ContainerMechaModder) cont;
		xSize = 175;
		ySize = 197;
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float var1, int var2, int var3) {
		int k = mc.renderEngine.getTexture(Core.texture_dir + "mechamodder.png");
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		mc.renderEngine.bindTexture(k);
		int l = (width - xSize) / 2;
		int i1 = (height - ySize) / 2;
		drawTexturedModalRect(l, i1, 0, 0, xSize, ySize);
	}

	@Override
	protected void drawGuiContainerForegroundLayer() {
		super.drawGuiContainerForegroundLayer();
		this.fontRenderer.drawString("Mecha-Modder", 7, 26, 4210752);
	}

}
