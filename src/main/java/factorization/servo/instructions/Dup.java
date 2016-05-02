package factorization.servo.instructions;

import factorization.servo.ServoMotor;
import factorization.servo.ServoStack;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

public class Dup extends SimpleInstruction {
    @Override
    protected Object getRecipeItem() {
        return new ItemStack(Items.egg);
    }

    @Override
    public void motorHit(ServoMotor motor) {
        ServoStack stack = motor.getArgStack();
        Object a = stack.pop();
        if (a == null) {
            motor.putError("Dup: Stack underflow");
            return;
        }
        stack.push(a);
        stack.push(a);
    }

    @Override
    protected String getSimpleName() {
        return "dup";
    }
}
