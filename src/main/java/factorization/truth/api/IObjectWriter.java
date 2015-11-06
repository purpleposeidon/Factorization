package factorization.truth.api;

import factorization.api.adapter.InterfaceAdapter;

import java.util.List;

public interface IObjectWriter<T> {
    /**
     * You can register IObjectWriters here to adapt classes you have no control over, such as vanilla minecraft objects.
     * If your class implements IObjectWriter, then there's no need to register it..
     */
    static InterfaceAdapter<Object, IObjectWriter> adapter = InterfaceAdapter.get(IObjectWriter.class);

    /**
     * @param out          A list of Words and Strings.
     * @param val          The object that's being examined. Possibly just 'this'; if val implements IObjectWriter,
     *                     then val.writeObject() is what is being called.
     * @param generic      A converter that can handles arbitrary objects, including ItemStacks and many other typical types.
     */
    void writeObject(List out, T val, IObjectWriter<Object> generic);
}
