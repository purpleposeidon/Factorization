package factorization.common.servo;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

import net.minecraft.item.ItemStack;

import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;
import factorization.common.FactorizationUtil;

public class ServoStack extends DataHelper implements IDataSerializable, Iterable {
    private LinkedList<Object> contents = new LinkedList<Object>();
    private int maxSize = 16;
    
    public void setContentsList(LinkedList<Object> obj) {
        contents = obj;
    }
    
    public boolean push(Object o) {
        if (contents.size() >= maxSize) {
            return false;
        }
        contents.add(o);
        return true;
    }
    
    public void forcePush(Object o) {
        contents.add(o);
    }
    
    public Object pop() {
        if (contents.isEmpty()) {
            return null;
        }
        return contents.removeFirst();
    }
    
    public <E> E popType(Class<? extends E> eClass) {
        return popType(eClass, true);
    }
    
    public <E> E findType(Class<? extends E> eClass) {
        return popType(eClass, false);
    }
    
    private <E> E popType(Class<? extends E> eClass, boolean remove) {
        Iterator<Object> it = contents.iterator();
        while (it.hasNext()) {
            Object o = it.next();
            if (o.getClass() == eClass) {
                if (remove) {
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
    
    public void setMaxSize(int newSize) {
        maxSize = newSize;
    }
    
    public int getFreeSpace() {
        return Math.min(0, maxSize - contents.size());
    }
    
    public void pushmergeItemStack(ItemStack toAdd) {
        for (Object o : this) {
            if (!(o instanceof ItemStack)) {
                continue;
            }
            ItemStack is = (ItemStack) o;
            if (FactorizationUtil.couldMerge(is, toAdd)) {
                int free_space = Math.max(is.getMaxStackSize() - is.stackSize, 0);
                int delta = Math.min(free_space, toAdd.stackSize);
                toAdd.stackSize -= delta;
                is.stackSize += delta;
                if (toAdd.stackSize <= 0) {
                    break;
                }
            }
        }
    }
    
    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        int length = data.asSameShare(prefix + "size").putInt(contents.size());
        maxSize = data.asSameShare(prefix + "max").putInt(maxSize);
        
        prefix = prefix + "#";
        if (data.isWriter()) {
            int i = 0;
            for (Object o : contents) {
                data.asSameShare(prefix + i++).putObject(o);
            }
        } else {
            contents.clear();
            for (int i = 0; i < length; i++) {
                Object n = data.asSameShare(prefix + i).putObject(null);
                if (n != null) {
                    contents.add(n);
                }
            }
        }
        return this;
    }
    
    boolean reader;
    void setReader(boolean reader) {
        this.reader = reader;
    }
    
    boolean configuring;
    void setConfiguring(boolean configuring) {
        this.configuring = configuring;
    }
    
    @Override
    protected DataHelper makeChild_do() { return this; }

    @Override
    protected void finishChild_do() { }

    @Override
    protected boolean shouldStore(Share share) {
        if (configuring) {
            return share.client_can_edit;
        } else {
            return !share.is_transient;
        }
    }

    @Override
    public boolean isReader() {
        return reader;
    }
    
    private <E> E get(E value) throws IOException {
        if (reader) {
            E ret = (E)popType(value.getClass());
            if (ret == null) {
                throw new IOException();
            }
            return ret;
        }
        push(value);
        return value;
    }

    @Override
    public boolean putBoolean(boolean value) throws IOException {
        if (valid) {
            return get(value);
        }
        return value;
    }

    @Override
    public byte putByte(byte value) throws IOException {
        if (valid) {
            return get(value);
        }
        return value;
    }

    @Override
    public short putShort(short value) throws IOException {
        if (valid) {
            return get(value);
        }
        return value;
    }

    @Override
    public int putInt(int value) throws IOException {
        if (valid) {
            return get(value);
        }
        return value;
    }

    @Override
    public long putLong(long value) throws IOException {
        if (valid) {
            return get(value);
        }
        return value;
    }

    @Override
    public float putFloat(float value) throws IOException {
        if (valid) {
            return get(value);
        }
        return value;
    }

    @Override
    public double putDouble(double value) throws IOException {
        if (valid) {
            return get(value);
        }
        return value;
    }

    @Override
    public String putString(String value) throws IOException {
        if (valid) {
            return get(value);
        }
        return value;
    }

    @Override
    public ItemStack putItemStack(ItemStack value) throws IOException {
        if (valid) {
            return get(value);
        }
        return value;
    }
}
