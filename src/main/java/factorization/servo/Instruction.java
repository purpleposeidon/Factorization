package factorization.servo;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;
import factorization.api.Coord;
import factorization.common.FzConfig;
import factorization.shared.Core;


public abstract class Instruction extends Decorator {
    @Override
    public boolean onClick(EntityPlayer player, Coord block, ForgeDirection side) {
        return false;
    }
    
    @Override
    public boolean onClick(EntityPlayer player, ServoMotor motor) {
        return false;
    }
    
    @Override
    public boolean isFreeToPlace() {
        return true;
    }
    
    @Override
    protected void addRecipes() {
        Core.registry.recipe(toItem(),
                "I<#",
                'I', getInstructionPlate(),
                '<', getRecipeItem(),
                '#', Core.registry.logicMatrixProgrammer);
    }
    
    protected ItemStack getInstructionPlate() {
        return new ItemStack(Core.registry.instruction_plate);
    }
    
    protected abstract ItemStack getRecipeItem();
    
    /** returns for how long the instruction should block for. */
    public CpuBlocking getBlockingBehavior() {
        return CpuBlocking.NO_BLOCKING;
    }
    
    @Override
    public float getSize() {
        if (FzConfig.large_servo_instructions) {
            return 3F/16F;
        }
        return super.getSize();
    }
    
    @Override
    public boolean collides() {
        return false;
    }
    
    public String toString() {
        String info = getInfo();
        if (info == null) {
            return getClass().getSimpleName();
        }
        return info;
    }
}
