package factorization.common;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;

public class FactorizationKeyHandler {
    public static final KeyBinding bag_swap_key = new KeyBinding("BoH Shift (FZ)", org.lwjgl.input.Keyboard.KEY_GRAVE, "key.categories.item");
    public static final KeyBinding pocket_key = new KeyBinding("PcktCrft Open (FZ)", org.lwjgl.input.Keyboard.KEY_C, "key.categories.item");
    
    static {
        ClientRegistry.registerKeyBinding(bag_swap_key);
        ClientRegistry.registerKeyBinding(pocket_key);
    }
    
    {
        FMLCommonHandler.instance().bus().register(this);
    }
    
    @SubscribeEvent
    public void checkKeys(ClientTickEvent event) {
        if (event.phase != Phase.START) return;
        cmdKey(Command.bagShuffle, bag_swap_key);
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
