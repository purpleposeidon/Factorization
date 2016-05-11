package factorization.servo.instructions;

import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.flat.api.IFlatModel;
import factorization.flat.api.IModelMaker;
import factorization.servo.iterator.ServoMotor;
import factorization.servo.rail.Instruction;
import net.minecraft.util.EnumFacing;

import java.io.IOException;

public class EntryControl extends Instruction {
    public boolean blocking = false;
    
    @Override
    public IDataSerializable putData(String prefix, DataHelper data) throws IOException {
        blocking = data.asSameShare("block").putBoolean(blocking);
        return this;
    }
    
    @Override
    protected boolean lmpConfigure() {
        blocking = !blocking;
        return true;
    }

    @Override
    protected Object getRecipeItem() {
        return "fenceGateWood";
    }

    @Override
    public void motorHit(ServoMotor motor) { }

    @Override
    public String getName() {
        return "fz.instruction.entryControl";
    }

    static IFlatModel yes, no;
    @Override
    public IFlatModel getModel(Coord at, EnumFacing side) {
        return blocking ? no : yes;
    }

    @Override
    protected void loadModels(IModelMaker maker) {
        yes = reg(maker, "entry_control/yes");
        no = reg(maker, "entry_control/no");
    }

    @Override
    public byte getPriority() {
        return (byte) (blocking ? -1 : +1);
    }
}
