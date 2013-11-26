package factorization.common.servo;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.Coord;
import factorization.common.Core;


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
    
    /** @return true if calling this instruction should stop execution for the tick. Things that might cause world-interaction should do this. */
    public boolean interrupts() {
        return false;
    }
}
