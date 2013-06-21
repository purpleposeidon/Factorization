package factorization.common.servo.instructions;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.DataInNBT;
import factorization.api.datahelpers.DataOutNBT;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;
import factorization.common.BlockIcons;
import factorization.common.Core;
import factorization.common.FactorizationUtil;
import factorization.common.servo.ActuatorItem;
import factorization.common.servo.Instruction;
import factorization.common.servo.ServoMotor;
import factorization.common.servo.ServoStack;

public class ConfigureActuator extends Instruction {
    boolean dump_config = false;
    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        dump_config = data.as(Share.VISIBLE, prefix + "dump").putBoolean(dump_config);
        return this;
    }

    @Override
    protected ItemStack getRecipeItem() {
        return Core.registry.dark_iron_sprocket;
    }

    @Override
    public void motorHit(ServoMotor motor) {
        ServoStack equipment = motor.getServoStack(ServoMotor.STACK_EQUIPMENT);
        ServoStack config = motor.getServoStack(ServoMotor.STACK_ARGUMENT);
        ItemStack actuator = equipment.findType(ItemStack.class);
        if (actuator == null) {
            motor.putError("No actuator to configure");
            return;
        }
        if (!(actuator.getItem() instanceof ActuatorItem)) {
            motor.putError("Item " + actuator.getItem() + " was not an actuator");
            return;
        }
        ActuatorItem actualator = (ActuatorItem) actuator.getItem();
        
        try {
            if (dump_config) {
                DataHelper stack = config.getDataHelper(false);
                DataHelper nbt = new DataInNBT(FactorizationUtil.getTag(actuator));
                stack.as(Share.VISIBLE, "");
                nbt.as(Share.VISIBLE, "");
                stack.put(nbt.put(actualator.getState()));
            } else {
                DataHelper stack = config.getDataHelper(true);
                DataHelper nbt = new DataOutNBT(FactorizationUtil.getTag(actuator));
                stack.as(Share.VISIBLE, "");
                nbt.as(Share.VISIBLE, "");
                nbt.put(stack.put(actualator.getState()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Icon getIcon(ForgeDirection side) {
        if (dump_config) {
            return BlockIcons.servo$deconfigure;
        } else {
            return BlockIcons.servo$configure;
        }
    }

    @Override
    public String getName() {
        return "fz.instruction.configureactuator";
    }
    
    @Override
    public boolean onClick(EntityPlayer player, Coord block, ForgeDirection side) {
        if (playerHasProgrammer(player)) {
            dump_config = !dump_config;
            return true;
        }
        return false;
    }
    
    @Override
    public String getInfo() {
        return dump_config ? "Dump Configuration" : "Load Configuration";
    }

}
