package factorization.common.servo.instructions;

import java.io.IOException;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Coord;
import factorization.api.FzOrientation;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;
import factorization.common.BlockIcons;
import factorization.common.servo.Instruction;
import factorization.common.servo.ServoMotor;

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
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        dir = data.as(Share.MUTABLE_INDIRECT, "dir").putEnum(dir);
        return this;
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
