package factorization.servo.instructions;

import factorization.api.Coord;
import factorization.servo.iterator.ServoMotor;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

public class ReadRedstone extends SimpleInstruction {
    @Override
    protected Object getRecipeItem() {
        return new ItemStack(Items.redstone);
    }

    @Override
    public void motorHit(ServoMotor motor) {
        Coord at = motor.getCurrentPos().add(motor.getOrientation().top);
        int power;
        IBlockState bs = at.getState();
        if (bs.getBlock() instanceof BlockRedstoneWire) {
            power = bs.getValue(BlockRedstoneWire.POWER);
        } else {
            power = at.w.isBlockIndirectlyGettingPowered(at.toBlockPos());
            //power = at.w.getBlockPowerInput(at.x, at.y, at.z);
            //power = at.w.getIndirectPowerLevelTo(at.x, at.y, at.z, motor.getOrientation().top.ordinal());
        }
        motor.getArgStack().push(power);
    }

    @Override
    protected String getSimpleName() {
        return "readredstone";
    }

}
