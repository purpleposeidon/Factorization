package factorization.weird;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import factorization.common.Command;
import factorization.common.FzConfig;
import factorization.shared.Core;

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
        Core.bindGuiTexture("pocketgui");
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
        int color = 0xa0a0a0;
        for (int i = 0; i < FzConfig.pocketActions.length(); i++) {
            char key = FzConfig.pocketActions.charAt(i);
            String msg = null;
            switch (i) {
            case 0: msg = "Empty the crafting grid"; break;
            case 1: msg = "Swirl items ↷"; break;
            case 2: msg = "Balance items"; break;
            case 3: msg = "Fill grid with item under cursor"; break;
            }
            if (msg == null) {
                continue;
            }
            int d = 10;
            int y = -d*FzConfig.pocketActions.length() + d*i;
            this.fontRenderer.drawString(key + ": " + msg, 8, y, color);
        }
        // this.fontRenderer.drawString("123456789", 178, 10, 4210752);
        // we can fit only that much
    }

    @Override
    protected void keyTyped(char key, int par2) {
        if (open_time < 4) {
            super.keyTyped(key, par2);
            return;
        }
        char my_key = ("" + key).toLowerCase().charAt(0);
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
        if (my_key == FzConfig.pocketActions.charAt(0) /* x */) {
            Command.craftClear.call(mc.thePlayer);
            return;
        }
        if (my_key == FzConfig.pocketActions.charAt(1) /* c */) {
            Command.craftSwirl.call(mc.thePlayer);
            return;
        }
        if (my_key == FzConfig.pocketActions.charAt(2) /* b */) {
            Command.craftBalance.call(mc.thePlayer);
            return;
        }
        if (my_key == FzConfig.pocketActions.charAt(3) /* f */) {
            int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
            int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
            Slot slot = getSlotAtPosition(mouseX, mouseY);
            if (slot != null) {
                Command.craftFill.call(mc.thePlayer, (byte) slot.getSlotIndex());
            }
            return;
        }
        super.keyTyped(key, par2);
    }
}
