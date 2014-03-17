package factorization.api.datahelpers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DataBackup extends DataHelper {
    Map<String, Object> fields = new HashMap();
    boolean isReading = true; //Else: isRestoring
    
    @Override
    protected boolean shouldStore(Share share) {
        return share.is_public && share.client_can_edit;
    }
    
    @Override
    protected <E> Object putImplementation(E o) throws IOException {
        if (isReading) {
            fields.put(name, o);
            return o;
        } else {
            if (fields.containsKey(name)) {
                return fields.get(name);
            }
            return o;
        }
    }
    
    @Override
    public boolean isReader() {
        return isReading;
    }
    
    public void restoring() {
        isReading = false;
    }
}
