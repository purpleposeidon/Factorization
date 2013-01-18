package factorization.fzds;

import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.NetHandler;
import net.minecraft.network.packet.Packet1Login;
import net.minecraft.world.World;

public class HammerProxy {
    public void clientLogin(NetHandler clientHandler, INetworkManager manager, Packet1Login login) { }
    
    public void clientLogout(INetworkManager manager) { }
    
    public World getClientRealWorld() { return null; }
    
    public World getOppositeWorld() { return null; }

    public void setClientWorld(World w) {
        throw new RuntimeException("Tried to setClientWorld on server");
    }

    public void restoreClientWorld() {
        throw new RuntimeException("Tried to restoreClientWorld on server");
    }

    public boolean isInShadowWorld() { return false; }
    
    public void clientInit() { }

    //Why is there no event for this? Is there an event for this?
    public void checkForWorldChange() { }
    
    public void runShadowTick() { }

    public void setPlayerIsEmbedded(DimensionSliceEntity dse) { }
}
