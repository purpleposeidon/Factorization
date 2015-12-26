package factorization.servo;

import factorization.net.StandardMessageType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import org.lwjgl.input.Keyboard;

public class GuiCommentEditor extends GuiScreen {
    GuiTextField rem;
    TileEntityServoRail rail;

    public GuiCommentEditor(TileEntityServoRail rail) {
        this.rail = rail;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();
        buttonList.add(new GuiButton(0, width / 2 - 100, height / 4 + 96 + 12, I18n.format("gui.done")));
        rem = new GuiTextField(1337, this.fontRendererObj, this.width / 2 - 150, 60, 300, 20);
        rem.setMaxStringLength(200);
        rem.setFocused(true);
        rem.setText(rail.comment);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        rail.comment = rem.getText();
        rail.broadcastMessage(Minecraft.getMinecraft().thePlayer, StandardMessageType.ServoRailEditComment, rem.getText());
        mc.displayGuiScreen(null);
    }

    @Override
    protected void keyTyped(char chr, int keySym) {
        rem.textboxKeyTyped(chr, keySym);

        if (keySym != 28 && keySym != 156) {
            if (keySym == 1) {
                mc.displayGuiScreen(null);
            }
        } else {
            this.actionPerformed(null);
        }
    }

    public void drawScreen(int mouseX, int mouseY, float partial) {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRendererObj, "Rail Comment", this.width / 2, 20, 16777215);
        rem.drawTextBox();
        super.drawScreen(mouseX, mouseY, partial);
    }
}
