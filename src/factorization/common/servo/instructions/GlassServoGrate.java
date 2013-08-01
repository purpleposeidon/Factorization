package factorization.common.servo.instructions;

import net.minecraft.block.Block;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import factorization.common.Core;

public class GlassServoGrate extends WoodenServoGrate {
    @Override
    public String getName() {
        return "fz.decorator.servoGrateGlass";
    }
    
    @Override
    public Icon getIcon(ForgeDirection side) {
        return Block.thinGlass.getBlockTextureFromSide(2);
    }
    
    @Override
    protected void addRecipes() {
        Core.registry.recipe(toItem(),
                " # ",
                "#-#",
                " # ",
                '-', Core.registry.servorail_item,
                '#', Block.thinGlass);
    }
}
