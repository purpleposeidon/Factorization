package factorization.rendersorting;

import java.util.ArrayList;

public class CleaningList<E> extends ArrayList<E> {
    private int lastMod = -1;

    public boolean setClean() {
        if (modCount == lastMod) return true;
        lastMod = modCount;
        return false;
    }

    public void setDirty() {
        lastMod = ~modCount;
    }
}
