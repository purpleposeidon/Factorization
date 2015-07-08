package factorization.api;

import net.minecraftforge.common.util.ForgeDirection;

/**
 * Modelled after IC2ex's {@link ic2.api.energy.tile.IKineticSource}.
 */
public interface IShaftPowerSource {
    /**
     *
     * @param direction the direction that this emits power from. For example, if this is a vertical windmill, then the
     *                  sails would be above this TileEntity, and the power would be accessed using
     *                  <code>availablePower(DOWN)</code>.
     * @return true if a connection in that direction is available.
     */
    boolean canConnect(ForgeDirection direction);

    /**
     * @param direction {@see IShaftPowerSource#canConnect}
     * @return how much power this can immediately provide. The units are those of torque * angular speed,
     * [ meter * Newton ] * [ radians / tick ]
     */
    double availablePower(ForgeDirection direction);

    /**
     * @param direction {@see IShaftPowerSource#canConnect}
     * @param maxPower The maximum amount of power to deplete.
     * @return The amount of power that was actually used.
     */
    double deplete(ForgeDirection direction, double maxPower);

    /**
     * @param direction {@see IShaftPowerSource#canConnect}
     * @return The angular speed, in radians per tick. The direction of rotation is not defined.
     */
    double getAngularSpeed(ForgeDirection direction);
}
