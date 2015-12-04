package factorization.servo.instructions;

import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.common.BlockIcons;
import factorization.servo.CpuBlocking;
import factorization.servo.EntryAction;
import factorization.servo.Instruction;
import factorization.servo.ServoMotor;
import factorization.util.FzUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IIcon;

import java.io.IOException;

public class SetEntryAction extends Instruction {
    EntryAction mode = EntryAction.ENTRY_EXECUTE;
    
    @Override
    public IDataSerializable putData(String prefix, DataHelper data) throws IOException {
        mode = data.asSameShare("mode").putEnum(mode);
        return this;
    }

    @Override
    protected ItemStack getRecipeItem() {
        return new ItemStack(Items.writable_book);
    }

    @Override
    public void motorHit(ServoMotor motor) { } //servomotor is hard-coded to call the preHit first.
    
    @Override
    public boolean preMotorHit(ServoMotor motor) {
        if (mode == EntryAction.ENTRY_WRITE) {
            if (motor.getArgStack().getSize() > 0) {
                motor.executioner.entry_action = mode;
            } else {
                motor.executioner.entry_action = EntryAction.ENTRY_EXECUTE;
            }
        } else {
            motor.executioner.entry_action = mode;
        }
        return true;
    }

    @Override
    public IIcon getIcon(EnumFacing side) {
        switch (mode) {
        default:
        case ENTRY_EXECUTE: return BlockIcons.servo$entry_execute;
        case ENTRY_LOAD: return BlockIcons.servo$entry_load;
        case ENTRY_WRITE: return BlockIcons.servo$entry_write;
        case ENTRY_IGNORE: return BlockIcons.servo$entry_ignore;
        }
    }

    @Override
    public String getName() {
        return "fz.instruction.entryaction";
    }
    
    @Override
    public boolean onClick(EntityPlayer player, Coord block, EnumFacing side) {
        if (!playerHasProgrammer(player)) {
            return false;
        }
        mode = FzUtil.shiftEnum(mode, EntryAction.values(), 1);
        return true;
    }
    
    @Override
    public String getInfo() {
        switch (mode) {
        default:
        case ENTRY_EXECUTE: return "Execute Immediately";
        case ENTRY_LOAD: return "Read to Stack";
        case ENTRY_WRITE: return "Write from Stack";
        case ENTRY_IGNORE: return "Ignore Instructions"; //excepting this one
        }
    }
    
    @Override
    public CpuBlocking getBlockingBehavior() {
        return CpuBlocking.BLOCK_FOR_TICK;
    }
}
