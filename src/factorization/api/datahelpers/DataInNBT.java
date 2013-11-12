package factorization.api.datahelpers;

import java.io.IOException;

import factorization.common.Core;

import net.minecraft.nbt.NBTTagCompound;

public class DataInNBT extends DataHelperNBT {	
    public DataInNBT(NBTTagCompound theTag) {
        tag = theTag;
    }
    
    @Override
    protected boolean shouldStore(Share share) {
        return !share.is_transient /*&& tag.hasKey(name)*/;
    }
    
    @Override
    public boolean isReader() {
        return true;
    }
    
    @Override
    protected <E> Object putImplementation(E o) throws IOException {
        if (!tag.hasKey(name)) {
            return o;
        }
        try {
            if (o instanceof Boolean) {
                return tag.getBoolean(name);
            } else if (o instanceof Byte) {
                return tag.getByte(name);
            } else if (o instanceof Short) {
                return tag.getShort(name);
            } else if (o instanceof Integer) {
                return tag.getInteger(name);
            } else if (o instanceof Long) {
                return tag.getLong(name);
            } else if (o instanceof Float) {
                return tag.getFloat(name);
            } else if (o instanceof Double) {
                return tag.getDouble(name);			
            } else if (o instanceof String) {
                return tag.getString(name);
            } else if (o instanceof NBTTagCompound) {
                return tag.getCompoundTag(name);
            }
        } catch (Throwable t) {
            Core.logWarning("Failed to load " + name + "; will use default value " + o);
            Core.logWarning("The tag: " + tag);
            t.printStackTrace();
        }
        return o;
    }
    
    @Override
    public boolean hasLegacy(String name) {
        return tag.hasKey(name);
    }

}
