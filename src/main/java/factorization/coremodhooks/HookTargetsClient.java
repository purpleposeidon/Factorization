package factorization.coremodhooks;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;
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
    
    public static boolean attackButtonPressed() {
        return MinecraftForge.EVENT_BUS.post(new HandleAttackKeyEvent());
    }
    
    public static boolean useButtonPressed() {
        return MinecraftForge.EVENT_BUS.post(new HandleUseKeyEvent());
    }
}
