package factorization.shared;

import java.util.ArrayList;

public class FastBag<E> extends ArrayList<E> {
    // Written by cpw! I could easily have written it myself of course ;)

    private static final long serialVersionUID = 1L;

    @Override
    public E remove(int index) {
        E last = get(size() - 1);
        E old = set(index, last);
        super.remove(size() - 1);
        return old;
    }
}