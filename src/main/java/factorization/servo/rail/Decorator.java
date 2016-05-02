package factorization.servo.rail;

import factorization.servo.iterator.ServoMotor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import factorization.servo.stepper.StepperEngine;
import factorization.shared.Core;


public abstract class Decorator extends ServoComponent {
    public abstract void motorHit(ServoMotor motor);
    public boolean preMotorHit(ServoMotor motor) {
        return false;
    }

    public void stepperHit(StepperEngine engine) { }

    public float getSize() {
        return TileEntityServoRail.width - 1F/2048F;
        //return 6F/16F;
    }
    
    public static boolean playerHasProgrammer(EntityPlayer player) {
        if (player == null) {
            return false;
        }
        ItemStack cur = player.getCurrentEquippedItem();
        if (cur == null) {
            return false;
        }
        return cur.getItem() == Core.registry.logicMatrixProgrammer;
    }
    
    public boolean isFreeToPlace() {
        return false;
    }
    
    public String getInfo() {
        return null;
    }
    
    public void onPlacedOnRail(TileEntityServoRail sr) {}
    
    public boolean collides() {
        return true;
    }
    
    public void afterClientLoad(TileEntityServoRail rail) { }
}
