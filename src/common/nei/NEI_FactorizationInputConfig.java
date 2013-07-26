package factorization.nei;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import codechicken.nei.api.IConfigureNEI;
import codechicken.nei.forge.GuiContainerManager;
import codechicken.nei.forge.IContainerInputHandler;
import factorization.client.FactorizationClientProxy;
import factorization.common.Command;
import factorization.common.Core;
import factorization.common.ItemPocketTable;

public class NEI_FactorizationInputConfig implements IConfigureNEI {
    @Override
    public void loadConfig() {
        ItemPocketTable.NEI_status = 1;
        GuiContainerManager.addInputHandler(new IContainerInputHandler() {
            @Override
            public void onMouseUp(GuiContainer gui, int mousex, int mousey, int button) { }

            @Override
            public void onMouseScrolled(GuiContainer gui, int mousex, int mousey, int scrolled) { }

            @Override
            public void onMouseClicked(GuiContainer gui, int mousex, int mousey, int button) { }

            @Override
            public void onKeyTyped(GuiContainer gui, char keyChar, int keyID) { }

            @Override
            public boolean mouseScrolled(GuiContainer gui, int mousex, int mousey, int scrolled) { return false; }

            @Override
            public boolean mouseClicked(GuiContainer gui, int mousex, int mousey, int button) { return false; }

            @Override
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

            @Override
            public boolean keyTyped(GuiContainer gui, char keyChar, int keyCode) { return false; }

            @Override
            public void onMouseDragged(GuiContainer gui, int mousex, int mousey, int button, long heldTime) { }
        });
    }

    @Override
    public String getName() {
        return "FZ Key Handler";
    }

    @Override
    public String getVersion() {
        return "v1";
    }
}
