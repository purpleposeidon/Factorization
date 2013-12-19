package factorization.wrath;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;

public class GuiLabel extends Gui {

    int x, y;
    String text;

    public GuiLabel(int x, int y, String text) {
        this.x = x;
        this.y = y;
        this.text = text;
    }

    public void drawLabel(Minecraft par1Minecraft, int par2, int par3) {
        FontRenderer fontRenderer = par1Minecraft.fontRenderer;
        fontRenderer.drawString(text, x, y, 0x404040);
    }
}
