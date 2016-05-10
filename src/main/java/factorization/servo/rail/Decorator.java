package factorization.servo.rail;

import factorization.servo.iterator.AbstractServoMachine;
import factorization.servo.iterator.ServoMotor;
import factorization.servo.stepper.StepperEngine;
import factorization.shared.Core;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;


public abstract class Decorator extends ServoComponent {
    public abstract void motorHit(ServoMotor motor);
    public boolean preMotorHit(ServoMotor motor) {
        return false;
    }

    public void stepperHit(StepperEngine engine) { }

    public void iteratorHit(AbstractServoMachine iterator) {
        if (iterator instanceof ServoMotor) {
            motorHit((ServoMotor) iterator);
        } else if (iterator instanceof StepperEngine) {
            stepperHit((StepperEngine) iterator);
        }
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

    public boolean collides() {
        return true;
    }
}
