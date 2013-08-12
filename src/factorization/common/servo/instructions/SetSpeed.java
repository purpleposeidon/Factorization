package factorization.common.servo.instructions;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.common.BlockIcons;
import factorization.common.servo.Instruction;
import factorization.common.servo.ServoMotor;

public class SetSpeed extends Instruction {
    byte speed = 3;
    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        speed = data.putByte(speed);
        return this;
    }

    @Override
    protected ItemStack getRecipeItem() {
        return new ItemStack(Item.sugar);
    }

    @Override
    public void motorHit(ServoMotor motor) {
        motor.target_speed = (byte) (speed - 1);
    }

    @Override
    public Icon getIcon(ForgeDirection side) {
        switch (speed) {
        default:
        case 1: return BlockIcons.servo$speed1;
        case 2: return BlockIcons.servo$speed2;
        case 3: return BlockIcons.servo$speed3;
        case 4: return BlockIcons.servo$speed4;
        case 5: return BlockIcons.servo$speed5;
        }
    }

    @Override
    public String getName() {
        return "fz.instruction.setspeed";
    }
    
    @Override
    public boolean onClick(EntityPlayer player, Coord block, ForgeDirection side) {
        if (!playerHasProgrammer(player)) {
            return false;
        }
        speed++;
        if (speed == 6) {
            speed = 1;
        }
        return true;
    }
    
    @Override
    public String getInfo() {
        switch (speed) {
        default:
        case 1: return "Slowest";
        case 2: return "Slow";
        case 3: return "Normal";
        case 4: return "Fast";
        case 5: return "Faster";
        }
    }

}
