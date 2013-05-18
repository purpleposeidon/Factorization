package factorization.common.servo;

import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public abstract class Actuator extends ServoComponent {
    /***
     * 
     * @param user The Entity that is using this; either an EntityPlayer or a ServoMotor
     * @param active Whether the device is actually enabled
     * @param sneaking Whether the alternate functionality should be used
     */
    public abstract void onUse(Entity user, boolean sneaking);
    
    public void onIdle(Entity user) { }
    
    @SideOnly(Side.CLIENT)
    public abstract void render();
    
    public static ItemStack takeItem(Entity user) {
        return null;
    }
    
    public boolean pushItem(Entity user, ItemStack is) {
        return false;
    }
}
