package factorization.util;

import com.google.common.base.Joiner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;

/**
 * Provides facilities for ensuring that dev-time debug/tracing/exploration code does not escape into the wild.
 * Also includes tools for working around hot-swap limitations.
 * <p/>
 * This class will be present at dev time, but must be excluded at compile time for release builds.
 */
@SuppressWarnings("unused")
public class NORELEASE {
    /**
     * A true value that is not subject to dead code elimination.
     * Possible usages include:
     * Forcing a boolean value to be true using ||
     * Forcing an early return
     */
    public static boolean on = true;

    /**
     * A false value that is not subject to dead code elimination.
     * Possible usages include:
     * Forcing a boolean value to be false using &&
     * Using an 'if' to comment out a block of code
     */
    public static boolean off = false;

    /**
     * A false value that should be replaced with 'true' for release.
     *
     * <pre>
     * if (NORELEASE.disabledUtilRelease) {
     *     cache.put(key, value);
     * }
     * </pre>
     */
    public static boolean disabledUntilRelease = false;

    private static final Joiner joiner = Joiner.on(" ").useForNull("null");

    /**
     * Noise-free logging. (On Posix, at least)
     */
    public static void println(Object... msg) {
        String line = joiner.join(msg);
        trace.println(line);
    }

    /**
     * Just returns a value. The intended use is for when a correct value has not yet been decided.
     */
    public static <E> E just(E v) {
        return v;
    }

    /**
     * Just returns the first value.
     * The intended usage is for temporarily using the first value, with the second value being what should
     * be used on release.
     */
    public static <E> E justNot(E v, E not) {
        return v;
    }

    /**
     * Just returns the first value.
     * The intended usage is for exploring various possible values.
     */
    public static <E> E justChoose(E v, E... not) {
        return v;
    }

    public static PrintStream trace;
    static {
        try {
            trace = new PrintStream(new FileOutputStream(new File("/dev/stderr")));
        } catch (FileNotFoundException e) {
            trace = System.err;
        }
    }

    /**
     * A compiler-enforced "to do".
     */
    public static void fixme(Object... notes) {
    }

    /**
     * Use this method to create a line of code that a debugging breakpoint can be placed on, eg
     * <code>
     *     if (someBuggyCondition()) {
     *         NORELEASE.breakpoint();
     *     }
     * </code>
     */
    public static void breakpoint() {
    }

    // Free variables for use when hotswapping code
    public static int i1 = 0, i2 = 0, i3 = 0, i4 = 0;
    public static boolean b1 = false, b2 = false, b3 = false, b4 = false;
    public static float f1 = 0, f2 = 0, f3 = 0, f4 = 0;
    public static double d1 = 0, d2 = 0, d3 = 0, d4 = 0;
    public static short s1 = 0, s2 = 0, s3 = 0, s4 = 0;
    public static String str1 = "", str2 = "", str3 = "", str4 = "";
    public static long l1 = 0, l2 = 0, l3 = 0, l4 = 0;
    public static Object obj1 = null, obj2 = null, obj3 = null, obj4 = null;
    public static HashMap<Object, Object> map1 = new HashMap<Object, Object>(), map2 = new HashMap<Object, Object>();

    /**
     * Use this method to hotswap a try/catch clause.
     */
    public static Object catcher(Object input) {
        try {
            NORELEASE.just(input); // Keep this call here to ensure that this doesn't get optimized out
        } catch (Throwable t) {
            t.printStackTrace();
            NORELEASE.breakpoint();
        }
        return null;
    }
}

