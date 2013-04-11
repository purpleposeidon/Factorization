package factorization.fzds.api;

public enum DeltaCapability {
    //TODO: Implement SCALE, TRANSPARENT, and INTERACT, BUILD
    COLLIDE, MOVE, ROTATE, DRAG,
    TAKE_INTERIOR_ENTITIES, REMOVE_EXTERIOR_ENTITIES, TRANSFER_PLAYERS,
    ORACLE, EMPTY, SCALE, TRANSPARENT, INTERACT, BUILD;
    //Do not re-order this list, only append.
    public final int bit; //TODO NORELEASE: make this a long!
    
    DeltaCapability() {
        this.bit = 1 << ordinal();
        if (bit == 0) {
            throw new IllegalArgumentException("Too many Caps");
        }
    }
    
    public boolean in(int field) {
        return (field & this.bit) != 0;
    }
    
    public static int of(DeltaCapability ...args) {
        int ret = 0;
        for (int i = 0; i < args.length; i++) {
            ret |= args[i].bit;
        }
        return ret;
    }
}