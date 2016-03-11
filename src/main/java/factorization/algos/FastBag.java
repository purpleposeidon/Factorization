package factorization.algos;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;

public class FastBag<E> extends ArrayList<E> {
    // Written by cpw! I could easily have written it myself of course ;)

    public FastBag() {
        super();
    }

    public FastBag(int initialCapacity) {
        super(initialCapacity);
    }

    private static final long serialVersionUID = 1L;

    @Override
    public E remove(int index) {
        E last = get(size() - 1);
        E old = set(index, last);
        super.remove(size() - 1);
        return old;
    }

    public E removeAny() {
        return super.remove(size() - 1);
    }

    // Hey, cpw, why didn't you include an Iterator implementation?

    @Override
    public Iterator<E> iterator() {
        return new Itr();
    }

    class Itr implements Iterator<E> {
        int index = 0;
        @Override
        public boolean hasNext() {
            return index < size();
        }

        @Override
        public E next() {
            return FastBag.this.get(index++);
        }

        @Override
        public void remove() {
            FastBag.this.remove(--index);
        }
    }

    public static <E> List<E> create() {
        return new FastBag<E>();
    }

    public static <E> List<E> create(int initialCapacity) {
        return new FastBag<E>(initialCapacity);
    }
}