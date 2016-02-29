package factorization.flat;

import factorization.api.Coord;

public interface IFlatRenderInfo {
    void markDirty(Coord at);

    IFlatRenderInfo NULL = new IFlatRenderInfo() {
        @Override public void markDirty(Coord at) { }
        @Override public void discard() { }
    };

    void discard();
}
