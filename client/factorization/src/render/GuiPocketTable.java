package factorization.src.render;

import net.minecraft.src.GuiContainer;

import org.lwjgl.opengl.GL11;

import factorization.src.Command;
import factorization.src.ContainerPocket;
import factorization.src.Core;

public class GuiPocketTable extends GuiContainer {
	public ContainerPocket containerPocket;

	public GuiPocketTable(ContainerPocket container) {
		super(container);
		containerPocket = container;
		xSize = 236;
		ySize = 89;
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float f, int i, int j) {
		int k = mc.renderEngine.getTexture(Core.texture_dir + "pocketgui.png");
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		mc.renderEngine.bindTexture(k);
		int l = (width - xSize) / 2;
		int i1 = (height - ySize) / 2;
		drawTexturedModalRect(l, i1, 0, 0, xSize, ySize);
	}

	@Override
	protected void drawGuiContainerForegroundLayer() {
		super.drawGuiContainerForegroundLayer();
		// I'd like it to say "Pocket Crafting", but that doesn't fit.
		// Could also make the tab a bit longer...
		// this.fontRenderer.drawString("Crafting", 178, 10, 4210752);
		this.fontRenderer.drawString("PcktCrftr", 178, 10, 4210752);
		// this.fontRenderer.drawString("123456789", 178, 10, 4210752);
		// we can fit only that much
		// also maybe draw a nice reference for keys to press for below
	}

	@Override
	protected void keyTyped(char key, int par2) {
		// 'x' clears items out of the way. Fill inv, then bag (and make slurp
		// sound). [XXX TODO -- Doing this server-friendly'd require a packet or
		// something]
		// 'f' in crafting area, balance items out. Then fill in with rest of
		// inventory.
		// 'z' cycle layout, based on outer edge:
		// - Full: rotate
		// - In a '\' corner: spread left/right
		// - In a '/' corner: spread up/down
		// - A line along a side: spread to the other side, skipping the middle.
		// - Two touching: fill the circumerfence, alternating.
		// - middle of a side: spread across center
		if (key == 'x') {
			Command.craftClear.call(mc.thePlayer);
			return;
		}
		if (key == 'c') {
			Command.craftMove.call(mc.thePlayer);
			return;
		}
		if (key == 'f') {
			Command.craftBalance.call(mc.thePlayer);
			return;
		}
		super.keyTyped(key, par2);
	}
}
