package factorization.api.datahelpers;

import java.io.IOException;

public interface IDataSerializable {
    /**
     * This is a highly generic serialization function. It is used for reading and writing packets, NBT tags, and possibly more exotic things.
     * @param prefix Used for custom data types, such as vectors. (I should maybe get rid of this.)
     * @param data
     * @return
     * @throws IOException
     */
    IDataSerializable serialize(String prefix, DataHelper data) throws IOException;
}
