package factorization.coremod;

import net.minecraft.client.Minecraft;
import factorization.common.FactorizationKeyHandler;
import factorization.common.FzConfig;
import factorization.shared.Core;

public class HookTargetsClient {
    public static void keyTyped(char chr, int keysym) {
        //Core.logInfo("KeyTyped: %s %s", chr, keysym); //NORELEASE
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;
        if (FzConfig.pocket_craft_anywhere) {
            if (FactorizationKeyHandler.pocket_key.getKeyCode() == keysym) {
                Core.registry.pocket_table.tryOpen(mc.thePlayer);
            }
        }
    }
}
