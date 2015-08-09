package factorization.api.adapter;

/**
 * This is the interface used for adapters.
 * @param <IN_TYPE> The type to be converted to InterfaceAdapter#SOURCE.
 * @param <OUT_TYPE> Must be the same type as InterfaceAdapter#TARGET.
 */
public interface Adapter<IN_TYPE, OUT_TYPE> {
    /**
     * Convert the type; presumably won't just return <code>(OUT_TYPE) val</code>, as that is handled by default.
     */
    OUT_TYPE adapt(IN_TYPE val);

    /**
     * @param valClass The class.
     * @return true if this adapter can handle valClass. The answer is cached. You might want 'valClass instanceof OUT_TYPE'
     */
    boolean canAdapt(Class<?> valClass);

    /**
     * @return The sorting priority of this adapter. 0 is a fine default.
     */
    int priority();
}
