package factorization.algos;

public class TortoiseAndHare {
    public static interface Advancer<E> {
        E getNext(E node);
    }

    public static <E> E race(E start, Advancer<E> advancer) {
        E tortoise = start;
        E hare = start;
        while (true) {
            for (int i = 0; i < 2; i++) {
                hare = advancer.getNext(hare);
                if (hare == null) return null;
                if (hare.equals(tortoise)) return hare;
            }
            tortoise = advancer.getNext(tortoise);
        }
    }
}
