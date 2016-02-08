package factorization.fzds.interfaces;

/**
 * Allows entities to control if they get moved between shadow & reality
 * @see factorization.fzds.interfaces.DeltaCapability
 */
public interface IFzdsEntryControl {
    boolean canEnter(IDimensionSlice dse);
    boolean canExit(IDimensionSlice dse);
    void onEnter(IDimensionSlice dse);
    void onExit(IDimensionSlice dse);
}
