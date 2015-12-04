package factorization.servo.instructions;

import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;
import factorization.common.BlockIcons;
import factorization.servo.Instruction;
import factorization.servo.ServoMotor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IIcon;

import java.io.IOException;

public class BooleanValue extends Instruction {
    boolean val = true;
    @Override
    public IDataSerializable putData(String prefix, DataHelper data) throws IOException {
        val = data.as(Share.VISIBLE, "val").putBoolean(val);
        return this;
    }

    @Override
    protected ItemStack getRecipeItem() {
        return new ItemStack(Blocks.lever);
    }

    @Override
    public void motorHit(ServoMotor motor) {
        motor.getArgStack().push(val);
    }
    
    @Override
    public boolean onClick(EntityPlayer player, Coord block, EnumFacing side) {
        if (playerHasProgrammer(player)) {
            val = !val;
            return true;
        }
        return false;
    }
    
    @Override
    public IIcon getIcon(EnumFacing side) {
        return val ? BlockIcons.servo$true : BlockIcons.servo$false;
    }

    @Override
    public String getName() {
        return "fz.instruction.boolean";
    }
    
    @Override
    public String getInfo() {
        return Boolean.toString(val);
    }
    
}
