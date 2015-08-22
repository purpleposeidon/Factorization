package factorization.api.adapter;

import java.util.*;

/**
 * Ascribes an interface to a type that might not actually implement it.
 * The implementation's a bit hairy; @see AdapterExample for an easy & fuzzy example.
 * @param <SOURCE> The input type
 * @param <TARGET> The class that the type shall be cast to.
 */
@SuppressWarnings("unused")
public class InterfaceAdapter<SOURCE, TARGET> implements Comparator<Adapter>, Adapter<SOURCE, TARGET> {
    public final Class<TARGET> targetInterface;

    /**
     * @param targetInterface The class object of the interface that will be adapted.
     * @param <S> The adaption source
     * @param <T> The adaption target, and class of {@code targetInterface}.
     * @return a globally shared InterfaceAdapter associated with the interface.
     */
    public static <S, T> InterfaceAdapter<S, T> get(Class<T> targetInterface) {
        InterfaceAdapter ret = common_adapters.get(targetInterface);
        if (ret == null) {
            ret = getExtra(targetInterface);
            common_adapters.put(targetInterface, ret);
        }
        if (ret.targetInterface != targetInterface) {
            throw new ClassCastException("targetInterfaces don't match");
        }
        return (InterfaceAdapter<S, T>) ret;
    }

    /**
     * @param targetInterface The class object of the interface that will be adapted.
     * @param <S> The adaption source
     * @param <T> The adaption target, and class of {@code targetInterface}.
     * @return an anonymous adapter
     */
    public static <S, T> InterfaceAdapter<S, T> getExtra(Class<T> targetInterface) {
        return new InterfaceAdapter<S, T>(targetInterface);
    }

    /**
     * Helper method for registering for use in situations where the interface does not rely on 'TARGET.this', such as Block.
     * @param inClass the class used for outInterface
     * @param outInterface the value that will be returned by cast() if the input is of type inClass.
     */
    public <OBJ extends SOURCE> void register(Class<OBJ> inClass, TARGET outInterface) {
        Adapter<OBJ, TARGET> ret = new GenericAdapter<OBJ, TARGET>(inClass, outInterface);
        register(ret);
    }

    /**
     * Registers an adapter.
     * @param adapter The adapter to register
     * @return this
     */
    public <REGISTERED_SOURCE extends SOURCE> InterfaceAdapter<SOURCE, TARGET> register(Adapter<REGISTERED_SOURCE, TARGET> adapter) {
        adapters.add(adapter);
        adapterCache.clear();
        return this;
    }

    /**
     * @param fallbackAdapter the adapter to be used if no other adapter is suitable. canCast is *not* called.
     *                        The default behavior is to return null.
     */
    public void setFallbackAdapter(Adapter<? extends SOURCE, TARGET> fallbackAdapter) {
        adapterCache.clear();
        this.fallbackAdapter = fallbackAdapter;
    }

    public Adapter<? extends SOURCE, TARGET> getFallbackAdapter() {
        return fallbackAdapter;
    }

    /**
     * @param nullAdapter the adapter to be used to handle null values. canCast is *not* called.
     *                    The default behavior is to return null.
     */
    public void setNullAdapter(Adapter<SOURCE, TARGET> nullAdapter) {
        adapterCache.clear();
        this.null_adapter = nullAdapter;
    }

    public Adapter<? extends SOURCE, TARGET> getNullAdapter() {
        return null_adapter;
    }

    /**
     * @return the list of adapters, for doing whatever with. Don't call this regularly.
     */
    public List<Adapter<? extends SOURCE, TARGET>> getAdapters() {
        adapterCache.clear();
        return adapters;
    }

    /**
     * @param obj The value to be converted. May be null.
     * @return The value, converted. Returns null if the null_adapter does not convert the input.
     */
    public <OBJ extends SOURCE> TARGET cast(OBJ obj) {
        if (obj == null) return null_adapter.adapt(null);
        Class<OBJ> objClass = (Class<OBJ>) obj.getClass(); // The docs claim the cast isn't necessary :|
        // (Might be a lang level thing.)
        Adapter<OBJ, TARGET> adapter = (Adapter<OBJ, TARGET>) adapterCache.get(objClass);
        if (adapter == null) {
            adapter = findAdapter(objClass);
            adapterCache.put(objClass, adapter);
        }
        return adapter.adapt(obj);
    }

    private InterfaceAdapter(Class<TARGET> targetInterface) {
        this.targetInterface = targetInterface;
    }

    private <OBJ extends SOURCE> Adapter<OBJ, TARGET> findAdapter(Class<OBJ> objClass) {
        if (adapterCache.isEmpty()) {
            Collections.sort(adapters, this);
        }
        if (targetInterface.isInstance(objClass)) {
            return (Adapter<OBJ, TARGET>) this; // the self adapter
        }
        for (Adapter<? extends SOURCE, TARGET> a : adapters) {
            if (a.canAdapt(objClass)) {
                return (Adapter<OBJ, TARGET>) a;
            }
        }
        return (Adapter<OBJ, TARGET>) fallbackAdapter;
    }

    private final HashMap<Class<? extends SOURCE>, Adapter<? extends SOURCE, TARGET>> adapterCache = new HashMap<Class<? extends SOURCE>, Adapter<? extends SOURCE, TARGET>>();
    private final ArrayList<Adapter<? extends SOURCE, TARGET>> adapters = new ArrayList<Adapter<? extends SOURCE, TARGET>>();
    private Adapter<SOURCE, TARGET> null_adapter = this;
    private Adapter<? extends SOURCE, TARGET> fallbackAdapter = (Adapter<? extends SOURCE, TARGET>) nullAdapter;
    private static final HashMap<Class, InterfaceAdapter> common_adapters = new HashMap<Class, InterfaceAdapter>();

    private static final Adapter<?, ?> nullAdapter = new GenericAdapter<Object, Object>(null /* rely on canCast() not being called */, null);

    // Comparator implementation; fewer classes! :D
    @Override
    @Deprecated
    public int compare(Adapter o1, Adapter o2) {
        int p1 = o1.priority();
        int p2 = o2.priority();
        return p1 - p2;
    }

    // 'self adapter' implementation; even fewer classes! :DD
    @Override
    @Deprecated
    public TARGET adapt(SOURCE val) {
        return (TARGET) val;
    }

    @Override
    @Deprecated
    public boolean canAdapt(Class<?> valClass) {
        return targetInterface.isAssignableFrom(valClass);
    }

    @Override
    @Deprecated
    public int priority() {
        return 0;
    }
}
