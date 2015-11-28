package factorization.api.datahelpers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DataBackup extends MergedDataHelper {
    Map<String, Object> fields = new HashMap<String, Object>();
    boolean isReading = true; //Else: isRestoring
    
    @Override
    protected boolean shouldStore(Share share) {
        return share.is_public && share.client_can_edit;
    }
    
    @Override
    protected <E> E putImplementation(E o) throws IOException {
        if (isReading) {
            fields.put(name, o);
            return o;
        } else {
            if (fields.containsKey(name)) {
                return (E) fields.get(name);
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
