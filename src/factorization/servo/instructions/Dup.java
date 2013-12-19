package factorization.servo.instructions;

import java.io.IOException;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.common.BlockIcons;
import factorization.servo.Instruction;
import factorization.servo.ServoMotor;
import factorization.servo.ServoStack;

public class Dup extends Instruction {
    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        return this;
    }

    @Override
    protected ItemStack getRecipeItem() {
        return new ItemStack(Item.egg);
    }

    @Override
    public void motorHit(ServoMotor motor) {
        ServoStack stack = motor.getServoStack(ServoMotor.STACK_ARGUMENT);
        Object a = stack.pop();
        if (a == null) {
            motor.putError("Dup: Stack underflow");
            return;
        }
        stack.push(a);
        stack.push(a);
    }

    @Override
    public Icon getIcon(ForgeDirection side) {
        return BlockIcons.servo$dup;
    }

    @Override
    public String getName() {
        return "fz.instruction.dup";
    }
}
