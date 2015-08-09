package factorization.api.adapter;

import java.util.*;

/**
 * Ascribes an interface to a type that might not actually implement it.
 * @see AdapterExample
 * @param <SOURCE> The input type
 * @param <TARGET> The class that the type shall be cast to.
 */
@SuppressWarnings("unused")
public class InterfaceAdapter<SOURCE, TARGET> implements Comparator<Adapter>, Adapter<SOURCE, TARGET> {
    public InterfaceAdapter(Class<TARGET> targetInterface) {
        this.targetInterface = targetInterface;
    }

    /**
     * Helper method for registering for use in situations where the interface does not rely on 'TARGET.this', such as Block.
     * @param inClass the class used for outInterface
     * @param outInterface the value that will be returned by cast() if the input is of type inClass.
     */
    public void register(Class<SOURCE> inClass, TARGET outInterface) {
        Adapter<SOURCE, TARGET> ret = new GenericAdapter<SOURCE, TARGET>(inClass, outInterface);
        register(ret);
    }

    /**
     * Registers an adapter.
     * @param adapter The adapter to register
     * @return this
     */
    public InterfaceAdapter<SOURCE, TARGET> register(Adapter<SOURCE, TARGET> adapter) {
        adapters.add(adapter);
        adapterCache.clear();
        return this;
    }

    /**
     * @param fallbackAdapter the adapter to be used if no other adapter is suitable. canCast is *not* called.
     *                        The default behavior is to return null.
     */
    public void setFallbackAdapter(Adapter<SOURCE, TARGET> fallbackAdapter) {
        adapterCache.clear();
        this.fallbackAdapter = fallbackAdapter;
    }

    /**
     * @param nullAdapter the adapter to be used to handle null values. canCast is *not* called.
     *                    The default behavior is to return null.
     */
    public void setNullAdapter(Adapter<SOURCE, TARGET> nullAdapter) {
        adapterCache.clear();
        this.null_adapter = nullAdapter;
    }

    /**
     * @return the list of adapters, for doing whatever with. Don't call this regularly.
     */
    public List<Adapter<SOURCE, TARGET>> getAdapters() {
        adapterCache.clear();
        return adapters;
    }

    /**
     * @param obj The value to be converted. May be null.
     * @return The value, converted. Returns null if the null_adapter does not convert the input.
     */
    public TARGET cast(SOURCE obj) {
        if (obj == null) return null_adapter.adapt(null);
        Class<? extends SOURCE> objClass = (Class<? extends SOURCE>) obj.getClass();
        Adapter<SOURCE, TARGET> adapter = adapterCache.get(objClass);
        if (adapter == null) {
            adapter = findAdapter(objClass);
            adapterCache.put(objClass, adapter);
        }
        return adapter.adapt(obj);
    }

    private Adapter<SOURCE, TARGET> findAdapter(Class<? extends SOURCE> objClass) {
        if (adapterCache.isEmpty()) {
            Collections.sort(adapters, this);
        }
        if (targetInterface.isInstance(objClass)) {
            return this; // the self adapter
        }
        for (Adapter<SOURCE, TARGET> a : adapters) {
            if (a.canAdapt(objClass)) {
                return a;
            }
        }
        return fallbackAdapter;
    }

    public final Class<TARGET> targetInterface;
    private final HashMap<Class<? extends SOURCE>, Adapter<SOURCE, TARGET>> adapterCache = new HashMap<Class<? extends SOURCE>, Adapter<SOURCE, TARGET>>();
    private final ArrayList<Adapter<SOURCE, TARGET>> adapters = new ArrayList<Adapter<SOURCE, TARGET>>();
    private Adapter<SOURCE, TARGET> null_adapter = this;
    private Adapter<SOURCE, TARGET> fallbackAdapter = (Adapter<SOURCE, TARGET>) nullAdapter;

    private static final Adapter<?, ?> nullAdapter = new GenericAdapter<Object, Object>(null /* rely on canCast() not being called */, null);

    // Comparator implementation; fewer classes! :D
    @Override
    public int compare(Adapter o1, Adapter o2) {
        int p1 = o1.priority();
        int p2 = o2.priority();
        return p1 - p2;
    }

    // 'self adapter' implementation; fewer classes! :D
    @Override
    public TARGET adapt(SOURCE val) {
        return (TARGET) val;
    }

    @Override
    public boolean canAdapt(Class<?> valClass) {
        return targetInterface.isAssignableFrom(valClass);
    }

    @Override
    public int priority() {
        return 0;
    }

}
