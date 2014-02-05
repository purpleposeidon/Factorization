package factorization.servo.instructions;

import java.io.IOException;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.common.BlockIcons;
import factorization.servo.Decorator;
import factorization.servo.ServoMotor;
import factorization.shared.Core;
import factorization.shared.FzUtil;
import factorization.shared.FzUtil.FzInv;

public class ServoEquipmentBay extends Decorator {

    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        return this;
    }
    
    String error = null;

    @Override
    public void motorHit(ServoMotor motor) {
        if (motor.worldObj.isRemote) {
            return;
        }
        final Coord c = motor.getCurrentPos();
        final ForgeDirection facing = motor.getOrientation().facing;
        final ForgeDirection top = motor.getOrientation().top;
        if (facing == ForgeDirection.DOWN) {
            return;
        }
        if (try_(motor, c.add(top), top)) {
            return;
        } else if (try_(motor, c.add(facing), facing)) {
            return;
        }
        if (error != null) {
            motor.putError(error);
            error = null;
        } else if (facing == ForgeDirection.UP) {
            motor.putError("Unequip failed");
        } else {
            motor.putError("Equip failed");
        }
    }

    boolean try_(ServoMotor motor, Coord inv, ForgeDirection dir) {
        return equip(motor, FzUtil.openInventory(inv.getTE(IInventory.class), dir));
    }

    /**
     * @return true if we should stop looking for inventories to use
     */
    boolean equip(ServoMotor motor, FzInv inv) {
        if (inv == null) {
            error = "Not facing an inventory";
            return false;
        }
        ForgeDirection direwolf20 = motor.getOrientation().facing;
        if (direwolf20 == ForgeDirection.UP) {
            // Unequip
            if (!motor.getInv().transfer(inv, 1, null)) {
                if (motor.getInv().isEmpty()) {
                    motor.putError("Servo Inventory is empty");
                } else {
                    motor.putError("Transfer failed");
                }
            }
            return true;
        } else {
            // Equip
            if (!inv.transfer(motor.getInv(), 1, null)) {
                if (inv.isEmpty()) {
                    motor.putError("Source inventory is empty");
                } else {
                    motor.putError("Transfer failed");
                }
            }
            return true;
        }
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
