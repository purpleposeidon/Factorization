package factorization.servo.instructions;

import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.flat.api.IFlatModel;
import factorization.flat.api.IModelMaker;
import factorization.servo.CpuBlocking;
import factorization.servo.EntryAction;
import factorization.servo.Instruction;
import factorization.servo.ServoMotor;
import factorization.util.FzUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

import java.io.IOException;
import java.util.Locale;

public class SetEntryAction extends Instruction {
    EntryAction mode = EntryAction.ENTRY_EXECUTE;
    
    @Override
    public IDataSerializable putData(String prefix, DataHelper data) throws IOException {
        mode = data.asSameShare("mode").putEnum(mode);
        return this;
    }

    @Override
    protected Object getRecipeItem() {
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
    public String getName() {
        return "fz.instruction.entryaction";
    }

    static IFlatModel[] models = new IFlatModel[EntryAction.values().length];

    @Override
    public IFlatModel getModel(Coord at, EnumFacing side) {
        return models[mode.ordinal()];
    }

    @Override
    protected void loadModels(IModelMaker maker) {
        for (EntryAction action : EntryAction.values()) {
            models[action.ordinal()] = reg(maker, "entryaction/" + action.toString().toLowerCase(Locale.ROOT));
        }
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
