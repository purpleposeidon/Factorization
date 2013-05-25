package factorization.common.servo;

public enum StackType {
    PRIMARY("Code for the primary function"),
    ALTERNATE("Code for a second function"),
    ARG_STACK("Stack for passing arguments"),
    ARG_STACK2("Second stack for passing arguments"),
    ON_BLOCK_ENTER("Triggered when entering a block"),
    ON_BLOCK_EXIT("Triggered when leaving a block"),
    ON_REDSTONE("Triggered when a signal is received"),
    ON_ITERATION("Called by the controller"),
    IO_BUFFER("Used for reading & writing to track"),
    ERRNO("Activated actuators put success/failure signals in here");
    
    String description;
    
    private StackType(String description) {
        this.description = description;
    }
}
