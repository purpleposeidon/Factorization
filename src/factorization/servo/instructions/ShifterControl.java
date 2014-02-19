package factorization.servo.instructions;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;
import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;
import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.servo.Instruction;
import factorization.servo.ServoMotor;
import factorization.servo.ServoStack;
import factorization.sockets.SocketShifter;
import factorization.sockets.TileEntitySocketBase;
import static factorization.servo.instructions.ShifterControl.ShifterModes.*;

public class ShifterControl extends Instruction {
    static enum ShifterModes {
        EXPORT_MODE,
        IMPORT_MODE,
        TRANSFER_LIMIT,
        TARGET_SLOT,
        STREAM,
        PULSE_EXACT,
        PULSE_SOME,
        PROBE;
        static final ShifterModes[] values = values();
    }
    ShifterModes mode = ShifterModes.EXPORT_MODE;
    
    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        mode = ShifterModes.values[data.as(Share.VISIBLE, "mode").putByte((byte) mode.ordinal())];
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
        case STREAM: shifter.mode = SocketShifter.ShifterMode.MODE_STREAM; return;
        case PULSE_EXACT: shifter.mode = SocketShifter.ShifterMode.MODE_PULSE_EXACT; return;
        case PULSE_SOME: shifter.mode = SocketShifter.ShifterMode.MODE_PULSE_SOME; return;
        case TRANSFER_LIMIT:
        case TARGET_SLOT:
            ServoStack ss = motor.getArgStack();
            Integer the_val = ss.popType(Integer.class);
            if (the_val == null) {
                motor.putError("Stack underflow");
                return;
            }
            int val = the_val;
            if (mode == TRANSFER_LIMIT) {
                shifter.transferLimit = (byte) val;
            } else {
                shifter.foreignSlot = val;
            }
            shifter.sanitize(motor);
            return;
        case PROBE:
            shifter.probe(motor);
            return;
        }
    }

    @Override
    public boolean onClick(EntityPlayer player, Coord block, ForgeDirection side) {
        if (!playerHasProgrammer(player)) {
            return false;
        }
        int i = mode.ordinal();
        i++;
        if (i == ShifterModes.values.length) {
            i = 0;
        }
        mode = ShifterModes.values[i];
        return true;
    }
    
    @Override
    public String getInfo() {
        switch (mode) {
        default:
        case EXPORT_MODE: return "Export Items";
        case IMPORT_MODE: return "Import Items";
        case TRANSFER_LIMIT: return "Set Transfer Limit";
        case TARGET_SLOT: return "Set Target Slot";
        case STREAM: return "Stream Transfer";
        case PULSE_EXACT: return "Pulse Transfer Exact";
        case PULSE_SOME: return "Pulse Transfer Some";
        case PROBE: return "Probe";
        }
    }
    
    @Override
    public IIcon getIIcon(ForgeDirection side) {
        switch (mode) {
        default:
        case EXPORT_MODE: return BlockIcons.servo$ctrl$shift_export;
        case IMPORT_MODE: return BlockIcons.servo$ctrl$shift_import;
        case TRANSFER_LIMIT: return BlockIcons.servo$ctrl$shift_transfer_limit;
        case TARGET_SLOT: return BlockIcons.servo$ctrl$shift_target_slot;
        case PULSE_EXACT: return BlockIcons.servo$ctrl$shift_pulse_exact;
        case PULSE_SOME: return BlockIcons.servo$ctrl$shift_pulse_some;
        case STREAM: return BlockIcons.servo$ctrl$shift_stream;
        case PROBE: return BlockIcons.servo$ctrl$shift_probe;
        }
    }

    @Override
    public String getName() {
        return "fz.instruction.shifterctrl";
    }

}
