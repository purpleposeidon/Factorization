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

public class Jump extends Instruction {

    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        return this;
    }

    @Override
    protected ItemStack getRecipeItem() {
        return new ItemStack(Block.pistonBase);
    }

    @Override
    public void motorHit(ServoMotor motor) {
        Boolean b = motor.getServoStack(ServoMotor.STACK_ARGUMENT).popType(Boolean.class);
        if (b == null) {
            motor.putError("Jump: Stack Underflow of Boolean");
            return;
        }
        motor.skipNextInstruction = b;
        motor.penalizeSpeed();
        motor.desync(true);
    }

    @Override
    public Icon getIcon(ForgeDirection side) {
        return BlockIcons.servo$jmp;
    }

    @Override
    public String getName() {
        return "fz.instruction.jmp";
    }

}
