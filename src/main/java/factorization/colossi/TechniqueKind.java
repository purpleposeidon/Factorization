package factorization.colossi;

/**
 * Classifies how a Technique should be used
 */
public enum TechniqueKind {
    /** Used to attack players. Technique.usabe() is called to see if it's okay to use. */
    OFFENSIVE,
    /** Only called after the colossus has been hurt. */
    DEFENSIVE,
    /** Used to kill time. */
    IDLER,
    /** An internal state. The technique-picker will never use one of these. */
    TRANSITION;
}
