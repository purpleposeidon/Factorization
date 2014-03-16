package factorization.servo.instructions;

import java.io.IOException;
import java.util.Arrays;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;
import factorization.api.Coord;
import factorization.api.FzColor;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;
import factorization.common.BlockIcons;
import factorization.servo.CpuBlocking;
import factorization.servo.Executioner;
import factorization.servo.Instruction;
import factorization.servo.ServoMotor;
import factorization.servo.ServoStack;

public class ValueTransfer extends Instruction {
    private static final byte MOVE_VALUE = 0, TAKE_VALUE = 1, MOVE_STACK = 2, TAKE_STACK = 3, ACTION_COUNT = 4;
    byte action = MOVE_VALUE;
    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        action = data.as(Share.VISIBLE, "action").putByte(action);
        return this;
    }

    @Override
    protected ItemStack getRecipeItem() {
        return new ItemStack(Blocks.hopper);
    }

    @Override
    public void motorHit(ServoMotor motor) {
        final ServoStack io = motor.getServoStack(Executioner.STACK_IO);
        final FzColor color = motor.getArgStack().popType(FzColor.class);
        if (color == null) {
            motor.putError("No color");
            return;
        }
        int sid = color.ordinal();
        if (sid == Executioner.STACK_IO) {
            return; // Hmm?
        }
        final ServoStack target = motor.getServoStack(sid);
        final ServoStack src, dst;
        if (action == MOVE_VALUE || action == MOVE_STACK) {
            src = target;
            dst = io;
        } else {
            src = io;
            dst = target;
        }
        Object o;
        switch (action) {
        default:
        case MOVE_VALUE:
        case TAKE_VALUE:
            o = src.pop();
            if (o == null) {
                motor.putError("Stack Underflow");
                return;
            }
            dst.push(o);
            break;
        case MOVE_STACK:
        case TAKE_STACK:
            for (Object val : src) {
                dst.push(val);
            }
            src.clear();
            break;
        }
    }

    @Override
    public IIcon getIcon(ForgeDirection side) {
        switch (action) {
        default:
        case MOVE_VALUE: return BlockIcons.servo$move_value;
        case TAKE_VALUE: return BlockIcons.servo$take_value;
        case MOVE_STACK: return BlockIcons.servo$move_stack;
        case TAKE_STACK: return BlockIcons.servo$take_stack;
        }
    }
    
    @Override
    public String getInfo() {
        switch (action) {
        default:
        case MOVE_VALUE: return "Push to IO";
        case TAKE_VALUE: return "Pop from IO";
        case MOVE_STACK: return "Move stack to IO";
        case TAKE_STACK: return "Take IO stack";
        }
    }

    @Override
    public String getName() {
        return "fz.instruction.valuetransfer";
    }
    
    @Override
    public CpuBlocking getBlockingBehavior() {
        return CpuBlocking.BLOCK_FOR_TICK;
    }
    
    @Override
    public boolean onClick(EntityPlayer player, Coord block, ForgeDirection side) {
        if (!playerHasProgrammer(player)) return false;
        action++;
        if (action == ACTION_COUNT) action = 0;
        return true;
    }
}
