package factorization.api;

import com.google.common.collect.ImmutableList;

import java.util.*;

/**
 * Ascribes an interface to a type that doesn't actually implement it.
 * @see AdapterExample
 */
public class InterfaceAdapter<TARGET> implements Comparator<InterfaceAdapter.Adapter> {
    public InterfaceAdapter(Class<TARGET> targetInterface) {
        this.targetInterface = targetInterface;
    }

    public <IN> Adapter<IN, TARGET> register(Class<IN> inClass, TARGET outInterface) {
        Adapter<IN, TARGET> ret = new GenericAdapter<IN, TARGET>(inClass, outInterface);
        register(ret);
        return ret;
    }

    public <IN> void register(Adapter<IN, TARGET> adapter) {
        adapters.add(adapter);
        adapterCache.clear();
        Collections.sort(adapters, this);
    }

    public <A> void remove(Adapter<A, TARGET> adapter) {
        adapters.remove(adapter);
        adapterCache.clear();
    }

    public List<Adapter<?, TARGET>> get() {
        return ImmutableList.copyOf(adapters);
    }

    public <A> TARGET cast(A obj) {
        if (obj == null) return null;
        final Class<?> objClass = obj.getClass();
        Adapter<A, TARGET> adapter = (Adapter<A, TARGET>) adapterCache.get(objClass);
        if (adapter == null) {
            adapter = (Adapter<A, TARGET>) findAdapter(objClass);
            adapterCache.put(objClass, adapter);
        }
        return adapter.cast(obj);
    }

    public interface Adapter<IN_TYPE, OUT_TYPE> {
        OUT_TYPE cast(IN_TYPE val);
        boolean canCast(Class<?> valClass);
        int priority();
    }

    private <A> Adapter<A, TARGET> findAdapter(Class<A> objClass) {
        if (targetInterface.isInstance(objClass)) {
            return (Adapter<A, TARGET>) selfAdapter;
        }
        for (Adapter<?, TARGET> a : adapters) {
            if (a.canCast(objClass)) {
                return (Adapter<A, TARGET>) a;
            }
        }
        return (Adapter<A, TARGET>) nullAdapter;
    }

    public final Class<TARGET> targetInterface;
    private final HashMap<Class<?>, Adapter<?, TARGET>> adapterCache = new HashMap<Class<?>, Adapter<?, TARGET>>();
    private final ArrayList<Adapter<?, TARGET>> adapters = new ArrayList<Adapter<?, TARGET>>();

    private static class GenericAdapter<SELF_IN, SELF_OUT> implements Adapter<SELF_IN, SELF_OUT> {
        private final Class<SELF_IN> genericClass;
        private final SELF_OUT genericInterface;

        private GenericAdapter(Class<SELF_IN> genericClass, SELF_OUT genericInterface) {
            this.genericClass = genericClass;
            this.genericInterface = genericInterface;
        }

        @Override
        public SELF_OUT cast(SELF_IN val) {
            return genericInterface;
        }

        @Override
        public boolean canCast(Class<?> valClass) {
            return genericClass.isAssignableFrom(valClass);
        }

        @Override
        public int priority() {
            return 1;
        }
    }

    private static final Adapter<?, ?> selfAdapter = new Adapter<Object, Object>() {
        @Override
        public Object cast(Object val) {
            return val;
        }

        @Override
        public boolean canCast(Class<?> valClass) {
            return false;
        }

        @Override
        public int priority() {
            return 10;
        }
    };

    private static final Adapter<?, ?> nullAdapter = new GenericAdapter<Object, Object>(null /* rely on canCast() not being called */, null);

    @Override
    public int compare(Adapter o1, Adapter o2) {
        int p1 = o1.priority();
        int p2 = o2.priority();
        return p1 - p2;
    }
}
