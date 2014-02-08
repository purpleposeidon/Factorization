package factorization.servo.instructions;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.common.BlockIcons;
import factorization.servo.CpuBlocking;
import factorization.servo.EntryAction;
import factorization.servo.Executioner;
import factorization.servo.Instruction;
import factorization.servo.ServoMotor;

public class SetEntryAction extends Instruction {
    EntryAction mode = EntryAction.ENTRY_EXECUTE;
    
    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        mode = data.asSameShare("mode").putEnum(mode);
        return this;
    }

    @Override
    protected ItemStack getRecipeItem() {
        return new ItemStack(Item.writableBook);
    }

    @Override
    public void motorHit(ServoMotor motor) { } //servomotor is hard-coded to call the preHit first.
    
    @Override
    public boolean preMotorHit(ServoMotor motor) {
        if (mode == EntryAction.ENTRY_WRITE) {
            if (motor.getServoStack(Executioner.STACK_IO).getSize() > 0) {
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
    public Icon getIcon(ForgeDirection side) {
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
    public boolean onClick(EntityPlayer player, Coord block, ForgeDirection side) {
        if (!playerHasProgrammer(player)) {
            return false;
        }
        int i = mode.ordinal() + 1;
        EntryAction values[] = mode.values();
        if (i >= values.length) {
            i = 0;
        }
        mode = values[i];
        return true;
    }
    
    @Override
    public String getInfo() {
        switch (mode) {
        default:
        case ENTRY_EXECUTE: return "Execute Immediately";
        case ENTRY_LOAD: return "Read to IO Stack";
        case ENTRY_WRITE: return "Write from IO Stack";
        case ENTRY_IGNORE: return "Ignore Instructions"; //excepting this one
        }
    }
    
    @Override
    public CpuBlocking getBlockingBehavior() {
        return CpuBlocking.BLOCK_FOR_TICK;
    }
}
