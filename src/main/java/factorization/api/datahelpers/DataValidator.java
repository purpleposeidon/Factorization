package factorization.api.datahelpers;

import net.minecraft.item.ItemStack;

import java.io.IOException;
import java.util.Map;


public class DataValidator extends DataHelper {
    Map<String, Object> fields;
    
    public DataValidator(Map<String, Object> fields) {
        this.fields = fields;
    }
    
    @Override
    protected boolean shouldStore(Share share) {
        return share.is_public && share.client_can_edit;
    }
    
    @Override
    protected <E> E putImplementation(E o) throws IOException {
        if (!fields.containsKey(name)) {
            log("Missing data");
            return o;
        }
        return (E) fields.get(name);
    }
    
    @Override
    public boolean isReader() {
        return true;
    }
    
    boolean has_log = false;
    
    @Override
    public void log(String message) {
        super.log(message);
        has_log = true;
    }
    
    public boolean isValid() {
        return !has_log;
    }

    @Override
    public ItemStack[] putItemArray(ItemStack[] value) throws IOException {
        return putImplementation(value);
    }

    @Override
    public int[] putIntArray(int[] value) throws IOException {
        return putImplementation(value);
    }
}