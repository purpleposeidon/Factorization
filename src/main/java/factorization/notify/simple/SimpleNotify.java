package factorization.notify.simple;

import java.lang.reflect.Method;

import net.minecraft.entity.player.EntityPlayer;
import cpw.mods.fml.common.Loader;

public class SimpleNotify {
    /**
     * Sends a message to the player to be drawn at a location in the world, rather than cluttering up the chat window.
     * Returns true if the notification system is installed; use this to fallback to chat messages.
     * 
     * 
     * 
     * <pre>
     * if (!SimpleNotify.send(player, xCoord, yCoord, zCoord, "colorblocks.notify.alreadycolored", heldColor)) {
     *     player.addChatMessage(new ChatComponentTranslation("colorblocks.chat.alreadycolored", heldColor));
     * }
     * </pre>
     */
    public static boolean send(EntityPlayer player, int blockX, int blockY, int blockZ, String message, String... formatParameters) {
        if (toCall == null) return false;
        try {
            toCall.invoke(null, player, blockX, blockY, blockZ, message, formatParameters);
        } catch (ReflectiveOperationException e) {
            return false;
        }
        return true;
    }
    
    static Method toCall = null;
    
    static {
        if (Loader.isModLoaded("factorization.notify")) {
            try {
                Class<?> notify = SimpleNotify.class.getClassLoader().loadClass("factorization.notify.Notify");
                toCall = notify.getMethod("send", EntityPlayer.class, Integer.class, Integer.class, Integer.class, String.class, String[].class);
            } catch (ReflectiveOperationException e) {
                e.printStackTrace();
            }
        }
    }
}
