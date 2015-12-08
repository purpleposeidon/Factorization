package factorization.servo.instructions;

import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.servo.Instruction;
import factorization.servo.ServoMotor;
import factorization.servo.ServoStack;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import java.io.IOException;

public class Product extends Instruction {
    @Override
    public IDataSerializable putData(String prefix, DataHelper data) throws IOException {
        return this;
    }

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
    public String getName() {
        return "fz.instruction.product";
    }
}
