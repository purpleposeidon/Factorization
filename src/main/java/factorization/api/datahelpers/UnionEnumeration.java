package factorization.api.datahelpers;

import factorization.servo.LoggerDataHelper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Holds a list of types and their default values.
 */
public class UnionEnumeration {
    public static final UnionEnumeration empty = new UnionEnumeration(new Class<?>[0], new Object[0]);

    private UnionEnumeration(Class<?>[] classes, Object[] zeros) {
        if (classes.length != zeros.length) throw new IllegalArgumentException("Sizes do not match");
        this.classes = classes;
        this.zeros = zeros;
        if (classes.length > Byte.MAX_VALUE) {
            throw new IllegalArgumentException("Too many types!");
        }
        for (int i = 0; i < classes.length; i++) {
            indexMap.put(classes[i], i);
        }
        DataValidator data = new DataValidator(new HashMap<String, Object>());
        for (int i = 0; i < classes.length; i++) {
            try {
                data.as(Share.VISIBLE_TRANSIENT, "#" + i).putUnion(this, zeros[i]);
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    public static UnionEnumeration build(Object ...parts) {
        if (parts.length % 2 != 0) throw new IllegalArgumentException("Not pairs");
        Class<?> classes[] = new Class<?>[parts.length / 2];
        Object zeros[] = new Object[parts.length / 2];
        for (int i = 0; i < parts.length; i += 2) {
            Class<?> klass = (Class<?>) parts[i];
            Object val = parts[i + 1];
            if (val != null && klass != val.getClass()) throw new IllegalArgumentException("default value does not match class");
            classes[i / 2] = klass;
            zeros[i / 2] = val;
            if (val == null ^ klass == Void.TYPE) {
                throw new IllegalArgumentException("nulls must correspond to Voids.");
            }
        }
        return new UnionEnumeration(classes, zeros);
    }

    final Class<?> classes[];
    final Object zeros[];
    final Map<Class<?>, Integer> indexMap = new HashMap<Class<?>, Integer>();

    public byte getIndex(Object val) {
        return (byte) (int) indexMap.get(val.getClass());
    }

    public Object byIndex(byte b) {
        return zeros[b];
    }

    public Class<?> classByIndex(byte index) {
        return classes[index];
    }
}
