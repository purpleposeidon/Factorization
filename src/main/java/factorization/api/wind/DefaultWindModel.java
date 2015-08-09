package factorization.api.wind;

import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class DefaultWindModel implements IWindModel {
    @Override
    public void registerWindmill(World w, int x, int y, int z, int radius, WindmillState state) {
    }

    @Override
    public Vec3 getWindPower(World w, int x, int y, int z) {
        return Vec3.createVectorHelper(-1, 0, 0);
    }
}
