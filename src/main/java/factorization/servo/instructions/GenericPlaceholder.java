package factorization.servo.instructions;

import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.flat.api.IFlatModel;
import factorization.flat.api.IModelMaker;
import factorization.servo.rail.Instruction;
import factorization.servo.rail.ServoComponent;
import factorization.servo.iterator.ServoMotor;
import net.minecraft.util.EnumFacing;

import java.io.IOException;

/**
 * Exists only for serialization purposes.
 */
public class GenericPlaceholder extends Instruction {
    public static final GenericPlaceholder INSTANCE = new GenericPlaceholder();
    @Override
    protected Object getRecipeItem() {
        return null;
    }

    @Override
    public void motorHit(ServoMotor motor) {

    }

    @Override
    public String getName() {
        return "fz.instruction.generic";
    }

    @Override
    public IFlatModel getModel(Coord at, EnumFacing side) {
        return null;
    }

    @Override
    protected void loadModels(IModelMaker maker) {

    }

    @Override
    public IDataSerializable putData(String prefix, DataHelper data) throws IOException {
        if (data.isWriter()) {
            return this; // Better not happen!
        }
        // Erm, actually it is this that'd better not happen?
        if (data.isNBT()) {
            return ServoComponent.load(data.getTag());
        }
        return this;
    }
}
