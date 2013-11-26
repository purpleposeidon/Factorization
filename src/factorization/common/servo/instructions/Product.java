package factorization.common.servo.instructions;

import java.io.IOException;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.common.BlockIcons;
import factorization.common.servo.Instruction;
import factorization.common.servo.ServoMotor;
import factorization.common.servo.ServoStack;

public class Product extends Instruction {
    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        return this;
    }

    @Override
    protected ItemStack getRecipeItem() {
        return new ItemStack(Block.fenceIron);
    }
    
    @Override
    protected ItemStack getInstructionPlate() {
        return (new Sum()).toItem();
    }

    @Override
    public void motorHit(ServoMotor motor) {
        ServoStack stack = motor.getServoStack(ServoMotor.STACK_ARGUMENT);
        Integer a = stack.popType(Integer.class);
        Integer b = stack.popType(Integer.class);
        if (a == null) a = 0;
        if (b == null) b = 0;
        stack.push(a*b);
    }

    @Override
    public Icon getIcon(ForgeDirection side) {
        return BlockIcons.servo$product;
    }

    @Override
    public String getName() {
        return "fz.instruction.product";
    }
}
