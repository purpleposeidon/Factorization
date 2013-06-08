package factorization.common.servo.instructions;

import java.io.IOException;

import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.common.BlockIcons;
import factorization.common.servo.Instruction;
import factorization.common.servo.ServoMotor;

public class ActivateActuator extends Instruction {

    @Override
    public Icon getIcon(ForgeDirection side) {
        return BlockIcons.servo$activate;
    }

    @Override
    public void motorHit(ServoMotor motor) {
        if (motor.getActuator() != null) {
            motor.getActuator().onUse(motor, false);
        }
    }

    @Override
    public String getName() {
        return "fz.instruction.activateactuator";
    }

    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        return this;
    }
}
