package factorization.common.servo;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public abstract class Actuator extends ServoComponent {
    public abstract void tick(ServoMotor motor, boolean active);
    
    @SideOnly(Side.CLIENT)
    public abstract void render();
}
