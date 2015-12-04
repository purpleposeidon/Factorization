package factorization.servo.instructions;

import java.io.IOException;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.EnumFacing;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.common.BlockIcons;
import factorization.servo.Executioner;
import factorization.servo.Instruction;
import factorization.servo.ServoMotor;
import factorization.servo.ServoStack;

public class Sum extends Instruction {
    @Override
    public IDataSerializable putData(String prefix, DataHelper data) throws IOException {
        return this;
    }

    @Override
    protected ItemStack getRecipeItem() {
        return new ItemStack(Blocks.iron_bars);
    }
    
    @Override
    protected ItemStack getInstructionPlate() {
        return (new IntegerValue()).toItem();
    }

    @Override
    public void motorHit(ServoMotor motor) {
        ServoStack stack = motor.getArgStack();
        Integer a = stack.popType(Integer.class);
        Integer b = stack.popType(Integer.class);
        if (a == null) a = 0;
        if (b == null) b = 0;
        stack.push(a + b);
    }

    @Override
    public IIcon getIcon(EnumFacing side) {
        return BlockIcons.servo$sum;
    }

    @Override
    public String getName() {
        return "fz.instruction.sum";
    }
    
}
