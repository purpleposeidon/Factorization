package factorization.scrap;

import java.util.Map;

public abstract class AbstractMapDeregister implements IRevertible {
    protected final Map map;
    protected final Object key, value;

    public AbstractMapDeregister(Map map, Object key) {
        this.map = map;
        this.key = key;
        this.value = map.get(key);
    }

    @Override
    public void apply() {
        map.remove(key);
    }

    @Override
    public void revert() {
        map.put(key, value);
    }

    @Override
    public String info() {
        return this.getClass().getSimpleName() + " " + ((Class) key).getCanonicalName() + " # " + value.getClass().getCanonicalName();
    }
}
