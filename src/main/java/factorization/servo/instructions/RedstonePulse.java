package factorization.servo.instructions;

import factorization.api.Coord;
import factorization.servo.iterator.CpuBlocking;
import factorization.servo.iterator.ServoMotor;
import factorization.shared.TileEntityCommon;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

public class RedstonePulse extends SimpleInstruction {
    @Override
    public void motorHit(ServoMotor motor) {
        if (motor.worldObj.isRemote) {
            return;
        }
        TileEntityCommon tef = motor.getCurrentPos().getTE(TileEntityCommon.class);
        if (tef == null) {
            return; //Just back away, very slowly...
        }
        tef.pulse();
    }

    @Override
    public boolean onClick(EntityPlayer player, Coord block, EnumFacing side) {
        return false;
    }

    @Override
    public boolean onClick(EntityPlayer player, ServoMotor motor) {
        return false;
    }

    @Override
    protected String getSimpleName() {
        return "pulse";
    }

    @Override
    protected Object getRecipeItem() {
        return new ItemStack(Blocks.stone_pressure_plate);
    }
    
    @Override
    public CpuBlocking getBlockingBehavior() {
        return CpuBlocking.BLOCK_UNTIL_NEXT_ENTRY;
    }
}
