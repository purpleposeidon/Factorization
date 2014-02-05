package factorization.servo;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Iterator;

import net.minecraft.nbt.NBTTagCompound;
import factorization.api.FzColor;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;
import factorization.shared.Core;

public class ServoStack implements IDataSerializable, Iterable {
    private ArrayDeque<Object> contents = new ArrayDeque<Object>();
    private final int maxSize = 16;
    private final Executioner executioner;
    
    public ServoStack(Executioner executioner) {
        this.executioner = executioner;
    }

    public void setContentsList(Collection<Object> obj) {
        contents.clear();
        contents.addAll(obj);
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
            if (o.getClass() == eClass) {
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

    public int getMaxSize() {
        return maxSize;
    }

    public int getFreeSpace() {
        return Math.max(0, maxSize - contents.size());
    }

    public int getSize() {
        return contents.size();
    }
    
    private static final class TypeInfo {
        final byte id;
        final Class theClass;
        final Object defaultValue;
        
        TypeInfo(byte id, Class theClass, Object defaultValue) {
            this.id = id;
            this.theClass = theClass;
            this.defaultValue = defaultValue;
        }
        
        private static byte count = 10;
        private static <O> TypeInfo def(Class<O> theClass, O def) {
            return new TypeInfo(count++, theClass, def);
        }
        
        private static TypeInfo[] typeInfo = new TypeInfo[] {
            def(Boolean.class, false),
            def(Byte.class, (byte) 0),
            def(Short.class, (short) 0),
            def(Integer.class, (int) 0),
            def(Long.class, (long) 0),
            def(Float.class, 0F),
            def(Double.class, 0D),
            def(String.class, ""),
            def(FzColor.class, FzColor.BLACK)
        };
        
        public static TypeInfo forObject(Object o) {
            Class theClass = o.getClass();
            byte id = -1;
            for (TypeInfo ti : typeInfo) {
                if (ti.theClass == theClass) {
                    return ti;
                }
            }
            throw new IllegalArgumentException("Can't handle: " + o);
        }
        
        public static TypeInfo forId(byte id) {
            for (TypeInfo ti : typeInfo) {
                if (ti.id == id) {
                    return ti;
                }
            }
            throw new IllegalArgumentException("Unknown type id: " + id);
        }
    }
    static final byte INSTRUCTION_ID = 9; //Below TypeInfo.count
    
    public void writeObject(DataHelper data, String entryName, Object value) throws IOException {
        if (value instanceof Instruction) {
            NBTTagCompound tag = new NBTTagCompound();
            ((Instruction) value).save(tag);
            
            data.asSameShare(entryName + ".type").putByte(INSTRUCTION_ID);
            data.asSameShare(entryName).put(tag);
        } else {
            TypeInfo ti = TypeInfo.forObject(value);
            data.asSameShare(entryName + ".type").putByte(ti.id);
            data.asSameShare(entryName).put(value);
        }
    }
    
    public Object readObject(DataHelper data, String entryName) throws IOException {
        byte id = data.asSameShare(entryName + ".type").putByte((byte)-1);
        if (id == -1) throw new IOException("Missing .type for " + entryName);
        if (id == INSTRUCTION_ID) {
            NBTTagCompound tag = data.asSameShare(entryName).putTag(new NBTTagCompound());
            return (Instruction) ServoComponent.load(tag);
        } else {
            TypeInfo ti = TypeInfo.forId(id);
            return data.asSameShare(entryName).put(ti.defaultValue);
        }
        
    }

    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        int length = data.asSameShare(prefix + "_size").putInt(contents.size());
        if (length == 0) {
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
    
    private class DataServoStack extends DataHelper {
        final boolean reader;
        public DataServoStack(boolean reader) {
            this.reader = reader;
        }

        @Override
        protected boolean shouldStore(Share share) {
            return true;
        }

        @Override
        public boolean isReader() {
            return reader;
        }

        @Override
        protected <E> Object putImplementation(E value) throws IOException {
            if (reader) {
                E ret = (E) popType(value.getClass());
                if (ret == null) {
                    throw new IOException();
                }
                return ret;
            } else {
                push(value);
                return value;
            }
        }
        
    }
    
    public DataHelper getDataHelper(boolean reader) {
        return new DataServoStack(reader);
    }
    
    @Override
    public String toString() {
        return contents.toString();
    }
}
