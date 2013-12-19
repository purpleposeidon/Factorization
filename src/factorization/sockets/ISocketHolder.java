package factorization.sockets;

import java.util.List;

import net.minecraft.item.ItemStack;


public interface ISocketHolder {
    public void sendMessage(int msgType, Object ...msg);
    public boolean extractCharge(int amount);
    /**
     * @return true if the buffer is not empty
     */
    public boolean dumpBuffer(List<ItemStack> buffer);
}
