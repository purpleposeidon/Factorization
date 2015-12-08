package factorization.servo.instructions;

import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;
import factorization.servo.AbstractServoMachine;
import factorization.servo.Instruction;
import factorization.servo.ServoMotor;
import factorization.servo.stepper.StepperEngine;
import factorization.util.SpaceUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

import java.io.IOException;

public class SetDirection extends Instruction {
    EnumFacing dir = EnumFacing.UP;

    @Override
    public boolean onClick(EntityPlayer player, Coord block, EnumFacing side) {
        if (playerHasProgrammer(player)) {
            int i = dir.ordinal();
            dir = SpaceUtil.getOrientation((i + 1) % 6);
            return true;
        }
        return false;
    }

    void hit(AbstractServoMachine motor) {
        EnumFacing d = dir.getOpposite();
        motor.setNextDirection(d);
        if (d == motor.getOrientation().facing.getOpposite()) {
            motor.changeOrientation(d);
        }
    }

    @Override
    public void motorHit(ServoMotor motor) {
        hit(motor);
    }

    @Override
    public void stepperHit(StepperEngine engine) {
        hit(engine);
    }

    @Override
    public String getName() {
        return "fz.instruction.setdirection";
    }
    
    @Override
    public IDataSerializable putData(String prefix, DataHelper data) throws IOException {
        dir = data.as(Share.MUTABLE, "dir").putEnum(dir);
        return this;
    }
    
    @Override
    protected Object getRecipeItem() {
        return new ItemStack(Items.arrow);
    }

}
