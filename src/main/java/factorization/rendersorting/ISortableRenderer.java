package factorization.rendersorting;

/**
 * An interface to be implemented by Entities and TileEntities to assist in sorting.
 * Note that it is the Entity/TileEntity itself that must implement this interface, not the renderer.
 *
 * The client's list of Entities and TileEntities will be sorted by class, improving instruction cache usage and
 * (slightly) reducing the number of GL state changes.
 *
 * This is only useful for renderers with a particular level of complexity. For example, enchanting tables use only a
 * single texture, and so do not benefit from this. On the other hand, players are extremely complex, having custom
 * skin and potentially *many* texture rebinds.
 *
 * An renderer that draws a single item is the cannonical use case: the texture that'll be bound is not determined by
 * the class, but instead by the item's renderer.
 *
 * @param <E> The Entity or TileEntity's type.
 */
public interface ISortableRenderer<E> {
    /**
     * @param other The other Entity or TileEntity being compared
     * @return a value that sorts to merge GL state changes.
     */
    int compareRenderer(E other);
}
