package factorization.servo.instructions;

import java.io.IOException;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;
import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;
import factorization.common.BlockIcons;
import factorization.servo.Executioner;
import factorization.servo.Instruction;
import factorization.servo.ServoMotor;

public class BooleanValue extends Instruction {
    boolean val = true;
    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
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
    public boolean onClick(EntityPlayer player, Coord block, ForgeDirection side) {
        if (playerHasProgrammer(player)) {
            val = !val;
            return true;
        }
        return false;
    }
    
    @Override
    public IIcon getIIcon(ForgeDirection side) {
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
