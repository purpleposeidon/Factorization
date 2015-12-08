package factorization.servo.instructions;

import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.servo.Instruction;
import factorization.servo.ServoMotor;
import factorization.shared.Core;

import java.io.IOException;

public class SetRepeatedInstruction extends Instruction {

    @Override
    public IDataSerializable putData(String prefix, DataHelper data) throws IOException {
        return this;
    }

    @Override
    protected Object getRecipeItem() {
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
    public String getName() {
        return "fz.instruction.repeatedInstruction";
    }

}
