package factorization.client.gui;

import java.util.ArrayList;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.tileentity.TileEntity;
import factorization.common.Sound;

public class ButtonSet {
    ArrayList<Gui> buttons = new ArrayList();
    int focused_id = -1;

    public int currentTop = 0;
    public int currentRight = 0;

    interface Predicate<A> {
        boolean test(A a);
    }

    Predicate<TileEntity> showTest = null;

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

    GuiLabel add(int x, int y, String text) {
        GuiLabel ret = new GuiLabel(x, y, text);
        buttons.add(ret);
        return ret;
    }

    boolean canShow(TileEntity te) {
        if (showTest == null) {
            return true;
        }
        return showTest.test(te);
    }

    void setTest(Predicate<TileEntity> tester) {
        showTest = tester;
    }

    void clear() {
        buttons.clear();
    }

    void draw(Minecraft mc, int i, int j) {
        focused_id = -1;
        for (Gui gui : buttons) {
            if (gui instanceof GuiButton) {
                GuiButton button = (GuiButton) gui;
                button.drawButton(mc, i, j);
                if (button.mousePressed(mc, i, j)) {
                    focused_id = button.id;
                }
            }
            if (gui instanceof GuiLabel) {
                GuiLabel label = (GuiLabel) gui;
                label.drawLabel(mc, i, j);
            }
        }
    }

    void handleClick(IClickable parentgui, Minecraft mc, int x, int y, int button) {
        if (button != 0 && button != 1) {
            return;
        }
        for (Gui _gui : buttons) {
            if (!(_gui instanceof GuiButton)) {
                continue;
            }
            GuiButton guibutton = (GuiButton) _gui;
            if (guibutton.mousePressed(mc, x, y)) {
                if (button == 1) {
                    Sound.leftClick.play();
                } else {
                    Sound.rightClick.play();
                }
                parentgui.actionPerformedMouse(guibutton, button == 1);
            }
        }
    }
}
