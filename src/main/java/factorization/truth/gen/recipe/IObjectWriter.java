package factorization.truth.gen.recipe;

import factorization.api.adapter.InterfaceAdapter;

import java.util.List;

public interface IObjectWriter<T> {
    static InterfaceAdapter<Object, IObjectWriter> adapter = InterfaceAdapter.get(IObjectWriter.class);

    /**
     * @param out          A list of Words and Strings. See
     * @param val          The object that's being examined
     * @param generic      A converter that can handles arbitrary objects, including ItemStacks and many other typical
     */
    void writeObject(List out, T val, IObjectWriter<Object> generic);
}
