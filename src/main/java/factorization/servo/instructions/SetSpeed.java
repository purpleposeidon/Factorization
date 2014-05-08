package factorization.servo.instructions;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;
import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.common.BlockIcons;
import factorization.servo.CpuBlocking;
import factorization.servo.Instruction;
import factorization.servo.ServoMotor;

public class SetSpeed extends Instruction {
    byte speed = 3;
    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        speed = data.putByte(speed);
        return this;
    }

    @Override
    protected ItemStack getRecipeItem() {
        return new ItemStack(Items.sugar);
    }

    @Override
    public void motorHit(ServoMotor motor) {
        if (motor.getTargetSpeed() == speed) return;
        motor.setTargetSpeed(speed);
        motor.markDirty();
    }

    @Override
    public IIcon getIcon(ForgeDirection side) {
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
    
    @Override
    public CpuBlocking getBlockingBehavior() {
        return CpuBlocking.BLOCK_FOR_TICK;
    }
}
