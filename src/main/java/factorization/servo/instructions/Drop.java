package factorization.servo.instructions;

import factorization.servo.ServoMotor;
import factorization.servo.ServoStack;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

public class Drop extends SimpleInstruction {
    @Override
    protected Object getRecipeItem() {
        return new ItemStack(Blocks.dropper);
    }

    @Override
    public void motorHit(ServoMotor motor) {
        ServoStack stack = motor.getArgStack();
        stack.pop();
    }

    @Override
    protected String getSimpleName() {
        return "drop";
    }
}
