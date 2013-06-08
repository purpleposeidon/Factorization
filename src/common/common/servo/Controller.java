package factorization.common.servo;

import java.util.LinkedList;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.Coord;


public abstract class Controller extends ServoComponent {
    public abstract void doUpdate(ServoMotor motor);
    
    @Override
    public boolean onClick(EntityPlayer player, Coord block, ForgeDirection side) {
        return false;
    }
    
    @Override
    public boolean onClick(EntityPlayer player, ServoMotor motor) {
        if (motor.controller == this) {
            return false;
        }
        if (motor.controller != null) {
            LinkedList<Object> toDrop = new LinkedList();
            motor.controller.deconfigure(toDrop);
            motor.dropItemStacks(toDrop);
        }
        motor.controller = this;
        return true;
    }
}
