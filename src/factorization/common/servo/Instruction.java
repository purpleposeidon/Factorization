package factorization.common.servo;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.Coord;
import factorization.common.Core;


public abstract class Instruction extends Decorator {
    @Override
    public boolean onClick(EntityPlayer player, Coord block, ForgeDirection side) {
        /*TileEntityServoRail rail = block.getTE(TileEntityServoRail.class);
        if (rail == null) {
            return false;
        }
        if (rail.decoration != null) {
            FactorizationUtil.spawnItemStack(player, rail.decoration.toItem());
        }
        rail.setDecoration(this);
        return true;*/
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
                'I', Core.registry.dark_iron,
                '<', getRecipeItem(),
                '#', Core.registry.logicMatrixProgrammer);
    }
    
    protected abstract ItemStack getRecipeItem();
    
    /** @return true if calling this instruction should stop execution for the tick. Things that might cause world-interaction should do this. */
    public boolean interrupts() {
        return false;
    }
}
