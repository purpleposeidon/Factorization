package factorization.fzds;

import factorization.fzds.api.IDeltaChunk;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class HammerProxy {
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
    
    MovingObjectPosition getShadowHit() { return null; }
    
    IDeltaChunk getHitIDC() { return null; }

    public void createClientShadowWorld() { }
}
