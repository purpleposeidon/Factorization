package factorization.fzds.network;

import net.minecraft.network.Packet;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public class FzdsPacketRegistry {
    static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel("FZDS");

    
    static boolean initialized = false;
    public static void init() {
        if (initialized) return;
        initialized = true;
    }
}
