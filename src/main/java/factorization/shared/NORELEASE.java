package factorization.shared;

import com.google.common.base.Joiner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * Provides facilities for ensuring that dev-time debug cruft does not escape into the wild.
 * 
 * This class will be present at dev time, but must be excluded at compile time for non-test builds.
 * 
 */
@SuppressWarnings("unused")
public class NORELEASE {
    /**
     * Use with || NORELEASE.on
     * NORELEASE.just may be better.
     */
    public static boolean on = true;
    
    /**
     * Use with && NORELEASE.off
     * NORELEASE.just may be better.
     */
    public static boolean off = false;
    
    /**
     * Use to adjust numeric values, such as int constant = 0xBEEF + 23 * NORELEASE.one.
     * NORELEASE.just may be better.
     */
    public static int one = 1;
    
    /**
     * Use to adjust numeric values, such as int minVal = 10 * NORELEASE.zero
     * NORELEASE.just may be better.
     */
    public static int zero = 0;

    public static final Joiner joiner = Joiner.on(" ").useForNull("null");

    /**
     * Noise-free logging. (On Posix, at least)
     */
    public static void println(Object... msg) {
        String line = joiner.join(msg);
        trace.println(line);
    }

    /**
     * Just returns a value. Might be better than on/off/one/zero.
     */
    public static <E> E just(E v) {
        return v;
    }

    public static PrintStream trace;
    
    /**
     * Indicate something that needs to be done before release, eg <code>NORELEASE.fixme("some stuff")</code>
     * @param notes
     */
    public static void fixme(Object... notes) { }

    /**
     * Create a place to put a breakpoint
     */
    public static void breakpoint() { }
    
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
    public static short s1 = 0, s2 = 0, s3 = 0, s4 = 0;
    public static String str1 = "", str2 = "", str3 = "", str4 = "";
    public static long l1 = 0, l2 = 0, l3 = 0, l4 = 0;
    public static Object obj1 = null, obj2 = null, obj3 = null, obj4 = null;

    /**
     * Use this method to hotswap a try/catch clause.
     */
    public static Object catcher(Object input) {
        try {
            NORELEASE.just(input); // Keep this call here to ensure that this doesn't get optimized out
        } catch (Throwable t) {
            t.printStackTrace();;
            NORELEASE.breakpoint();
        }
        return null;
    }
}
