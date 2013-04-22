package factorization.fzds.api;

public enum DeltaCapability {
    //TODO: Implement SCALE, TRANSPARENT, INTERACT, REMOVE_ALL_ENTITIES, BLOCK_MINE
    COLLIDE, MOVE, ROTATE, DRAG,
    TAKE_INTERIOR_ENTITIES, REMOVE_EXTERIOR_ENTITIES, TRANSFER_PLAYERS,
    ORACLE, EMPTY, SCALE, TRANSPARENT, INTERACT, BLOCK_PLACE, BLOCK_MINE, REMOVE_ITEM_ENTITIES, REMOVE_ALL_ENTITIES;
    //Do not re-order this list, only append.
    public final long bit;
    
    DeltaCapability() {
        this.bit = 1 << ordinal();
        if (bit == 0) {
            throw new IllegalArgumentException("Too many Caps");
        }
    }
    
    public boolean in(long field) {
        return (field & this.bit) != 0;
    }
    
    public static long of(DeltaCapability ...args) {
        long ret = 0;
        for (int i = 0; i < args.length; i++) {
            ret |= args[i].bit;
        }
        return ret;
    }
}