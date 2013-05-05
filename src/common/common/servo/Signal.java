package factorization.common.servo;

/**
 * The track can have 
 */
public enum Signal {
    //We can't have > 16 due to only having 16 colors.
    STOP_MOTOR, SLOW_MOTOR, IGNORE_INSTRUCTIONS, ACTIVATE_ACTUATOR, SWAP_ACTUATOR, REDSTONE /* maybe */;
}
