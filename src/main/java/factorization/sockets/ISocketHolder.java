package factorization.sockets;

import factorization.shared.NetworkFactorization.MessageType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Vec3;

import java.util.List;


public interface ISocketHolder {
    public void sendMessage(MessageType msgType, Object ...msg);
    public boolean extractCharge(int amount);
    /**
     * @return true if the buffer is not empty
     */
    public boolean dumpBuffer(List<ItemStack> buffer);
    public Vec3 getServoPos();
}
