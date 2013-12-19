package factorization.servo.instructions;

import java.io.IOException;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.common.BlockIcons;
import factorization.servo.Instruction;
import factorization.servo.ServoMotor;

public class IntegerValue extends Instruction {
    int val = 1;

    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        val = data.asSameShare(prefix + "val").put(val);
        return this;
    }

    @Override
    protected ItemStack getRecipeItem() {
        return new ItemStack(Block.fenceIron);
    }

    @Override
    public void motorHit(ServoMotor motor) {
        motor.getServoStack(ServoMotor.STACK_ARGUMENT).push(val);
    }

    @Override
    public Icon getIcon(ForgeDirection side) {
        if (val == 1) {
            return BlockIcons.servo$one;
        } else if (val == 0) {
            return BlockIcons.servo$zero;
        } else {
            return BlockIcons.servo$number;
        }
    }

    @Override
    public String getName() {
        return "fz.instruction.integervalue";
    }
    
    @Override
    public boolean onClick(EntityPlayer player, Coord block, ForgeDirection side) {
        if (playerHasProgrammer(player)) {
            if (val == 0) {
                val = 1;
                return true;
            } else if (val == 1) {
                val = 0;
                return true;
            }
        }
        return false;
    }
    
    @Override
    public String getInfo() {
        return "" + val;
    }
}
