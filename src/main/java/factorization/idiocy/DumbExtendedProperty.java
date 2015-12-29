package factorization.idiocy;

import net.minecraftforge.common.property.IUnlistedProperty;

public class DumbExtendedProperty<K> implements IUnlistedProperty<K> {
    public final String name;
    public final Class<K> klass;

    public DumbExtendedProperty(String name, Class<K> klass) {
        this.name = name;
        this.klass = klass;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isValid(K value) {
        return true;
    }

    @Override
    public Class<K> getType() {
        return klass;
    }

    @Override
    public String valueToString(K value) {
        return value.toString();
    }
}
