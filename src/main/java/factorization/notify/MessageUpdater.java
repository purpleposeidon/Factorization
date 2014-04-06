package factorization.notify;

import net.minecraft.entity.player.EntityPlayer;

public interface MessageUpdater {
    /**
     * @return false to indicate that no more updates should be sent.
     */
    boolean update(boolean firstMessage);
}
