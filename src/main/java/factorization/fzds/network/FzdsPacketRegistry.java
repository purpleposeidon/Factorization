package factorization.fzds.network;

import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;

public class FzdsPacketRegistry {
    static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel("FZDS");

    
    static boolean initialized = false;
    public static void init() {
        if (initialized) return;
        initialized = true;
    }
}
