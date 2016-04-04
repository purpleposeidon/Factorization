package factorization.sockets;

import factorization.api.energy.IWorkerContext;
import factorization.net.StandardMessageType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;

import java.util.List;


public interface ISocketHolder {
    void sendMessage(StandardMessageType msgType, Object ...msg);
    /**
     * @return true if the buffer is not empty
     */
    boolean dumpBuffer(List<ItemStack> buffer);
    Vec3 getServoPos();
    IWorkerContext getContext();
}
