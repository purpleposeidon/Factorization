package factorization.servo;

import java.io.IOException;

import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import net.minecraft.item.ItemStack;

public class LoggerDataHelper extends DataHelper {
    ServoMotor motor;
    public boolean hadError = false;
    
    public LoggerDataHelper(ServoMotor motor) {
        this.motor = motor;
    }
    
    @Override
    protected boolean shouldStore(Share share) {
        return false;
    }

    @Override
    public boolean isReader() {
        return false;
    }
    
    @Override
    public boolean isWriter() {
        return false;
    }

    @Override
    protected <E> E putImplementation(E o) throws IOException {
        return o;
    }
    
    @Override
    public void log(String message) {
        motor.putError(message);
        hadError = true;
    }

    @Override
    public ItemStack[] putItemArray(ItemStack[] value) throws IOException {
        return putImplementation(value);
    }
}
