package factorization.common.servo.instructions;

import java.io.IOException;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.common.BlockIcons;
import factorization.common.Core;
import factorization.common.FactorizationUtil;
import factorization.common.FactorizationUtil.FzInv;
import factorization.common.servo.Decorator;
import factorization.common.servo.ServoMotor;
import factorization.common.servo.ServoStack;

public class ServoEquipmentBay extends Decorator {

    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        return this;
    }

    @Override
    public void motorHit(ServoMotor motor) {
        final Coord c = motor.getCurrentPos();
        final ForgeDirection facing = motor.orientation.facing;
        final ForgeDirection top = motor.orientation.top;
        if (facing == ForgeDirection.DOWN) {
            return;
        }
        if (try_(motor, c.add(top), top)) {
            return;
        } else if (try_(motor, c.add(facing), facing)) {
            return;
        }
        if (facing == ForgeDirection.UP) {
            motor.putError("Unequip failed");
        } else {
            motor.putError("Equip failed");
        }
    }

    boolean try_(ServoMotor motor, Coord inv, ForgeDirection dir) {
        return equip(motor, FactorizationUtil.openInventory(inv.getTE(IInventory.class), dir));
    }

    boolean equip(ServoMotor motor, FzInv inv) {
        if (inv == null) {
            return false;
        }
        ForgeDirection direwolf20 = motor.orientation.facing;
        ServoStack ss = motor.getServoStack(ServoMotor.STACK_EQUIPMENT);
        if (direwolf20 == ForgeDirection.UP) {
            // Unequip
            if (ss.getSize() <= 0) {
                motor.putError("Stack underflow");
                return true;
            }
            ItemStack is = ss.popType(ItemStack.class);
            if (is == null) {
                motor.putError("Stack underflow");
                return true;
            }
            if (inv.push(is) == null) {
                return true;
            }
        } else {
            // Equip
            if (ss.getFreeSpace() <= 0) {
                motor.putError("Stack overflow");
                return true;
            }
            ItemStack is = inv.pullWithLimit(1);
            if (is == null) {
                return false;
            }
            return ss.push(is);
        }
        return false;
    }

    @Override
    public boolean onClick(EntityPlayer player, Coord block, ForgeDirection side) {
        return false;
    }

    @Override
    public boolean onClick(EntityPlayer player, ServoMotor motor) {
        return false;
    }

    @Override
    public String getName() {
        return "fz.decorator.equipmentBay";
    }

    @Override
    public Icon getIcon(ForgeDirection side) {
        if (side == ForgeDirection.DOWN) {
            return BlockIcons.servo$bay_bottom;
        }
        if (side == ForgeDirection.UP) {
            return BlockIcons.servo$bay_top;
        }
        return BlockIcons.servo$bay;
    }

    @Override
    public float getSize() {
        return 0F / 16F;
    }

    @Override
    public boolean stretchIcon() {
        return true;
    }
    
    @Override
    protected void addRecipes() {
        Core.registry.recipe(toItem(),
                "IHI",
                "IGI",
                "III",
                'G', Core.registry.dark_iron_sprocket,
                'H', Block.hopperBlock,
                'I', Item.ingotIron);
    }
}
