package factorization.constellation;

public interface IStarRegion {
    /**
     * Call when the object is invalidated/destroyed/unloaded.
     * @param star The {@link factorization.constellation.IStar} that has been registered with this region.
     */
    void removeStar(IStar star);

    /**
     * Call when a redraw is needed
     * @param star The {@link factorization.constellation.IStar} that has been registered with this region.
     */
    void dirtyStar(IStar star);
}
