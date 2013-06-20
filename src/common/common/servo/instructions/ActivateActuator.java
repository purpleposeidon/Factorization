package factorization.common.servo.instructions;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.common.BlockIcons;
import factorization.common.servo.ActuatorItem;
import factorization.common.servo.Instruction;
import factorization.common.servo.ServoMotor;
import factorization.common.servo.ServoStack;

public class ActivateActuator extends Instruction {

    @Override
    public Icon getIcon(ForgeDirection side) {
        if (sneaky) {
            return BlockIcons.servo$activate_sneaky;
        }
        return BlockIcons.servo$activate;
    }
    
    boolean sneaky = false;

    @Override
    public void motorHit(ServoMotor motor) {
        ServoStack ss = motor.getServoStack(ServoMotor.STACK_EQUIPMENT);
        ItemStack is = ss.popType(ItemStack.class);
        if (is == null) {
            return;
        }
        if (is.getItem() instanceof ActuatorItem) {
            ActuatorItem actuator = (ActuatorItem) is.getItem();
            if (actuator == null) {
                return;
            }
            boolean last_sneak = motor.sneaking;
            motor.sneaking = sneaky;
            actuator.onUse(is, motor);
            motor.sneaking = last_sneak;
        }
        ss.forceAppend(is);
    }
    
    @Override
    public boolean onClick(EntityPlayer player, Coord block, ForgeDirection side) {
        sneaky = !sneaky;
        return true;
    }

    @Override
    public String getName() {
        return "fz.instruction.activateactuator";
    }

    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        sneaky = data.asSameShare(prefix + "sneak").putBoolean(sneaky);
        return this;
    }
    
    @Override
    protected ItemStack getRecipeItem() {
        return new ItemStack(Item.redstone);
    }
}
