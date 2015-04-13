package factorization.fzds.gui;

import factorization.fzds.Hammer;
import factorization.fzds.interfaces.IFzdsShenanigans;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

public class ProxiedGuiScreen extends GuiScreen implements IFzdsShenanigans {
    final GuiScreen sub;

    public ProxiedGuiScreen(GuiScreen sub) {
        this.sub = sub;
        if (sub instanceof ProxiedGuiContainer || sub instanceof ProxiedGuiScreen) {
            throw new IllegalArgumentException("Nesting has negative socio-economic impact! Not allowed!");
        }
    }


    private boolean enter() {
        if (!Hammer.proxy.isInShadowWorld()) {
            Hammer.proxy.setShadowWorld();
            return true;
        }
        return false;
    }

    private void exit(boolean do_it) {
        if (do_it) {
            Hammer.proxy.restoreRealWorld();
        }
    }

    public void initGui() {
        boolean switched = enter();
        try {
            sub.initGui();
        } finally {
            exit(switched);
        }

        this.width = sub.width;
        this.height = sub.height;

        super.initGui();
    }

    @Override
    public void setWorldAndResolution(Minecraft mc, int width, int height) {
        super.setWorldAndResolution(mc, width, height);
        boolean switched = enter();
        try {
            sub.setWorldAndResolution(mc, width, height);
        } finally {
            exit(switched);
        }
    }

    public void drawScreen(int mouseX, int mouseY, float partial) {
        boolean switched = enter();
        try {
            sub.drawScreen(mouseX, mouseY, partial);
        } finally {
            exit(switched);
        }
    }

    @Override
    public void handleInput() {
        boolean switched = enter();
        try {
            sub.handleInput();
        } finally {
            exit(switched);
        }
    }

    public void onGuiClosed() {
        boolean switched = enter();
        try {
            sub.onGuiClosed();
        } finally {
            exit(switched);
        }
        super.onGuiClosed();
    }

    public boolean doesGuiPauseGame() {
        return sub.doesGuiPauseGame();
    }

    public void updateScreen() {
        boolean switched = enter();
        try {
            sub.updateScreen();
        } finally {
            exit(switched);
        }
    }
}
