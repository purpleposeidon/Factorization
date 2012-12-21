package factorization.client.gui;

import java.util.ArrayList;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;

import org.lwjgl.opengl.GL11;

import factorization.common.ContainerFactorization;
import factorization.common.Core;
import factorization.common.Sound;
import factorization.common.TileEntityMaker;

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

    @Override
    protected void drawGuiContainerBackgroundLayer(float f, int i, int j) {
        int k = mc.renderEngine
                .getTexture(Core.texture_dir + "makergui.png");
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.renderEngine.bindTexture(k);
        int l = (width - xSize) / 2;
        int i1 = (height - ySize) / 2;
        drawTexturedModalRect(l, i1, 0, 0, xSize, ySize);

        // draw buttons
        updateGui();
        for (GuiButton button : buttons) {
            button.drawButton(mc, i, j);
        }
    }

    protected void drawGuiContainerForegroundLayer() {
        fontRenderer.drawString("Target slot", 84, 6, 0x404040);
        fontRenderer.drawString("Maker", 6, 6, 0x404040);
        fontRenderer.drawString("X", 41, 23, 0x0);
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
