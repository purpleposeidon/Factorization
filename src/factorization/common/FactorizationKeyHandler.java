package factorization.common;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;

public class FactorizationKeyHandler {
    public static final FactorizationKeyHandler instance = new FactorizationKeyHandler();
    public static final KeyBinding bag_swap_key = new KeyBinding("FZ Bag of Holding", org.lwjgl.input.Keyboard.KEY_GRAVE, "Item");
    public static final KeyBinding pocket_key = new KeyBinding("FZ Pocket Crafting Table", org.lwjgl.input.Keyboard.KEY_C, "Item");
    
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
