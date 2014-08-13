package factorization.colossi;

import static factorization.colossi.Animation.*;

public enum AnimationDefinitions {
    INSTANCE;
    
    void setup() { }
    
    {
        double LEG_SWING_ANGLE = 30;
        double PUSHOFF_FRAME_EXTENSION = -1;
        double PUSHOFF_FRAME_DURATION = 1.0/3.0;
        begin("legWalk");
        
        keyFrame(-LEG_SWING_ANGLE, 0, 0);
        
        withExtension(PUSHOFF_FRAME_EXTENSION);
        withDuration(PUSHOFF_FRAME_DURATION);
        keyFrame(-LEG_SWING_ANGLE, 0, 0);
        
        keyFrame(0, 0, 0);
        
        keyFrame(LEG_SWING_ANGLE, 0, 0);
        
        altPoint();
        keyFrame(0, 0, 0);
        
        withDuration(0);
        keyFrame(-LEG_SWING_ANGLE, 0, 0);
        
        end();
    }
}
