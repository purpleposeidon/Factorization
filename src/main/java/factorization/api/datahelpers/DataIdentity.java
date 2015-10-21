package factorization.api.datahelpers;

import net.minecraft.item.ItemStack;

import java.io.IOException;

public class DataIdentity extends DataHelper {
    final DataHelper parent;
    public DataIdentity(DataHelper parent) {
        this.parent = parent;
    }

    @Override
    protected boolean shouldStore(Share share) {
        return false;
    }

    @Override
    public boolean isReader() {
        return parent.isReader();
    }
    
    @Override
    protected <E> E putImplementation(E o) throws IOException {
        return o;
    }

    @Override
    public ItemStack[] putItemArray(ItemStack[] value) throws IOException {
        return value;
    }

    @Override
    public int[] putIntArray(int[] value) throws IOException {
        return value;
    }
}
