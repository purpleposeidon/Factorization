package factorization.api.datahelpers;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.io.IOException;

public class RegisteredDataUnion {
    final BiMap<Class<? extends IDataSerializable>, String> map = HashBiMap.create();

    public <T extends IDataSerializable> RegisteredDataUnion add(String name, Class<T> klass) {
        map.put(klass, name);
        return this;
    }

    public IDataSerializable put(String prefix, DataHelper data, IDataSerializable value) throws IOException {
        String typeIndex = prefix + ".type";
        if (data.isWriter()) {
            data.asSameShare(typeIndex).putString(map.get(value.getClass()));
        } else {
            String typeId = data.asSameShare(typeIndex).putString("null");
            try {
                value = map.inverse().get(typeId).newInstance();
            } catch (Throwable e) {
                throw new IOException(e);
            }
        }
        return value.serialize(prefix, data);
    }
}
