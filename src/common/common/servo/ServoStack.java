package factorization.common.servo;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;

public class ServoStack implements IDataSerializable {
    private LinkedList<Object> contents = new LinkedList<Object>();
    private int maxSize = 16;
    
    public boolean push(Object o) {
        if (contents.size() >= maxSize) {
            return false;
        }
        contents.add(o);
        return true;
    }
    
    public Object pop() {
        if (contents.isEmpty()) {
            return null;
        }
        return contents.removeFirst();
    }
    
    public Iterator<Object> iterator() {
        return contents.iterator();
    }
    
    public int getMaxSize() {
        return maxSize;
    }
    
    public void setMaxSize(int newSize) {
        maxSize = newSize;
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
}
