package factorization.servo;

import factorization.api.FzColor;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.UnionEnumeration;
import factorization.servo.instructions.GenericPlaceholder;
import factorization.shared.Core;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Iterator;

public class ServoStack implements IDataSerializable, Iterable {
    private ArrayDeque<Object> contents = new ArrayDeque<Object>();
    private final int maxSize = 16;
    private final Executioner executioner;
    
    public ServoStack(Executioner executioner) {
        this.executioner = executioner;
    }

    public void setContentsList(Collection<Object> obj) {
        clear();
        contents.addAll(obj);
    }
    
    public void clear() {
        contents.clear();
        executioner.stacks_changed = true;
    }

    public boolean push(Object o) {
        if (o == null) {
            Core.logSevere("Tried to push null!");
            Thread.dumpStack();
            return false;
        }
        if (contents.size() >= maxSize) {
            return false;
        }
        contents.addFirst(o);
        executioner.stacks_changed = true;
        return true;
    }

    public boolean append(Object o) {
        if (o == null) {
            Core.logSevere("Tried to append null!");
            Thread.dumpStack();
            return false;
        }
        if (contents.size() >= maxSize) {
            return false;
        }
        contents.add(o);
        executioner.stacks_changed = true;
        return true;
    }

    public void forceAppend(Object o) {
        contents.add(o);
        executioner.stacks_changed = true;
    }

    public Object pop() {
        if (contents.isEmpty()) {
            return null;
        }
        executioner.stacks_changed = true;
        return contents.removeFirst();
    }
    
    public Object peek() {
        if (contents.isEmpty()) {
            return null;
        }
        return contents.getFirst();
    }

    public Object popEnd() {
        if (contents.isEmpty()) {
            return null;
        }
        executioner.stacks_changed = true;
        return contents.removeLast();
    }

    public <E> E popType(Class<? extends E> eClass) {
        return popType(eClass, true);
    }

    public Object remove(int index) {
        if (index >= contents.size()) {
            return null;
        }
        return contents.remove(index); // At least n is small. By default. >_>
    }

    public <E> E findType(Class<? extends E> eClass) {
        return popType(eClass, false);
    }

    private <E> E popType(Class<? extends E> eClass, boolean remove) {
        Iterator<Object> it = contents.iterator();
        while (it.hasNext()) {
            Object o = it.next();
            if (o == null) {
                it.remove();
                continue;
            }
            if (o.getClass() == eClass || eClass.isInstance(o)) {
                if (remove) {
                    executioner.stacks_changed = true;
                    it.remove();
                }
                return (E) o;
            }
        }
        return null;
    }

    @Override
    public Iterator<Object> iterator() {
        return contents.iterator();
    }
    
    public Iterator<Object> descendingIterator() {
        return contents.descendingIterator();
    }

    public int getMaxSize() {
        return maxSize;
    }

    public int getFreeSpace() {
        return Math.max(0, maxSize - contents.size());
    }

    public int getSize() {
        return contents.size();
    }

    static UnionEnumeration buildUnion() {
        UnionEnumeration ret = UnionEnumeration.build(
            // These odd void types were space for expanding. Not hard to be backwards compat.
            Void.TYPE, null, // 0
            Void.TYPE, null, // 1
            Void.TYPE, null, // 2
            Void.TYPE, null, // 3
            Void.TYPE, null, // 4
            Void.TYPE, null, // 5
            Void.TYPE, null, // 6
            Void.TYPE, null, // 7
            Void.TYPE, null, // 8
            Instruction.class, new GenericPlaceholder(), // 9
            Boolean.class, false,
            Byte.class, (byte) 0,
            Short.class, (short) 0,
            Integer.class, 0,
            Long.class, 0L,
            Float.class, 0F,
            Double.class, 0D,
            String.class, "",
            FzColor.class, FzColor.BLACK);
        if (ret.getIndex(false) != 10) throw new AssertionError(); // Should be 10 for compat
        if (ret.getIndex(new GenericPlaceholder()) != 9) throw new AssertionError(); // And likewise for insn
        return ret;
    }

    static final UnionEnumeration stackableTypes = buildUnion();

    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        int length = data.asSameShare(prefix + "_size").putInt(contents.size());
        if (length == 0) {
            if (data.isReader()) {
                contents.clear();
            }
            return this;
        }
        prefix = prefix + "#";
        if (data.isWriter()) {
            int i = 0;
            for (Object o : contents) {
                writeObject(data, prefix + i++, o);
            }
        } else {
            contents.clear();
            for (int i = 0; i < length; i++) {
                Object n = readObject(data, prefix + i);
                if (n != null) {
                    contents.add(n);
                }
            }
        }
        return this;
    }

    void writeObject(DataHelper data, String entryName, Object value) throws IOException {
        data.asSameShare(entryName).putUnion(stackableTypes, value);
    }

    Object readObject(DataHelper data, String entryName) throws IOException {
        return data.asSameShare(entryName).putUnion(stackableTypes, null);
    }


    @Override
    public String toString() {
        return contents.toString();
    }
    
    
}
