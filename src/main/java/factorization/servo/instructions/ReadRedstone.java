package factorization.servo.instructions;

import java.io.IOException;

import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;
import factorization.api.Coord;
import factorization.api.FzOrientation;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.common.BlockIcons;
import factorization.notify.Notice;
import factorization.servo.Instruction;
import factorization.servo.ServoMotor;

public class ReadRedstone extends Instruction {

    @Override
    public IDataSerializable putData(String prefix, DataHelper data) throws IOException {
        return this;
    }

    @Override
    protected ItemStack getRecipeItem() {
        return new ItemStack(Items.redstone);
    }

    @Override
    public void motorHit(ServoMotor motor) {
        Coord at = motor.getCurrentPos().add(motor.getOrientation().top);
        int power = 0;
        if (at.getBlock() instanceof BlockRedstoneWire) {
            power = at.getMd();
        } else {
            power = at.w.getStrongestIndirectPower(at.x, at.y, at.z);
            //power = at.w.getBlockPowerInput(at.x, at.y, at.z);
            //power = at.w.getIndirectPowerLevelTo(at.x, at.y, at.z, motor.getOrientation().top.ordinal());
        }
        motor.getArgStack().push(power);
    }

    @Override
    public IIcon getIcon(ForgeDirection side) {
        return BlockIcons.servo$read_redstone;
    }

    @Override
    public String getName() {
        return "fz.instruction.readredstone";
    }

}
