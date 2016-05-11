package factorization.servo.instructions;

import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;
import factorization.flat.api.IFlatModel;
import factorization.flat.api.IModelMaker;
import factorization.servo.iterator.ServoMotor;
import factorization.servo.rail.Instruction;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

import java.io.IOException;

public class BooleanValue extends Instruction {
    boolean val = true;
    @Override
    public IDataSerializable putData(String prefix, DataHelper data) throws IOException {
        val = data.as(Share.VISIBLE, "val").putBoolean(val);
        return this;
    }

    @Override
    protected Object getRecipeItem() {
        return new ItemStack(Blocks.lever);
    }

    @Override
    public void motorHit(ServoMotor motor) {
        motor.getArgStack().push(val);
    }
    
    @Override
    protected boolean lmpConfigure() {
        val = !val;
        return false;
    }

    @Override
    public String getName() {
        return "fz.instruction.boolean";
    }

    static IFlatModel _true, _false;

    @Override
    public IFlatModel getModel(Coord at, EnumFacing side) {
        return val ? _true : _false;
    }

    @Override
    protected void loadModels(IModelMaker maker) {
        _true = reg(maker, "boolean/true");
        _false = reg(maker, "boolean/false");
    }

    @Override
    public String getInfo() {
        return Boolean.toString(val);
    }
    
}
