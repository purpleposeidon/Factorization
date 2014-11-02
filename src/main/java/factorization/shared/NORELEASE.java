package factorization.shared;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import com.google.common.base.Joiner;

public class NORELEASE {
    public static boolean on = true;
    public static boolean off = false;
    public static int one = 1;
    public static int zero = 0;
    
    public static void println(Object... msg) {
        String line = Joiner.on(" ").join(msg);
        trace.println(line);
    }
    
    public static PrintStream trace;
    
    
    static {
        try {
            trace = new PrintStream(new FileOutputStream(new File("/dev/stderr")));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            trace = System.err;
        }
    }
}
