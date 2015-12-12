package factorization.servo.instructions;

import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.servo.CpuBlocking;
import factorization.servo.Instruction;
import factorization.servo.ServoMotor;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import java.io.IOException;

public class Trap extends Instruction {

    @Override
    public IDataSerializable putData(String prefix, DataHelper data) throws IOException {
        return this;
    }

    @Override
    protected Object getRecipeItem() {
        return new ItemStack(Blocks.trapped_chest);
    }

    @Override
    public void motorHit(ServoMotor motor) {
        if (motor.isStopped()) {
            motor.setStopped(false);
            return;
        }
        if (!motor.getCurrentPos().isWeaklyPowered()) {
            motor.setStopped(true);
        }
    }

    @Override
    public String getName() {
        return "fz.instruction.trap";
    }
    
    @Override
    public CpuBlocking getBlockingBehavior() {
        return CpuBlocking.BLOCK_UNTIL_NEXT_ENTRY;
    }
}
