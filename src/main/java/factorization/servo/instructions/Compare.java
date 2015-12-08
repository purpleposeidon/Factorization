package factorization.servo.instructions;

import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;
import factorization.servo.Instruction;
import factorization.servo.ServoMotor;
import factorization.servo.ServoStack;
import factorization.util.FzUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

import java.io.IOException;

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
        return new ItemStack(Items.quartz);
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

    @Override
    public String getInfo() {
        return null;
        //return "" + cmp.toString();
    }
    
    @Override
    public boolean onClick(EntityPlayer player, Coord block, EnumFacing side) {
        if (!playerHasProgrammer(player)) {
            return false;
        }
        cmp = FzUtil.shiftEnum(cmp, CmpType.values(), 1);
        return true;
    }
}
