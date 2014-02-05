package factorization.servo.instructions;

import java.io.IOException;
import java.util.Arrays;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.FzColor;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;
import factorization.common.BlockIcons;
import factorization.servo.Instruction;
import factorization.servo.ServoMotor;
import factorization.servo.ServoStack;

public class ValueTransfer extends Instruction {
    private static final byte MOVE_VALUE = 0, TAKE_VALUE = 1, MOVE_STACK = 2, TAKE_STACK = 3;
    byte action = MOVE_VALUE;
    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        data.as(Share.PRIVATE, "action").putByte(action);
        return this;
    }

    @Override
    protected ItemStack getRecipeItem() {
        return new ItemStack(Block.hopperBlock);
    }

    @Override
    public void motorHit(ServoMotor motor) {
        final ServoStack io = motor.getServoStack(motor.STACK_ARGUMENT);
        final FzColor color = io.popType(FzColor.class);
        if (color == null) {
            motor.putError("No color");
            return;
        }
        int sid = color.ordinal();
        if (sid == motor.STACK_IO) {
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
            src.setContentsList(Arrays.asList());
            break;
        }
    }

    @Override
    public Icon getIcon(ForgeDirection side) {
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
        case MOVE_VALUE: return "Pop from IO";
        case TAKE_VALUE: return "Push to IO";
        case MOVE_STACK: return "Bulk move all to IO";
        case TAKE_STACK: return "Bulk take entire IO";
        }
    }

    @Override
    public String getName() {
        return "fz.instructions.valuetransfer";
    }

}
