package factorization.coremod;

import net.minecraft.client.Minecraft;
import factorization.common.Command;
import factorization.common.FactorizationKeyHandler;
import factorization.common.FzConfig;

public class HookTargetsClient {
    public static void keyTyped(char chr, int keysym) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;
        if (FzConfig.pocket_craft_anywhere) {
            if (FactorizationKeyHandler.pocket_key.getKeyCode() == keysym) {
                Command.craftOpen.call(mc.thePlayer);
            }
        }
    }
}
