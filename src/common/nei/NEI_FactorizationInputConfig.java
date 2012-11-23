package factorization.nei;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.src.GuiContainer;
import codechicken.nei.api.IConfigureNEI;
import codechicken.nei.forge.GuiContainerManager;
import codechicken.nei.forge.IContainerInputHandler;
import factorization.client.FactorizationClientProxy;
import factorization.common.Command;
import factorization.common.Core;
import factorization.common.FactorizationProxy;
import factorization.common.ItemPocketTable;
import factorization.common.Registry;

public class NEI_FactorizationInputConfig implements IConfigureNEI {
    public void loadConfig() {
        GuiContainerManager.addInputHandler(new IContainerInputHandler() {
            @Override
            public void onMouseUp(GuiContainer gui, int mousex, int mousey, int button) { }

            public void onMouseScrolled(GuiContainer gui, int mousex, int mousey, int scrolled) { }

            public void onMouseClicked(GuiContainer gui, int mousex, int mousey, int button) { }

            public void onKeyTyped(GuiContainer gui, char keyChar, int keyID) { }

            public boolean mouseScrolled(GuiContainer gui, int mousex, int mousey, int scrolled) { return false; }

            public boolean mouseClicked(GuiContainer gui, int mousex, int mousey, int button) { return false; }

            public boolean lastKeyTyped(GuiContainer gui, char keyChar, int keyID) {
                if (FactorizationClientProxy.bag_swap_key.keyCode == keyID) {
                    Command cmd = Command.bagShuffle;
                    if (Keyboard.isKeyDown(Minecraft.getMinecraft().gameSettings.keyBindSneak.keyCode)) {
                        cmd = cmd.reverse;
                    }
                    cmd.call(Core.proxy.getClientPlayer());
                    return true;
                }
                if (FactorizationClientProxy.pocket_key.keyCode == keyID) {
                    if (Core.registry.pocket_table.findPocket(Core.proxy.getClientPlayer()) != null) {
                        Command.craftOpen.call(Core.proxy.getClientPlayer());
                    }
                    return true;
                }
                return false;
            }

            public boolean keyTyped(GuiContainer gui, char keyChar, int keyCode) { return false; }
        });
    }

    @Override
    public String getName() {
        return "factorizationKeyHandler";
    }

    @Override
    public String getVersion() {
        return "1";
    }
}
