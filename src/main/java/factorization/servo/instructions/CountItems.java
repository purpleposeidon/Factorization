package factorization.servo.instructions;

import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.servo.Instruction;
import factorization.servo.ServoMotor;
import factorization.util.InvUtil;
import factorization.util.InvUtil.FzInv;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IIcon;

import java.io.IOException;

public class CountItems extends Instruction {

    @Override
    public IDataSerializable putData(String prefix, DataHelper data) throws IOException {
        return this;
    }

    @Override
    protected ItemStack getRecipeItem() {
        return new ItemStack(Items.comparator);
    }

    @Override
    public void motorHit(ServoMotor motor) {
        int count = 0;
        FzInv inv = InvUtil.openInventory(motor, false);
        for (int i = 0; i < inv.size(); i++) {
            ItemStack is = inv.get(i);
            if (is == null) continue;
            count += is.stackSize;
        }
        motor.getArgStack().push(count);
    }

    @Override
    public IIcon getIcon(EnumFacing side) {
        return BlockIcons.servo$count_items;
    }

    @Override
    public String getName() {
        return "fz.instruction.countitems";
    }

}
