package factorization.servo.instructions;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;
import factorization.shared.Core;

public class IronServoGrate extends WoodenServoGrate {
    @Override
    public String getName() {
        return "fz.decorator.servoGrateIron";
    }
    
    @Override
    public IIcon getIIcon(ForgeDirection side) {
        return Blocks.fenceIron.getBlockTextureFromSide(2);
    }
    
    @Override
    protected void addRecipes() {
        Core.registry.recipe(toItem(),
                " # ",
                "#-#",
                " # ",
                '-', Core.registry.servorail_item,
                '#', Blocks.fenceIron);
    }
}
