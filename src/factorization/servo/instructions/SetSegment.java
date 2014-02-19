package factorization.servo.instructions;

import java.io.IOException;

import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;
import factorization.api.FzColor;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.common.BlockIcons;
import factorization.servo.Instruction;
import factorization.servo.ServoMotor;
import factorization.shared.Core;

public class SetSegment extends Instruction {

    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        return this;
    }

    @Override
    protected ItemStack getRecipeItem() {
        return Core.registry.dark_iron_sprocket;
    }

    @Override
    public void motorHit(ServoMotor motor) {
        FzColor color = motor.getArgStack().popType(FzColor.class);
        if (color == null) {
            motor.putError("Stack underflow: no color");
            return;
        }
        motor.executioner.seg = (byte) color.ordinal();
    }

    @Override
    public IIcon getIIcon(ForgeDirection side) {
        return BlockIcons.servo$set_segment;
    }

    @Override
    public String getName() {
        return "fz.instruction.setsegment";
    }

}
