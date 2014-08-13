package factorization.colossi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Animation {
    private static Map<String, ArrayList<KeyFrame>> animations = new HashMap();
    private static ArrayList<KeyFrame> currentFrameList = null;
    private static String currentName;
    private static int midPoint;
    
    /**
     * Starts a named animation.
     */
    public static void begin(String name) {
        currentFrameList = new ArrayList();
        currentName = name;
        midPoint = -1;
        reset();
    }
    
    /**
     * Ends a named animation.
     */
    public static void end() {
        if (currentName == null) {
            throw new IllegalStateException();
        }
        if (midPoint != -1) {
            ArrayList<KeyFrame> before = new ArrayList();
            ArrayList<KeyFrame> after = new ArrayList();
            for (int i = 0; i < currentFrameList.size(); i++) {
                (i < midPoint ? before : after).add(currentFrameList.get(i));
            }
            ArrayList<KeyFrame> joined = new ArrayList();
            joined.addAll(after);
            joined.addAll(before);
            animations.put(currentName + "Alt",  joined);
        }
        animations.put(currentName, currentFrameList);
        currentName = null;
        currentFrameList = null;
    }
    
    /**
     * Creates an animation named "<currentName>Alt" that starts after this point and runs to the last frame, looping around to the first frame and ending before this frame.
     */
    public static void altPoint() {
        midPoint = currentFrameList.size();
    }
    
    /**
     * Adds a KeyFrame to the current animation.
     * @param duration how long the keyframe lasts.
     * @param extension relative vertical motion of the part
     * @param swing rotation on the North-South axis
     * @param sweep rotation on the *body's* Up-Down axis
     * @param twist rotation on the *part's* Up-Down axis
     */
    public static void keyFrame(double duration, double extension, double swing, double sweep, double twist) {
        currentFrameList.add(new KeyFrame(duration, extension, swing, sweep, twist));
        reset();
    }
    
    /**
     * Adds a KeyFrame using the default, or earlier-provided, values for duration and extension.
     * The default value for the duration is 1. The default value for extension is 0.
     * duration and extension get reset to their default values every keyframe.
     * @param swing rotation on the North-South axis
     * @param sweep rotation on the *body's* Up-Down axis
     * @param twist rotation on the *part's* Up-Down axis
     */
    public static void keyFrame(double swing, double sweep, double twist) {
        keyFrame(current_duration, current_extension, swing, sweep, twist);
    }
    
    private static double current_duration, current_extension;
    
    private static void reset() {
        current_duration = 1;
        current_extension = 0;
    }
    
    public static void withDuration(double duration) {
        current_duration = duration;
    }
    
    public static void withExtension(double extension) {
        current_extension = extension;
    }
    
    static List<KeyFrame> lookup(String name) {
        AnimationDefinitions.INSTANCE.setup();
        return animations.get(name);
    }
}
