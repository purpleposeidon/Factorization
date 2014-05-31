package factorization.fzds.network;

import net.minecraft.network.Packet;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public class FzdsPacketRegistry {
    static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel("FzDs");

    
    static boolean initialized = false;
    public static void init() {
        if (initialized) return;
        initialized = true;
        INSTANCE.registerMessage(WrappedPacket.class, WrappedPacket.class, 0, Side.CLIENT);
    }
    
    public static Packet wrap(Packet packet) {
        return INSTANCE.getPacketFrom(new WrappedPacket(packet));
    }
}
