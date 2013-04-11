package factorization.fzds;

import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.NetHandler;
import net.minecraft.network.packet.Packet1Login;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class HammerProxy {
    public void clientLogin(NetHandler clientHandler, INetworkManager manager, Packet1Login login) { }
    
    public void clientLogout(INetworkManager manager) { }
    
    public World getClientRealWorld() { return null; }
    
    public void setShadowWorld() {
        throw new RuntimeException("Tried to setShadowWorld on server");
    }

    public void restoreRealWorld() {
        throw new RuntimeException("Tried to restoreRealWorld on server");
    }

    public boolean isInShadowWorld() { return false; }
    
    public void clientInit() { }

    //Why is there no event for this? Is there an event for this?
    public void checkForWorldChange() { }
    
    public void runShadowTick() { }
    
    void registerStuff() { }
    
    void updateRayPosition(DseRayTarget ray) { }
    
    MovingObjectPosition getShadowHit() { return null; }
    
    void mineBlock(MovingObjectPosition mop) { }
}
