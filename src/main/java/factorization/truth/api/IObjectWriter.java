package factorization.truth.api;

import factorization.api.adapter.InterfaceAdapter;

import java.util.List;

public interface IObjectWriter<T> {
    /**
     * You can register IObjectWriters here to adapt classes you have no control over, such as vanilla minecraft objects.
     */
    static InterfaceAdapter<Object, IObjectWriter> adapter = InterfaceAdapter.get(IObjectWriter.class);

    /**
     * @param out          A list of Words and Strings.
     * @param val          The object that's being examined. Possibly just 'this'.
     * @param generic      A converter that can handles arbitrary objects, including ItemStacks and many other typical
     */
    void writeObject(List out, T val, IObjectWriter<Object> generic);
}
