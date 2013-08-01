package factorization.common.servo.instructions;

import net.minecraft.block.Block;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import factorization.common.Core;

public class IronServoGrate extends WoodenServoGrate {
    @Override
    public String getName() {
        return "fz.decorator.servoGrateIron";
    }
    
    @Override
    public Icon getIcon(ForgeDirection side) {
        return Block.fenceIron.getBlockTextureFromSide(2);
    }
    
    @Override
    protected void addRecipes() {
        Core.registry.recipe(toItem(),
                " # ",
                "#-#",
                " # ",
                '-', Core.registry.servorail_item,
                '#', Block.fenceIron);
    }
}
