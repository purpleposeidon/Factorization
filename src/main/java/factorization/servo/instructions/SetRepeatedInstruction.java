package factorization.servo.instructions;

import java.io.IOException;

import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.EnumFacing;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.common.BlockIcons;
import factorization.servo.Instruction;
import factorization.servo.ServoMotor;
import factorization.shared.Core;

public class SetRepeatedInstruction extends Instruction {

    @Override
    public IDataSerializable putData(String prefix, DataHelper data) throws IOException {
        return this;
    }

    @Override
    protected ItemStack getRecipeItem() {
        return Core.registry.dark_iron_sprocket.copy();
    }

    @Override
    public void motorHit(ServoMotor motor) {
        Object obj = motor.getArgStack().pop();
        if (obj instanceof SetRepeatedInstruction) {
            obj = null;
        }
        motor.executioner.setEntryInstruction(obj);
    }

    @Override
    public IIcon getIcon(EnumFacing side) {
        return BlockIcons.servo$repeated_instruction;
    }

    @Override
    public String getName() {
        return "fz.instruction.repeatedInstruction";
    }

}
