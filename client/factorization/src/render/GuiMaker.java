package factorization.src.render;

import java.util.ArrayList;

import net.minecraft.src.GuiButton;
import net.minecraft.src.GuiContainer;

import org.lwjgl.opengl.GL11;

import factorization.src.ContainerFactorization;
import factorization.src.Core;
import factorization.src.Sound;
import factorization.src.TileEntityMaker;

public class GuiMaker extends GuiContainer {
	TileEntityMaker maker;
	ArrayList<GuiButton> buttons = new ArrayList<GuiButton>();
	ContainerFactorization cont;

	public GuiMaker(ContainerFactorization cont) {
		super(cont);
		this.maker = (TileEntityMaker) cont.factory;
		this.cont = cont;
	}

	@Override
	public void initGui() {
		super.initGui();
		int buttons_left = guiLeft + 86, buttons_top = guiTop + 18;
		int button_size = 18;
		int id = 0;
		buttons.clear();
		for (int y = 0; y < 3; y++) {
			for (int x = 0; x < 3; x++) {
				GuiButton add;
				add = new GuiButton(id, buttons_left + button_size * x,
						buttons_top + button_size * y, button_size,
						button_size, "");
				buttons.add(add);
				id++;
			}
		}
	}

	int getPageSize() {
		if (maker.fuel == 0) {
			return 0;
		}
		int ret = (int) Math.log(maker.fuel);
		if (12 < ret) {
			ret = 12;
		}
		return ret;
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float f, int i, int j) {
		int k = mc.renderEngine
				.getTexture(Core.texture_dir + "makergui.png");
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		mc.renderEngine.bindTexture(k);
		int l = (width - xSize) / 2;
		int i1 = (height - ySize) / 2;
		drawTexturedModalRect(l, i1, 0, 0, xSize, ySize);

		// draw pages
		int pageSize = getPageSize();
		drawTexturedModalRect(l + 58, (i1 + 39) + 12 - pageSize, // x, y
				184, 12 - pageSize, // u, v (seems to be texture offset)
				15, pageSize // width, height
		);

		// draw buttons
		updateGui();
		for (GuiButton button : buttons) {
			button.drawButton(mc, i, j);
		}
	}

	protected void drawGuiContainerForegroundLayer() {
		fontRenderer.drawString("Target slot", 84, 6, 0x404040);
		String paper = "No paper"; // Pa-pa-pa-paper!

		if (maker.fuel == 1) {
			paper = "1 page";
		}
		if (maker.fuel > 1) {
			if (maker.fuel > 1000) {
				java.text.DecimalFormat formatter = new java.text.DecimalFormat(
						"0.#E0");
				paper = formatter.format(maker.fuel) + " pgs";
			} else if (maker.fuel > 99) {
				paper = maker.fuel + " pages";
			} else {
				paper = maker.fuel + " pages";
			}
		}
		if (maker.fuel < 0) {
			paper = "Reams!"; // Well-prepared!
		}

		fontRenderer.drawString(paper, 6, 40, 0x404040);
		fontRenderer.drawString("Maker", 6, 6, 0x404040);
		fontRenderer.drawString("X", 48, 23, 0x0);
	}

	private void updateGui() {
		for (int i = 0; i < 9; i++) {
			buttons.get(i).displayString = maker.targets[i] ? "X" : "";
		}
	}

	@Override
	protected void actionPerformed(GuiButton guibutton) {
		maker.setTargets(guibutton.id, !maker.targets[guibutton.id]);
		//XXX TODO: Just turned this off...
		//maker.doLogic();
	}

	@Override
	protected void mouseClicked(int x, int y, int button) {
		super.mouseClicked(x, y, button);
		for (GuiButton guibutton : buttons) {
			if (guibutton.mousePressed(mc, x, y)) {
				if (button == 1) {
					Sound.leftClick.play();
				} else {
					Sound.rightClick.play();
				}
				actionPerformed(guibutton);
			}
		}
	}
}
