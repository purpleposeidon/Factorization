package factorization.fzds.api;

public enum Caps {
    //TODO: Implement SCALE, TRANSPARENT, and INTERACT
    COLLIDE, MOVE, ROTATE, DRAG, TAKE_INTERIOR_ENTITIES, REMOVE_EXTERIOR_ENTITIES, TRANSFER_PLAYERS, ORACLE, EMPTY, SCALE, TRANSPARENT, INTERACT; //Do not re-order this list, only append.
    public int bit;
    
    Caps() {
        this.bit = 1 << ordinal();
    }
    
    public boolean in(int field) {
        return (field & this.bit) != 0;
    }
    
    public static int of(Caps ...args) {
        int ret = 0;
        for (int i = 0; i < args.length; i++) {
            ret |= args[i].bit;
        }
        return ret;
    }
}