package factorization.servo.stepper;

public enum DropMode {
    /**
     * The DSE should be converted back to real-world block form as soon as it is released.
     */
    IMMEDIATE,

    /**
     * The DSE should be converted after, say, 30 seconds.
     */
    EVENTUALLY,

    /**
     * The DSE should never be converted.
     */
    NEVER
}
