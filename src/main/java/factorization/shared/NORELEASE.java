package factorization.shared;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import com.google.common.base.Joiner;

/**
 * Provides facilities for ensuring that dev-time debug cruft does not escape into the wild.
 * 
 * This class will be present at dev time, but must be excluded at compile time for non-test builds.
 * 
 */
public class NORELEASE {
    /**
     * Use with || NORELEASE.on
     */
    public static boolean on = true;
    
    /**
     * Use with && NORELEASE.off
     */
    public static boolean off = false;
    
    /**
     * Use to adjust numeric values, such as int constant = 0xBEEF + 23 * NORELEASE.one
     */
    public static int one = 1;
    
    /**
     * Use to adjust numeric values, such as int minVal = 10 * NORELEASE.zero
     */
    public static int zero = 0;
    
    /**
     * Noise-free logging. (On Posix, at least)
     * @param msg
     */
    public static void println(Object... msg) {
        String line = Joiner.on(" ").join(msg);
        trace.println(line);
    }
    
    public static PrintStream trace;
    
    /**
     * Indicate something that needs to be done before release. Maybe use as NORELEASE.fixme(/* some stuff &#42;/)
     * @param notes
     */
    public static void fixme(Object... notes) { }
    
    static {
        try {
            trace = new PrintStream(new FileOutputStream(new File("/dev/stderr")));
        } catch (FileNotFoundException e) {
            trace = System.err;
        }
    }
    
    // Free variables for use when hotswapping code
    public static int i1 = 0, i2 = 0, i3 = 0, i4 = 0;
    public static boolean b1 = false, b2 = false, b3 = false, b4 = false;
    public static Object obj1 = null, obj2 = null, obj3 = null, obj4 = null;
}
