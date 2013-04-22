package factorization.api.datahelpers;

import java.io.IOException;

import net.minecraft.item.ItemStack;

public class DataIdentity extends DataHelper {
    final DataHelper parent;
    public DataIdentity(DataHelper parent) {
        this.parent = parent;
    }
    
    @Override
    protected DataHelper makeChild_do() {
        return this;
    }

    @Override
    protected void finishChild_do() {}

    @Override
    protected boolean shouldStore(Share share) {
        return false;
    }

    @Override
    public boolean isReader() {
        return parent.isReader();
    }

    @Override
    public boolean putBoolean(boolean value) throws IOException {
        return value;
    }

    @Override
    public byte putByte(byte value) throws IOException {
        return value;
    }

    @Override
    public short putShort(short value) throws IOException {
        return value;
    }

    @Override
    public int putInt(int value) throws IOException {
        return value;
    }

    @Override
    public long putLong(long value) throws IOException {
        return value;
    }

    @Override
    public float putFloat(float value) throws IOException {
        return value;
    }

    @Override
    public double putDouble(double value) throws IOException {
        return value;
    }

    @Override
    public String putString(String value) throws IOException {
        return value;
    }

    @Override
    public ItemStack putItemStack(ItemStack value) throws IOException {
        return value;
    }

}
