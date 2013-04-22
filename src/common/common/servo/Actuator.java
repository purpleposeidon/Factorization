package factorization.common.servo;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public abstract class Actuator {
    public abstract void use();
    
    @SideOnly(Side.CLIENT)
    public abstract void render();
}
