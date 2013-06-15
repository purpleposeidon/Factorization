package factorization.api.datahelpers;

import java.io.IOException;

import net.minecraft.item.ItemStack;

public class WrappedDataHelper extends DataHelper {
    DataHelper under;
    
    public WrappedDataHelper(DataHelper under) {
        this.under = under;
    }
    
    @Override
    protected DataHelper makeChild_do() {
        return under.makeChild_do();
    }

    @Override
    protected void finishChild_do() {
        under.finishChild_do();
    }

    @Override
    protected boolean shouldStore(Share share) {
        return under.shouldStore(share);
    }

    @Override
    public boolean isReader() {
        return under.isReader();
    }

    @Override
    public boolean putBoolean(boolean value) throws IOException {
        return under.putBoolean(value);
    }

    @Override
    public byte putByte(byte value) throws IOException {
        return under.putByte(value);
    }

    @Override
    public short putShort(short value) throws IOException {
        return under.putShort(value);
    }

    @Override
    public int putInt(int value) throws IOException {
        return under.putInt(value);
    }

    @Override
    public long putLong(long value) throws IOException {
        return under.putLong(value);
    }

    @Override
    public float putFloat(float value) throws IOException {
        return under.putFloat(value);
    }

    @Override
    public double putDouble(double value) throws IOException {
        return under.putDouble(value);
    }

    @Override
    public String putString(String value) throws IOException {
        return under.putString(value);
    }

    @Override
    public ItemStack putItemStack(ItemStack value) throws IOException {
        return under.putItemStack(value);
    }

}
