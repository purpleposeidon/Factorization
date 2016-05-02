package factorization.servo.instructions;

import factorization.servo.ServoMotor;
import factorization.util.InvUtil;
import factorization.util.InvUtil.FzInv;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

public class CountItems extends SimpleInstruction {
    @Override
    protected Object getRecipeItem() {
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
    protected String getSimpleName() {
        return "countitems";
    }
}
