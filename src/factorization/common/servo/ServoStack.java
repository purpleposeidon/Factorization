package factorization.common.servo;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;

public class ServoStack implements IDataSerializable, Iterable {
    private LinkedList<Object> contents = new LinkedList<Object>();
    private final int maxSize = 16;
    private final ServoMotor motor;
    
    public ServoStack(ServoMotor motor) {
        this.motor = motor;
    }

    public void setContentsList(LinkedList<Object> obj) {
        contents = obj;
    }

    public boolean push(Object o) {
        if (contents.size() >= maxSize) {
            return false;
        }
        contents.addFirst(o);
        motor.stacks_changed = true;
        return true;
    }

    public boolean append(Object o) {
        if (contents.size() >= maxSize) {
            return false;
        }
        contents.add(o);
        motor.stacks_changed = true;
        return true;
    }

    public void forceAppend(Object o) {
        contents.add(o);
        motor.stacks_changed = true;
    }

    public Object pop() {
        if (contents.isEmpty()) {
            return null;
        }
        motor.stacks_changed = true;
        return contents.removeFirst();
    }

    public Object popEnd() {
        if (contents.isEmpty()) {
            return null;
        }
        motor.stacks_changed = true;
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
                    motor.stacks_changed = true;
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
                data.asSameShare(prefix + i++).putUntypedOject(o);
            }
        } else {
            contents.clear();
            for (int i = 0; i < length; i++) {
                Object n = data.asSameShare(prefix + i).putUntypedOject(null);
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
