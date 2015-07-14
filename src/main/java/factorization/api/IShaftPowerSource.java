package factorization.api;

import net.minecraftforge.common.util.ForgeDirection;

/**
 * Modeled after IC2ex's {@link ic2.api.energy.tile.IKineticSource}.
 */
public interface IShaftPowerSource {
    /**
     * @param direction the direction that this emits power from. For example, if this is a vertical windmill, then the
     *                  sails would be above this TileEntity, and the power would be accessed using
     *                  <code>availablePower(DOWN)</code>.
     * @return true if a connection in that direction can be made.
     */
    boolean canConnect(ForgeDirection direction);

    /**
     * @param direction {@see IShaftPowerSource#canConnect}
     * @return how much power is still available for use this tick. The units are those of torque * angular speed
     * This value should always be positive, even if the velocity is negative.
     */
    double availablePower(ForgeDirection direction);

    /**
     * Takes the power that is available for this tick.
     * @param direction {@see IShaftPowerSource#canConnect}
     * @param maxPower The maximum amount of power to deplete. This value must be positive.
     * @return The amount of power that was actually used, limited by actual availability. This value must be positive.
     */
    double powerConsumed(ForgeDirection direction, double maxPower);

    /**
     * @param direction {@see IShaftPowerSource#canConnect}
     * @return The angular velocity, in radians per tick. May be negative. If a generator is causing a shaft to turn
     * clockwise (looking down the shaft from the position of the generator), then its angular velocity is positive.
     */
    double getAngularVelocity(ForgeDirection direction);

    /**
     * Speeds higher than this are going to render badly due to FPS limits. Consider boosting {@link IShaftPowerSource#availablePower(ForgeDirection)}
     * instead.
     */
    double SUGGESTED_MAX_VELOCITY = Math.PI / 4;

    class Display {
        private Display() { }

        /**
         * Limit the rotational speed client-side to avoid odd effects at speeds greater than the frame rate.
         * The results of invoking this function should be kept as a private matter between the shaft and its
         * renderer; it should not be returned from {@link IShaftPowerSource#}getAngularVelocity}.
         *
         * If a shaft that respects this method is attatched to a shaft that does not, ugliness can happen.
         * This can be temporarily worked around by setting MAX_DISPLAY_VELOCITY to infinity.
         *
         * It's probably best to actually limit
         *
         * @param velocity The actual velocity
         * @return A rotational velocity to be used for render purposes only.
         */
        public static double limitVelocity(double velocity) {
            if (velocity > MAX_DISPLAY_VELOCITY) return MAX_DISPLAY_VELOCITY;
            if (velocity < -MAX_DISPLAY_VELOCITY) return -MAX_DISPLAY_VELOCITY;
            return velocity;
        }

        private static double MAX_DISPLAY_VELOCITY = SUGGESTED_MAX_VELOCITY;

        /**
         * {@see Display#limitVelocity}
         * @param maxDisplayVelocity The maximum velocity for display purposes
         */
        public static void setMaxDisplayVelocity(double maxDisplayVelocity) {
            MAX_DISPLAY_VELOCITY = Math.abs(maxDisplayVelocity);
        }
    }
}
