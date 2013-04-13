package factorization.api.datahelpers;

import java.io.IOException;

public interface IDataSerializable {
    IDataSerializable serialize(String prefix, DataHelper data) throws IOException; /* Could this be generic? */
}
