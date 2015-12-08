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

public class Drop extends Instruction {
    @Override
    public IDataSerializable putData(String prefix, DataHelper data) throws IOException {
        return this;
    }

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
    public String getName() {
        return "fz.instruction.drop";
    }
    
}
