package factorization.servo.instructions;

import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;
import factorization.flat.api.IFlatModel;
import factorization.flat.api.IModelMaker;
import factorization.servo.rail.Instruction;
import factorization.servo.iterator.ServoMotor;
import factorization.servo.iterator.ServoStack;
import factorization.sockets.SocketShifter;
import factorization.sockets.TileEntitySocketBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

import java.io.IOException;
import java.util.Locale;

import static factorization.servo.instructions.ShifterControl.ShifterModes.EXPORT_MODE;
import static factorization.servo.instructions.ShifterControl.ShifterModes.TRANSFER_LIMIT;

public class ShifterControl extends Instruction {
    enum ShifterModes {
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
    public IDataSerializable putData(String prefix, DataHelper data) throws IOException {
        mode = ShifterModes.values[data.as(Share.VISIBLE, "mode").putByte((byte) mode.ordinal())];
        return this;
    }

    @Override
    protected Object getRecipeItem() {
        return new ItemStack(Blocks.hopper);
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
    public boolean onClick(EntityPlayer player, Coord block, EnumFacing side) {
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
    public String getName() {
        return "fz.instruction.shifterctrl";
    }

    static IFlatModel[] model = new IFlatModel[ShifterModes.values().length];
    @Override
    public IFlatModel getModel(Coord at, EnumFacing side) {
        return model[mode.ordinal()];
    }

    @Override
    protected void loadModels(IModelMaker maker) {
        for (ShifterModes m : ShifterModes.values()) {
            model[m.ordinal()] = reg(maker, "shifterctrl/" + m.toString().toLowerCase(Locale.ROOT));
        }
    }

}
