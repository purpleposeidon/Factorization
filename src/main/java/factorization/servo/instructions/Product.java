package factorization.servo.instructions;

import factorization.servo.iterator.ServoMotor;
import factorization.servo.iterator.ServoStack;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

public class Product extends SimpleInstruction {
    @Override
    protected Object getRecipeItem() {
        return new ItemStack(Blocks.iron_bars);
    }
    
    @Override
    protected ItemStack getInstructionPlate() {
        return (new Sum()).toItem();
    }

    @Override
    public void motorHit(ServoMotor motor) {
        ServoStack stack = motor.getArgStack();
        Integer a = stack.popType(Integer.class);
        Integer b = stack.popType(Integer.class);
        if (a == null) a = 0;
        if (b == null) b = 0;
        stack.push(a*b);
    }

    @Override
    protected String getSimpleName() {
        return "product";
    }
}
