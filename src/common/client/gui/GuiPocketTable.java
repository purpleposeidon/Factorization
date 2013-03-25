package factorization.client.gui;

import net.minecraft.client.gui.inventory.GuiContainer;

import org.lwjgl.opengl.GL11;

import factorization.common.Command;
import factorization.common.ContainerPocket;
import factorization.common.Core;

public class GuiPocketTable extends GuiContainer {
    public ContainerPocket containerPocket;

    public GuiPocketTable(ContainerPocket container) {
        super(container);
        containerPocket = container;
        xSize = 236;
        ySize = 89;
    }

    private int open_time = 0;

    @Override
    protected void drawGuiContainerBackgroundLayer(float f, int i, int j) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.renderEngine.bindTexture(Core.gui_dir + "pocketgui.png");
        int l = (width - xSize) / 2;
        int i1 = (height - ySize) / 2;
        drawTexturedModalRect(l, i1, 0, 0, xSize, ySize);
        open_time++;
    }
    
    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
        // I'd like it to say "Pocket Crafting", but that doesn't fit.
        // Could also make the tab a bit longer...
        // this.fontRenderer.drawString("Crafting", 178, 10, 4210752);
        this.fontRenderer.drawString("PcktCrftng", 178, 10, 4210752);
        this.fontRenderer.drawString("Keys: " + Core.pocketActions, 178, 51, 4210752);
        // this.fontRenderer.drawString("123456789", 178, 10, 4210752);
        // we can fit only that much
        // also maybe draw a nice reference for keys to press for below
    }

    @Override
    protected void keyTyped(char key, int par2) {
        if (open_time < 4) {
            super.keyTyped(key, par2);
            return;
        }
        // 'x' clears items out of the way. Fill inv, then bag (and make slurp
        // sound). [XXX TODO -- Doing this server-friendly'd require a packet or
        // something]
        // 'z' in crafting area, balance items out. Then fill in with rest of
        // inventory.
        // 'c' cycle layout, based on outer edge:
        // - Full: rotate
        // - In a '\' corner: spread left/right
        // - In a '/' corner: spread up/down
        // - A line along a side: spread to the other side, skipping the middle.
        // - Two touching: fill the circumerfence, alternating.
        // - middle of a side: spread across center
        if (key == Core.pocketActions.charAt(0) /* x */) {
            Command.craftClear.call(mc.thePlayer);
            return;
        }
        if (key == Core.pocketActions.charAt(1) /* c */) {
            Command.craftMove.call(mc.thePlayer);
            return;
        }
        if (key == Core.pocketActions.charAt(2) /* z */) {
            Command.craftBalance.call(mc.thePlayer);
            return;
        }
        super.keyTyped(key, par2);
    }
}
