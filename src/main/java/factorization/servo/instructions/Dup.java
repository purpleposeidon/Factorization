package factorization.servo.instructions;

import java.io.IOException;

import net.minecraft.item.Item;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.common.BlockIcons;
import factorization.servo.Executioner;
import factorization.servo.Instruction;
import factorization.servo.ServoMotor;
import factorization.servo.ServoStack;

public class Dup extends Instruction {
    @Override
    public IDataSerializable putData(String prefix, DataHelper data) throws IOException {
        return this;
    }

    @Override
    protected ItemStack getRecipeItem() {
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
    public IIcon getIcon(ForgeDirection side) {
        return BlockIcons.servo$dup;
    }

    @Override
    public String getName() {
        return "fz.instruction.dup";
    }
}
