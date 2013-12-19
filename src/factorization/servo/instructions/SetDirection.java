package factorization.servo.instructions;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;
import factorization.common.BlockIcons;
import factorization.servo.Instruction;
import factorization.servo.ServoMotor;

public class SetDirection extends Instruction {
    ForgeDirection dir = ForgeDirection.UP;
    
    @Override
    public Icon getIcon(ForgeDirection side) {
        if (side == ForgeDirection.UNKNOWN) {
            return BlockIcons.servo$set_direction.side_W;
        }
        return BlockIcons.servo$set_direction.get(dir.getOpposite(), side);
    }
    
    @Override
    public boolean onClick(EntityPlayer player, Coord block, ForgeDirection side) {
        if (playerHasProgrammer(player)) {
            int i = dir.ordinal();
            dir = ForgeDirection.getOrientation((i + 1) % 6);
            return true;
        }
        return false;
    }

    @Override
    public void motorHit(ServoMotor motor) {
        ForgeDirection d = dir.getOpposite();
        motor.nextDirection = d;
        if (d == motor.orientation.facing.getOpposite()) {
            motor.changeOrientation(d);
        }
    }

    @Override
    public String getName() {
        return "fz.instruction.setdirection";
    }
    
    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        dir = data.as(Share.MUTABLE, "dir").putEnum(dir);
        return this;
    }
    
    @Override
    protected ItemStack getRecipeItem() {
        return new ItemStack(Item.arrow);
    }

}
