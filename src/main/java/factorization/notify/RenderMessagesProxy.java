package factorization.notify;

import net.minecraft.item.ItemStack;
import net.minecraft.util.IChatComponent;

public class RenderMessagesProxy {

    public void addMessage(Object locus, ItemStack item, String format, String... args) { }
    public void onscreen(String message, String[] formatArgs) { }
    public void replaceable(IChatComponent msg, int msgKey) { }

}
