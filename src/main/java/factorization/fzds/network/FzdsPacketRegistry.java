package factorization.fzds.network;

import net.minecraft.network.Packet;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class FzdsPacketRegistry {
    static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel("FZDS");

    
    static boolean initialized = false;
    public static void init() {
        if (initialized) return;
        initialized = true;
    }
}
