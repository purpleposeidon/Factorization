package factorization.servo.instructions;

import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.servo.Instruction;
import factorization.servo.ServoMotor;
import factorization.servo.ServoStack;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

import java.io.IOException;

public class Dup extends Instruction {
    @Override
    public IDataSerializable putData(String prefix, DataHelper data) throws IOException {
        return this;
    }

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
    public String getName() {
        return "fz.instruction.dup";
    }
}
