package factorization.api.wind;

import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public interface IWindModel {
    enum WindmillState {
        /**
         * A windmill has been created
         */
        CREATE,
        /**
         * The chunk containing this windmill has been loaded.
         * The wind model is not required to remember windmill positions across world loads.
         */
        REMIND,
        /**
         * The windmill has been removed
         */
        REMOVE
    }

    /**
     * Notify the model about the status of a windmill
     * @param radius the size of the windmill in blocks. If the radius of an active windmill changes, it should REMOVE
     *               & re-CREATE itself.
     * @param state The state of the windmill.
     */
    void registerWindmill(World w, int x, int y, int z, int radius, WindmillState state);

    /**
     * @return the wind power at the location. The 'default' value is Vec3(-1, 0, 0), since vanilla clouds blow westward.
     * (The units are... not defined; probably whatever IC2 uses? This might conflict with the default value tho.)
     * If the position is the exact location of a windmill, then it should be assumed that it is that windmill
     * measuring the windpower, and so should not count as obstructing itself. It is not guaranteed that windmill
     * wind obstruction is actually simulated.
     */
    Vec3 getWindPower(World w, int x, int y, int z);
}
