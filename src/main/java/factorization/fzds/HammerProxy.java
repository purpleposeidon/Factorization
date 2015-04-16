package factorization.fzds;

import cpw.mods.fml.common.network.handshake.NetworkDispatcher;
import factorization.fzds.interfaces.IDeltaChunk;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class HammerProxy {
    // Clients should use DeltaChunk
    public World getClientRealWorld() { return null; }
    
    public void setShadowWorld() {
        throw new RuntimeException("Tried to setShadowWorld on server");
    }

    public void restoreRealWorld() {
        throw new RuntimeException("Tried to restoreRealWorld on server");
    }

    public boolean isInShadowWorld() { return false; }
    
    public void clientInit() { }
    
    void registerStuff() { }
    
    void updateRayPosition(DseRayTarget ray) { }
    
    public MovingObjectPosition getShadowHit() { return null; }
    
    IDeltaChunk getHitIDC() { return null; }

    public void createClientShadowWorld() { }

    public void cleanupClientWorld() { }

    public boolean guiCheckStart() {
        return false;
    }

    public void guiCheckEnd(boolean oldState) {

    }
}
