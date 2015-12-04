package factorization.common;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import factorization.shared.Core;

public class FactorizationKeyHandler {
    public static final KeyBinding pocket_key = new KeyBinding("PcktCrft Open (FZ)", org.lwjgl.input.Keyboard.KEY_C, "key.categories.item");
    
    static {
        ClientRegistry.registerKeyBinding(pocket_key);
    }
    
    {
        Core.loadBus(this);
    }
    
    @SubscribeEvent
    public void checkKeys(ClientTickEvent event) {
        if (event.phase != Phase.START) return;
        cmdKey(Command.craftOpen, pocket_key);
    }
    
    private void cmdKey(Command cmd, KeyBinding binding) {
        if (!binding.isPressed()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings.keyBindSneak.isPressed()) {
            cmd = cmd.reverse;
        }
        cmd.call(mc.thePlayer);
    }
}
