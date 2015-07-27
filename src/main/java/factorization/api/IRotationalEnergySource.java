package factorization.api;

import net.minecraftforge.common.util.ForgeDirection;

/**
 * Modeled after IC2ex's {@link ic2.api.energy.tile.IKineticSource}.
 *
 */
public interface IRotationalEnergySource {
    /**
     * @param direction the direction that this can emit energy from. For example, if this is a vertical windmill,
     *                  then the sails would be above this TileEntity, and the power would be accessed using
     *                  <code>availableEnergy(DOWN)</code>.
     * @return true if a connection in that direction can be made.
     */
    boolean canConnect(ForgeDirection direction);

    /**
     * @param direction {@see IRotationalEnergySource#canConnect}
     * @return how much power is still available for use this tick. The units are those of torque * angular speed
     * This value should always be positive, even if the velocity is negative.
     */
    double availableEnergy(ForgeDirection direction);

    /**
     * Takes the power that is available for this tick.
     * @param direction {@see IRotationalEnergySource#canConnect}
     * @param maxPower The maximum amount of power to deplete. This value must be positive.
     * @return The amount of power that was actually used, limited by actual availability.
     */
    double takeEnergy(ForgeDirection direction, double maxPower);

    /**
     * @param direction {@see IRotationalEnergySource#canConnect}
     * @return The angular velocity, in radians per tick. May be negative. If a generator is causing a shaft to turn
     * clockwise (looking down the shaft from the position of the generator), then its angular velocity is positive.
     *
     * This value should be kept synchronized with the client, but need not be exact.
     */
    double getVelocity(ForgeDirection direction);

    /**
     * Speeds higher than this are going to render badly due to FPS limits. If the speed would go above this,
     * consider increasing availableEnergy() without increasing velocity.
     */
    double MAX_SPEED = Math.PI / 8;

    /**
     * <code>return ((TileEntity) this).isInvalid();</code>
     * the deobfuscator is simply TOO LAME to rename this method for us.
     */
    boolean isTileEntityInvalid();
}
