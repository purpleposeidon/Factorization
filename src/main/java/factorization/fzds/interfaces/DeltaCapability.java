package factorization.fzds.interfaces;

public enum DeltaCapability {
    //TODO: Implement SCALE, TRANSPARENT, REMOVE_ALL_ENTITIES
    /** The player can collide with the blocks contained in the DSE. */
    COLLIDE,
    /** The DSE can translate linearly */
    MOVE,
    /** The DSE can rotate */
    ROTATE,
    /** When the DSE moves, entities (that are outside of hammer space) will move with it */
    DRAG,
    /** Entities within the bounds of the DSE will be sucked into hammerspace */
    TAKE_INTERIOR_ENTITIES,
    /** Entities outside the bounds of the DSE will be moved to the real world. */
    REMOVE_EXTERIOR_ENTITIES,
    /** TAKE_INTERIOR_ENTITIES and REMOVE_EXTERIOR_ENTITIES will not operate on players unless this is true. */
    TRANSFER_PLAYERS,
    /** The DSE reflects the real world instead of hammerspace. */
    ORACLE,
    /** The DSE will self-destruct if it is empty. (TODO: Is this implemented?) (TODO: This is a bad/confusing name.) */
    EMPTY,
    /** The DSE can be rescaled. (Not implemented.) */
    SCALE,
    /** The DSE will render with a custom opacity. (Not implemented? Should be easy.) */
    TRANSPARENT,
    /** The player can punch and click on blocks & entities. */
    INTERACT,
    /** The player can place blocks. (TODO: Verify respect) */
    BLOCK_PLACE,
    /** The player can mine blocks. (TODO: Verify respect) */
    BLOCK_MINE,
    /** All item entities will be ejected from hammer space. */
    REMOVE_ITEM_ENTITIES,
    /** All entities will be ejected from hammer space. */
    REMOVE_ALL_ENTITIES,
    /** Entities will have their velocities adjusted when they collide with a moving DSE. (TODO) */
    ENTITY_PHYSICS,
    /** Living entities can be hurt by physics (TODO) */
    PHYSICS_DAMAGE,
    /** When a DSE hits an entity, an opposing force will be applied to the DSE. (TODO) */
    CONSERVE_MOMENTUM,
    /** When a DSE hits an entity, hurt it & apply knockback */
    VIOLENT_COLLISIONS,
    /** The DSE stops moving if it hits a block */
    COLLIDE_WITH_WORLD
    ;
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