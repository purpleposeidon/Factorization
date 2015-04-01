package factorization.fzds.interfaces;

import net.minecraft.util.Vec3;
import net.minecraft.world.World;

/**
 * Entities can use this to implement precisely how they move in and out of IDCs.
 * @see factorization.fzds.interfaces.IFzdsEntryControl
 */
public interface IFzdsCustomTeleport {
    void transferEntity(IDeltaChunk idc, World newWorld, Vec3 newPosition);
}
