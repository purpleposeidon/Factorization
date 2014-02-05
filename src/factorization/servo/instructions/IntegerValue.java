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
import factorization.servo.Executioner;
import factorization.servo.Instruction;
import factorization.servo.ServoMotor;

public class IntegerValue extends Instruction {
    private int val = 1;

    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        setVal(data.asSameShare(prefix + "val").put(getVal()));
        return this;
    }

    @Override
    protected ItemStack getRecipeItem() {
        return new ItemStack(Block.fenceIron);
    }

    @Override
    public void motorHit(ServoMotor motor) {
        motor.getArgStack().push(getVal());
    }

    @Override
    public Icon getIcon(ForgeDirection side) {
        if (getVal() == 1) {
            return BlockIcons.servo$one;
        } else if (getVal() == 0) {
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
            if (getVal() == 0) {
                setVal(1);
                return true;
            } else if (getVal() == 1) {
                setVal(0);
                return true;
            }
        }
        return false;
    }
    
    @Override
    public String getInfo() {
        return "" + getVal();
    }

    public int getVal() {
        return val;
    }

    public void setVal(int val) {
        this.val = val;
    }
}
