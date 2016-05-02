package factorization.servo.instructions;

import factorization.servo.iterator.ServoMotor;
import factorization.shared.Core;

public class SetRepeatedInstruction extends SimpleInstruction {
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
    protected String getSimpleName() {
        return "repeatedInstruction";
    }

}
