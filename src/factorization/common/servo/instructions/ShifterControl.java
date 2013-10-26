package factorization.common.servo.instructions;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;
import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.common.TileEntitySocketBase;
import factorization.common.servo.Instruction;
import factorization.common.servo.ServoMotor;
import factorization.common.servo.ServoStack;
import factorization.common.sockets.SocketShifter;

public class ShifterControl extends Instruction {
    static final byte EXPORT_MODE = 0, IMPORT_MODE = 1, TRANSFER_LIMIT = 2, TARGET_SLOT = 3, PULSE = 4, STREAM = 5;
    static final byte MODE_MAX = STREAM + 1;
    byte mode = EXPORT_MODE;
    
    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        mode = data.as(Share.VISIBLE, "mode").putByte(mode);
        return this;
    }

    @Override
    protected ItemStack getRecipeItem() {
        return FactoryType.SOCKET_SHIFTER.asSocketItem();
    }

    @Override
    public void motorHit(ServoMotor motor) {
        TileEntitySocketBase socket = motor.socket;
        if (!(socket instanceof SocketShifter)) {
            motor.putError("Socket is not an Item Shifter");
            return;
        }
        SocketShifter shifter = (SocketShifter) socket;
        switch (mode) {
        default:
        case EXPORT_MODE:
        case IMPORT_MODE:
            shifter.exporting = mode == EXPORT_MODE;
            return;
        case PULSE:
        case STREAM:
            shifter.streamMode = mode == STREAM;
            return;
        case TRANSFER_LIMIT:
        case TARGET_SLOT:
            ServoStack ss = motor.getServoStack(ServoMotor.STACK_ARGUMENT);
            Integer the_val = ss.popType(Integer.class);
            if (the_val == null) {
                motor.putError("Stack underflow");
                return;
            }
            int val = the_val;
            if (mode == TRANSFER_LIMIT) {
                shifter.transferLimit = (byte) val;
            } else {
                shifter.foreignSlot = (byte) val;
            }
            shifter.sanitize(motor);
            return;
        }
    }

    @Override
    public boolean onClick(EntityPlayer player, Coord block, ForgeDirection side) {
        if (!playerHasProgrammer(player)) {
            return false;
        }
        mode++;
        if (mode == MODE_MAX) {
            mode = 0;
        }
        return true;
    }
    
    @Override
    public String getInfo() {
        switch (mode) {
        default:
        case EXPORT_MODE: return "Export Items";
        case IMPORT_MODE: return "Import Items";
        case TRANSFER_LIMIT: return "Set Transfer Timit";
        case TARGET_SLOT: return "Set Target Slot";
        case STREAM: return "Streaming Transfer";
        case PULSE: return "Pulsed Transfer";
        }
    }
    
    @Override
    public Icon getIcon(ForgeDirection side) {
        switch (mode) {
        default:
        case EXPORT_MODE: return BlockIcons.servo$ctrl$shift_export;
        case IMPORT_MODE: return BlockIcons.servo$ctrl$shift_import;
        case TRANSFER_LIMIT: return BlockIcons.servo$ctrl$shift_transfer_limit;
        case TARGET_SLOT: return BlockIcons.servo$ctrl$shift_target_slot;
        case STREAM: return BlockIcons.servo$ctrl$shift_stream;
        case PULSE: return BlockIcons.servo$ctrl$shift_pulse;
        }
    }

    @Override
    public String getName() {
        return "fz.instruction.shifterctrl";
    }

}
