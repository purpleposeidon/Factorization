package factorization.constellation;

import factorization.api.Coord;

public interface IStar {
    /**
     * @return a {@link factorization.api.Coord} giving the position of what's being rendered
     */
    Coord getStarPos();

    /**
     * Callback for rendering
     * @param bulkRender The source for getting tessellators
     */
    void draw(BulkRender bulkRender);

    /**
     * Setter for the {@linkplain factorization.constellation.IStarRegion}
     * @param region The {@linkplain factorization.constellation.IStarRegion}
     */
    void setRegion(IStarRegion region);

    /**
     * Getter for the IStarRegion
     * @return The {@linkplain factorization.constellation.IStarRegion}
     */
    IStarRegion getRegion();
}
