package factorization.api.adapter;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import factorization.api.energy.ContextBlock;
import factorization.api.energy.IWorker;
import net.minecraft.block.Block;

import java.util.*;

/**
 * Ascribes an interface to a type that might not actually implement it.
 * The implementation's a bit hairy; @see AdapterExample for a warm & fuzzy example.
 * @param <SRC> The input type
 * @param <DST> The class that the type shall be cast to.
 */
// Lots and lots of unchecked warnings :(
@SuppressWarnings("unused")
public final class InterfaceAdapter<SRC, DST> {
    public final Class<DST> targetInterface;

    /**
     * @param targetInterface The class object of the interface that will be adapted.
     * @param <S> The adaption source
     * @param <D> The adaption target, and class of {@code targetInterface}.
     * @return a globally shared InterfaceAdapter associated with the interface.
     */
    public static <S, D> InterfaceAdapter<S, D> get(Class<D> targetInterface) {
        InterfaceAdapter<?, ?> ret = default_adapters.get(targetInterface);
        if (ret == null) {
            ret = makeAdapter(targetInterface);
            default_adapters.put(targetInterface, ret);
        }
        if (ret.targetInterface != targetInterface) {
            throw new ClassCastException("targetInterfaces don't match");
        }
        return (InterfaceAdapter<S, D>) ret;
    }

    /**
     * @param targetInterface The class object of the interface that will be adapted.
     * @param <S> The adaption source
     * @param <T> The adaption target, and class of {@code targetInterface}.
     * @return an anonymous adapter
     */
    public static <S, T> InterfaceAdapter<S, T> makeAdapter(Class<T> targetInterface) {
        return new InterfaceAdapter<S, T>(targetInterface);
    }

    /**
     * Helper method for registering for use in situations where the interface does not rely on 'TARGET.this', such as Block.
     * @param inClass the class used for outInterface
     * @param outInterface the value that will be returned by cast() if the input is of type inClass.
     */
    public <OBJ extends SRC> InterfaceAdapter<SRC, DST> register(Class<OBJ> inClass, DST outInterface) {
        Adapter<OBJ, DST> ret = new GenericAdapter<OBJ, DST>(inClass, outInterface);
        register(ret);
        return this;
    }

    /**
     * Registers an adapter.
     * @param adapter The adapter to register
     * @return this
     */
    public <REG extends SRC> InterfaceAdapter<SRC, DST> register(Adapter<REG, DST> adapter) {
        adapters.add(adapter);
        adapterCache.clear();
        return this;
    }

    /**
     * @param fallbackAdapter the adapter to be used if no other adapter is suitable. canCast is *not* called.
     *                        The default behavior is to return null.
     */
    public InterfaceAdapter<SRC, DST> setFallbackAdapter(Adapter<? extends SRC, DST> fallbackAdapter) {
        adapterCache.clear();
        this.fallbackAdapter = fallbackAdapter;
        return this;
    }

    public Adapter<? extends SRC, DST> getFallbackAdapter() {
        return fallbackAdapter;
    }

    /**
     * @param nullAdapter the adapter to be used to handle null values. canCast is *not* called.
     *                    The default behavior is to return null.
     */
    public void setNullAdapter(Adapter<SRC, DST> nullAdapter) {
        adapterCache.clear();
        this.null_adapter = nullAdapter;
    }

    public Adapter<? extends SRC, DST> getNullAdapter() {
        return null_adapter;
    }

    /**
     * @return the list of adapters, for doing whatever with. Don't call this regularly.
     */
    public List<Adapter<? extends SRC, DST>> getAdapters() {
        adapterCache.clear();
        return adapters;
    }

    /**
     * @param obj The value to be converted. May be null.
     * @return The value, converted. Returns null if the null_adapter does not convert the input.
     */
    public <OBJ extends SRC> DST cast(OBJ obj) {
        if (obj == null) return null_adapter.adapt(null);
        Class<? extends OBJ> objClass = (Class<? extends OBJ>) obj.getClass(); // The docs claim the cast isn't necessary :|
        // (Might be a lang level thing.)
        Adapter<OBJ, DST> adapter = (Adapter<OBJ, DST>) adapterCache.get(objClass);
        if (adapter == null) {
            adapter = findAdapter(objClass);
            adapterCache.put(objClass, adapter);
        }
        return adapter.adapt(obj);
    }

    private InterfaceAdapter(Class<DST> targetInterface) {
        this.targetInterface = targetInterface;
    }

    private <OBJ extends SRC> Adapter<OBJ, DST> findAdapter(Class<? extends OBJ> objClass) {
        if (adapterCache.isEmpty()) {
            Collections.sort(adapters, ADAPTER_COMPARATOR);
        }
        if (targetInterface.isAssignableFrom(objClass)) {
            return (Adapter<OBJ, DST>) SELF_ADAPTER;
        }
        for (Adapter<? extends SRC, DST> a : adapters) {
            if (a.canAdapt(objClass)) {
                return (Adapter<OBJ, DST>) a;
            }
        }
        return (Adapter<OBJ, DST>) fallbackAdapter;
    }

    private final HashMap<Class<? extends SRC>, Adapter<? extends SRC, DST>> adapterCache = Maps.newHashMap();
    private final ArrayList<Adapter<? extends SRC, DST>> adapters = Lists.newArrayList();
    private Adapter<SRC, DST> null_adapter = (Adapter<SRC, DST>) NULL_ADAPTER;
    private Adapter<? extends SRC, DST> fallbackAdapter = (Adapter<? extends SRC, DST>) NULL_ADAPTER;
    private static final HashMap<Class, InterfaceAdapter<?, ?>> default_adapters = Maps.newHashMap();

    // These two adapters rely on canAdapt() not being called!
    private static final Adapter<?, ?> NULL_ADAPTER = new GenericAdapter<Object, Object>(null, null);
    private static final Adapter<?, ?> SELF_ADAPTER = new Adapter<Object, Object>() {
        @Override public Object adapt(Object val) { return val; }
        @Override public int priority() { return 0; }
        @Override public boolean canAdapt(Class<?> valClass) { throw new IllegalArgumentException("Don't ask me!"); }
    };
    private static final Comparator<Adapter> ADAPTER_COMPARATOR = new Comparator<Adapter>() {
        @Override
        public int compare(Adapter o1, Adapter o2) {
            int p1 = o1.priority();
            int p2 = o2.priority();
            return p1 - p2;
        }
    };
}
