package factorization.common.astro;

import net.minecraft.server.MinecraftServer;
import net.minecraft.src.INetworkManager;
import net.minecraft.src.NetHandler;
import net.minecraft.src.NetLoginHandler;
import net.minecraft.src.Packet1Login;
import net.minecraft.src.World;
import net.minecraftforge.common.DimensionManager;
import cpw.mods.fml.common.network.IConnectionHandler;
import cpw.mods.fml.common.network.Player;
import factorization.common.Core;

public class HammerManager implements IConnectionHandler {
    public static int dimensionID;
    public static World hammerWorldClient = null;
    public static void setup() {
        if (!Core.enable_dimension_slice) {
            return;
        }
        dimensionID = Core.dimension_slice_dimid;
        DimensionManager.registerProviderType(dimensionID, HammerWorldProvider.class, false);
        DimensionManager.registerDimension(dimensionID, dimensionID);
    }
    
    @Override
    public void connectionClosed(INetworkManager manager) {
        hammerWorldClient = null; //XXX TODO: Stuff needs to happen here!
    }

    /**
     * {@link factorization.client.FactorizationClientProxy.hammerClientLogin}
     */
    @Override
    public void clientLoggedIn(NetHandler clientHandler, INetworkManager manager, Packet1Login login) {
        Core.proxy.hammerClientLogin(clientHandler, manager, login);
    }
    

    @Override
    public void playerLoggedIn(Player player, NetHandler netHandler, INetworkManager manager) {}

    @Override
    public String connectionReceived(NetLoginHandler netHandler, INetworkManager manager) { return null; }

    @Override
    public void connectionOpened(NetHandler netClientHandler, String server, int port, INetworkManager manager) { }

    @Override
    public void connectionOpened(NetHandler netClientHandler, MinecraftServer server, INetworkManager manager) { }
}
