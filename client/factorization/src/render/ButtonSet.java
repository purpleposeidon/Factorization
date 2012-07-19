package factorization.src.render;

import java.util.ArrayList;

import net.minecraft.client.Minecraft;
import net.minecraft.src.GuiButton;
import factorization.src.Sound;

public class ButtonSet {
	ArrayList<GuiButton> buttons = new ArrayList();
	int focused_id = -1;

	public int currentTop = 0;
	public int currentRight = 0;

	GuiButton add(int id, int xPos, int yPos, int width, int height, String text) {
		if (xPos == -1) {
			xPos = currentRight;
		}
		if (yPos == -1) {
			yPos = currentTop;
		}
		GuiButton ret = new GuiButton(id, xPos, yPos, width, height, text);
		buttons.add(ret);
		currentTop = yPos;
		currentRight = xPos + width;
		return ret;
	}

	void clear() {
		buttons.clear();
	}

	void draw(Minecraft mc, int i, int j) {
		focused_id = -1;
		for (GuiButton button : buttons) {
			button.drawButton(mc, i, j);
			if (button.mousePressed(mc, i, j)) {
				focused_id = button.id;
			}
		}
	}

	void handleClick(IClickable gui, Minecraft mc, int x, int y, int button) {
		if (button != 0 && button != 1) {
			return;
		}
		for (GuiButton guibutton : buttons) {
			if (guibutton.mousePressed(mc, x, y)) {
				if (button == 1) {
					Sound.leftClick.play();
				} else {
					Sound.rightClick.play();
				}
				gui.actionPerformedMouse(guibutton, button == 1);
			}
		}
	}
}
