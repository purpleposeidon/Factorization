package factorization.fzds.api;


public interface IFzdsEntryControl {
    boolean canEnter(IDeltaChunk dse);
    boolean canExit(IDeltaChunk dse);
    void onEnter(IDeltaChunk dse);
    void onExit(IDeltaChunk dse);
}
