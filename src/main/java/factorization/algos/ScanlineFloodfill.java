package factorization.algos;

public abstract class ScanlineFloodfill {
    protected abstract boolean forward();
    protected abstract boolean backward();
    protected abstract void popQueue();
    protected abstract void savePlace();
    protected abstract void restorePlace();

    @SuppressWarnings("StatementWithEmptyBody")
    protected final void tick() {
        popQueue();
        savePlace();
        while (forward()) {  }
        restorePlace();
        while (backward()) { }
    }

}
