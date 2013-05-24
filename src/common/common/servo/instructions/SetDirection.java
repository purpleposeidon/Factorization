package factorization.common.servo.instructions;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Coord;
import factorization.api.FzOrientation;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.common.BlockIcons;
import factorization.common.FactorizationUtil;
import factorization.common.servo.Instruction;
import factorization.common.servo.ServoMotor;
import factorization.common.servo.ServoStack;

public class SetDirection extends Instruction {
    ForgeDirection dir = ForgeDirection.UP;
    
    @Override
    public Icon getIcon(ForgeDirection side) {
        return BlockIcons.arrow_direction.get(dir, side);
    }

    @Override
    public void motorHit(ServoMotor motor) {
        if (motor.orientation.facing != dir) {
            FzOrientation newOrientation = FzOrientation.fromDirection(dir);
            FzOrientation test = newOrientation.pointTopTo(motor.orientation.top);
            if (test != FzOrientation.UNKNOWN) {
                newOrientation = test;
            } else {
                test = newOrientation.pointTopTo(motor.orientation.facing);
                if (test != FzOrientation.UNKNOWN) {
                    newOrientation = test;
                }
            }
            motor.orientation = newOrientation;
        }
    }

    @Override
    public String getName() {
        return "fz.instruction.setdirection";
    }

    @Override
    protected void putData(DataHelper data) throws IOException {
        dir = data.as(Share.VISIBLE, "dir").putEnum(dir);
    }

    @Override
    public boolean configure(ServoStack stack) {
        Iterator<Object> it = stack.iterator();
        while (it.hasNext()) {
            Object o = it.next();
            if (o instanceof ForgeDirection) {
                it.remove();
                dir = (ForgeDirection) o;
                return true;
            }
        }
        return false;
    }
    
    @Override
    public void deconfigure(List<Object> stack) {
        stack.add(dir);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderStatic(Coord where, RenderBlocks rb) {
        if (where != null) {
            BlockIcons.arrow_direction.setRotations(dir, rb);
            super.renderStatic(where, rb);
            BlockIcons.arrow_direction.unsetRotations(rb);
        } else {
            super.renderStatic(where, rb);
        }
    }

}
