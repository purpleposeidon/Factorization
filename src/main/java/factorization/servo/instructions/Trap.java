package factorization.servo.instructions;

import factorization.servo.CpuBlocking;
import factorization.servo.ServoMotor;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

public class Trap extends SimpleInstruction {
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
    protected String getSimpleName() {
        return "trap";
    }

    @Override
    public CpuBlocking getBlockingBehavior() {
        return CpuBlocking.BLOCK_UNTIL_NEXT_ENTRY;
    }
}
