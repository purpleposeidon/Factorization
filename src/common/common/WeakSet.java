package factorization.common;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;


public class WeakSet<T> implements Set<T> {
    WeakHashMap<T, Object> under = new WeakHashMap<T, Object>();
    private static final Object EMPTY = new Object();

    @Override
    public boolean add(T e) {
        return under.put(e, EMPTY) == null;
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        boolean any = false;
        for (Object e : c) {
            any |= add((T) e);
        }
        return any;
    }

    @Override
    public void clear() {
        under.clear();
    }

    @Override
    public boolean contains(Object o) {
        return under.containsKey(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object e : c) {
            if (!under.containsKey(e)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isEmpty() {
        return under.isEmpty();
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            Iterator<Entry<T, Object>> it = under.entrySet().iterator();
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public T next() {
                return it.next().getKey();
            }

            @Override
            public void remove() {
                it.remove();
            }
            
        };
    }

    @Override
    public boolean remove(Object o) {
        return under.remove(o) == EMPTY;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean any = false;
        for (Object e : c) {
            any |= remove(e);
        }
        return any;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        boolean any = false;
        Iterator<Entry<T, Object>> it = under.entrySet().iterator();
        while (it.hasNext()) {
            if (!c.contains(it.next())) {
                it.remove();
                any = true;
            }
        }
        return any;
    }

    @Override
    public int size() {
        return under.size();
    }

    @Override
    public Object[] toArray() {
        return under.keySet().toArray();
    }

    @Override
    public <R> R[] toArray(R[] a) {
        return under.keySet().toArray(a);
    }
    
    
}
