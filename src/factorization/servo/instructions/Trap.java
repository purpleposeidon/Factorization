package factorization.servo.instructions;

import java.io.IOException;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.common.BlockIcons;
import factorization.servo.Instruction;
import factorization.servo.ServoMotor;

public class Trap extends Instruction {

    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        return this;
    }

    @Override
    protected ItemStack getRecipeItem() {
        return new ItemStack(Block.chestTrapped);
    }

    @Override
    public void motorHit(ServoMotor motor) {
        if (!motor.getCurrentPos().isWeaklyPowered()) {
            motor.setStopped(true);
        }
    }

    @Override
    public Icon getIcon(ForgeDirection side) {
        return BlockIcons.servo$trap;
    }

    @Override
    public String getName() {
        return "fz.instruction.trap";
    }

}
