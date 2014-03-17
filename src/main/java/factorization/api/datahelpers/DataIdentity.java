package factorization.api.datahelpers;

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
    protected <E> Object putImplementation(E o) throws IOException {
        return o;
    }

}
