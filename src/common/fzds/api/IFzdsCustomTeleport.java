package factorization.fzds.api;

import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public interface IFzdsCustomTeleport {
    void transferEntity(World newWorld, Vec3 location);
}
