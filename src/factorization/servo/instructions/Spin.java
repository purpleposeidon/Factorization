package factorization.servo.instructions;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.Coord;
import factorization.api.FzOrientation;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;
import factorization.common.BlockIcons;
import factorization.servo.Instruction;
import factorization.servo.ServoMotor;

public class Spin extends Instruction {
    boolean cc = true;
    
    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        cc = data.as(Share.VISIBLE, prefix + "cc").putBoolean(cc);
        return this;
    }
    
    @Override
    public boolean onClick(EntityPlayer player, Coord block, ForgeDirection side) {
        if (player.worldObj.isRemote) {
            return true;
        }
        if (playerHasProgrammer(player)) {
            cc = !cc;
            return true;
        }
        return false;
    }

    @Override
    protected ItemStack getRecipeItem() {
        return new ItemStack(Item.silk);
    }

    @Override
    public void motorHit(ServoMotor motor) {
        ForgeDirection newTop = motor.orientation.top;
        for (int i = cc ? 3 : 1; i > 0; i--) {
            newTop = newTop.getRotation(motor.orientation.facing);
        }
        FzOrientation next = motor.orientation.pointTopTo(newTop);
        if (next != FzOrientation.UNKNOWN) {
            motor.orientation = next;
        }
    }

    @Override
    public Icon getIcon(ForgeDirection side) {
        return cc ? BlockIcons.servo$spin_cc : BlockIcons.servo$spin_ccc;
    }

    @Override
    public String getName() {
        return "fz.instruction.spin";
    }
}
