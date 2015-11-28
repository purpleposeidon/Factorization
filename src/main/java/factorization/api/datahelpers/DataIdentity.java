package factorization.api.datahelpers;

import java.io.IOException;

public class DataIdentity extends MergedDataHelper {
    public static final DataHelper instance = new DataIdentity();

    @Override
    protected boolean shouldStore(Share share) {
        return false;
    }

    @Override
    public boolean isValid() {
        return false;
    }

    @Override
    public boolean isReader() {
        return false;
    }

    @Override
    protected <E> E putImplementation(E o) throws IOException {
        return o;
    }
}
