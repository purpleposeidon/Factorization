package factorization.servo.instructions;

import factorization.api.Coord;
import factorization.api.FzOrientation;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;
import factorization.servo.AbstractServoMachine;
import factorization.servo.CpuBlocking;
import factorization.servo.Instruction;
import factorization.servo.ServoMotor;
import factorization.servo.stepper.StepperEngine;
import factorization.shared.Core;
import factorization.util.FzUtil;
import factorization.util.SpaceUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

import java.io.IOException;

public class RotateTop extends Instruction {
    EnumFacing top = EnumFacing.UP;

    @Override
    public boolean onClick(EntityPlayer player, Coord block, EnumFacing side) {
        if (playerHasProgrammer(player)) {
            top = FzUtil.shiftEnum(top, EnumFacing.VALUES, 1);
            return true;
        }
        return false;
    }

    void hit(AbstractServoMachine motor) {
        FzOrientation o = motor.getOrientation().pointTopTo(top.getOpposite());
        if (o != null) {
            motor.setOrientation(o);
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
        return "fz.instruction.rotatetop";
    }
    
    @Override
    public IDataSerializable putData(String prefix, DataHelper data) throws IOException {
        top = data.as(Share.MUTABLE, "top").putEnum(top);
        return this;
    }
    
    @Override
    protected Object getRecipeItem() {
        return new ItemStack(Core.registry.fan);
    }
    
    @Override
    public CpuBlocking getBlockingBehavior() {
        return CpuBlocking.BLOCK_UNTIL_NEXT_ENTRY;
    }
}
