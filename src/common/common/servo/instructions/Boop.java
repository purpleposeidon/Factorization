package factorization.common.servo.instructions;

import java.io.IOException;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.common.Core;
import factorization.common.servo.Instruction;
import factorization.common.servo.ServoMotor;

public class Boop extends Instruction {
    @Override
    public Icon getIcon(ForgeDirection side) {
        return Block.music.getBlockTextureFromSide(0);
    }

    @Override
    public void motorHit(ServoMotor motor) {
        Core.notify(null, motor.getCurrentPos(), "Boop");
    }
    
    @Override
    public String getInfo() {
        return "Boop?";
    }

    @Override
    public String getName() {
        return "fz.instruction.boop";
    }
    
    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException { return this; }
    
    @Override
    protected ItemStack getRecipeItem() {
        return new ItemStack(Block.music);
    }
}
