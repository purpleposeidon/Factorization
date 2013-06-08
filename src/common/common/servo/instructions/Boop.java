package factorization.common.servo.instructions;

import java.io.IOException;

import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.common.BlockIcons;
import factorization.common.Core;
import factorization.common.servo.Instruction;
import factorization.common.servo.ServoMotor;

public class Boop extends Instruction {
    @Override
    public Icon getIcon(ForgeDirection side) {
        // TODO Auto-generated method stub
        return BlockIcons.battery_top;
    }

    @Override
    public void motorHit(ServoMotor motor) {
        Core.notify(null, motor.getCurrentPos(), "Boop");
    }

    @Override
    public String getName() {
        return "fz.instruction.boop";
    }
    
    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException { return this; }
}
