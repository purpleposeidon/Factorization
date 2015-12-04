package factorization.servo.instructions;

import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.servo.Instruction;
import factorization.servo.ServoComponent;
import factorization.servo.ServoMotor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IIcon;
import net.minecraft.util.EnumFacing;

import java.io.IOException;

/**
 * Exists only for serialization purposes.
 */
public class GenericPlaceholder extends Instruction {
    @Override
    protected ItemStack getRecipeItem() {
        return null;
    }

    @Override
    public void motorHit(ServoMotor motor) {

    }

    @Override
    public IIcon getIcon(EnumFacing side) {
        return null;
    }

    @Override
    public String getName() {
        return "fz.instruction.generic";
    }

    @Override
    public IDataSerializable putData(String prefix, DataHelper data) throws IOException {
        if (data.isWriter()) {
            return this; // Better not happen!
        }
        if (data.isNBT()) {
            return ServoComponent.load(data.getTag());
        }
        return this;
    }
}
