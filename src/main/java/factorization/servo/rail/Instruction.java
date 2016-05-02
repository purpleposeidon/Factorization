package factorization.servo.rail;

import factorization.api.Coord;
import factorization.common.FzConfig;
import factorization.servo.iterator.CpuBlocking;
import factorization.servo.iterator.ServoMotor;
import factorization.shared.Core;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;


public abstract class Instruction extends Decorator {
    @Override
    public boolean onClick(EntityPlayer player, Coord block, EnumFacing side) {
        // FIXME: This is lame. Put in a method for cycling through state...
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
        Object recipeItem = getRecipeItem();
        if (recipeItem == null) return;
        Core.registry.oreRecipe(toItem(),
                "I<#",
                'I', getInstructionPlate(),
                '<', recipeItem,
                '#', Core.registry.logicMatrixProgrammer);
    }
    
    protected ItemStack getInstructionPlate() {
        return new ItemStack(Core.registry.instruction_plate);
    }
    
    protected abstract Object getRecipeItem();
    
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
