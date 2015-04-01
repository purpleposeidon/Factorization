package factorization.fzds.interfaces;

/**
 * Allows entities to control if they get moved between shadow & reality
 * @see factorization.fzds.interfaces.DeltaCapability
 */
public interface IFzdsEntryControl {
    boolean canEnter(IDeltaChunk dse);
    boolean canExit(IDeltaChunk dse);
    void onEnter(IDeltaChunk dse);
    void onExit(IDeltaChunk dse);
}
