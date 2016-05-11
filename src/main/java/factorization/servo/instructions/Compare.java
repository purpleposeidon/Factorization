package factorization.servo.instructions;

import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;
import factorization.flat.api.IFlatModel;
import factorization.flat.api.IModelMaker;
import factorization.servo.iterator.ServoMotor;
import factorization.servo.iterator.ServoStack;
import factorization.servo.rail.Instruction;
import factorization.util.FzUtil;
import net.minecraft.util.EnumFacing;

import java.io.IOException;
import java.util.Locale;

public class Compare extends Instruction {
    static enum CmpType {
        LT, LE, EQ, NE, GE, GT;

        boolean apply(Comparable a, Comparable b) {
            @SuppressWarnings("unchecked")
            int cmp = (int) Math.signum(a.compareTo(b));
            switch (this) {
            default:
            case EQ: return cmp == 0;
            case NE: return cmp != 0;
            case GE: return cmp >= 0;
            case GT: return cmp > 0;
            case LE: return cmp <= 0;
            case LT: return cmp < 0;
            }
        }
    }
    
    CmpType cmp = CmpType.EQ;
    
    @Override
    public IDataSerializable putData(String prefix, DataHelper data) throws IOException {
        cmp = data.as(Share.VISIBLE, "cmp").putEnum(cmp);
        return this;
    }

    @Override
    protected Object getRecipeItem() {
        return "gemQuartz";
    }

    @Override
    public void motorHit(ServoMotor motor) {
        ServoStack ss = motor.getArgStack();
        Object a = ss.pop();
        if (a == null) {
            motor.putError("CMP: Stack underflow");
            return;
        }
        Object b = ss.popType(a.getClass());
        if (b == null) {
            motor.putError("CMP: Stack underflow of type: " + a.getClass());
            return;
        }
        if (!(a instanceof Comparable)) {
            motor.putError("CMP: Not Comparable: " + a.getClass());
            return;
        }
        ss.push(cmp.apply((Comparable)a, (Comparable)b));
    }

    @Override
    public String getName() {
        return "fz.instruction.cmp";
    }

    static IFlatModel models[] = new IFlatModel[CmpType.values().length];
    @Override
    public IFlatModel getModel(Coord at, EnumFacing side) {
        return models[cmp.ordinal()];
    }

    @Override
    protected void loadModels(IModelMaker maker) {
        for (CmpType ct : CmpType.values()) {
            models[ct.ordinal()] = reg(maker, "cmp/" + ct.toString().toLowerCase(Locale.ROOT));
        }
    }

    @Override
    public String getInfo() {
        return null;
        //return "" + cmp.toString();
    }
    
    @Override
    protected boolean lmpConfigure() {
        cmp = FzUtil.shiftEnum(cmp, CmpType.values(), 1);
        return true;
    }
}
