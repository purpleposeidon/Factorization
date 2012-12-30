package factorization.fzds.api;

import factorization.fzds.DimensionSliceEntity;

public interface IFzdsEntryControl {
    boolean canEnter(DimensionSliceEntity dse);
    boolean canExit(DimensionSliceEntity dse);
    void onEnter(DimensionSliceEntity dse);
    void onExit(DimensionSliceEntity dse);
}
