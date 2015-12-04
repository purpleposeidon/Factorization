package factorization.servo.instructions;

import java.io.IOException;

import factorization.servo.AbstractServoMachine;
import factorization.servo.stepper.StepperEngine;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.EnumFacing;
import factorization.api.Coord;
import factorization.api.FzOrientation;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;
import factorization.common.BlockIcons;
import factorization.servo.CpuBlocking;
import factorization.servo.Instruction;
import factorization.servo.ServoMotor;
import factorization.shared.Core;

public class RotateTop extends Instruction {
    EnumFacing top = EnumFacing.UP;
    
    @Override
    public IIcon getIcon(EnumFacing side) {
        if (side == null) {
            return BlockIcons.servo$set_facing.side_W;
        }
        return BlockIcons.servo$set_facing.get(top.getOpposite(), side);
    }
    
    @Override
    public boolean onClick(EntityPlayer player, Coord block, EnumFacing side) {
        if (playerHasProgrammer(player)) {
            int i = top.ordinal();
            top = SpaceUtil.getOrientation((i + 1) % 6);
            return true;
        }
        return false;
    }

    void hit(AbstractServoMachine motor) {
        FzOrientation o = motor.getOrientation().pointTopTo(top.getOpposite());
        if (o != FzOrientation.UNKNOWN) {
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
    protected ItemStack getRecipeItem() {
        return new ItemStack(Core.registry.fan);
    }
    
    @Override
    public CpuBlocking getBlockingBehavior() {
        return CpuBlocking.BLOCK_UNTIL_NEXT_ENTRY;
    }
}
