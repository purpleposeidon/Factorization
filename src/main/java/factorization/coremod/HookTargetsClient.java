package factorization.coremod;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import factorization.common.Command;
import factorization.common.FactorizationKeyHandler;
import factorization.common.FzConfig;
import factorization.docs.DocumentationModule;

public class HookTargetsClient {
    public static void keyTyped(char chr, int keysym) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;
        EntityPlayer player = mc.thePlayer;
        if (FzConfig.pocket_craft_anywhere) {
            if (FactorizationKeyHandler.pocket_key.getKeyCode() == keysym) {
                Command.craftOpen.call(player);
            }
        }
        if (chr == '?') {
            DocumentationModule.openPageForHilightedItem();
        }
    }
}
