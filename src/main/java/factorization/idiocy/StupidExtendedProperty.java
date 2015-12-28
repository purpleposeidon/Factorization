package factorization.idiocy;

import net.minecraftforge.common.property.IUnlistedProperty;

public class StupidExtendedProperty<V> implements IUnlistedProperty<V> {
    public final String name;
    public final Class<V> klass;

    public StupidExtendedProperty(String name, Class<V> klass) {
        this.name = name;
        this.klass = klass;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isValid(V value) {
        return true;
    }

    @Override
    public Class<V> getType() {
        return klass;
    }

    @Override
    public String valueToString(V value) {
        return "<" + name + ">";
    }
}
