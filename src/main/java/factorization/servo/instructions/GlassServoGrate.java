package factorization.servo.instructions;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;
import factorization.shared.Core;

public class GlassServoGrate extends WoodenServoGrate {
    @Override
    public String getName() {
        return "fz.decorator.servoGrateGlass";
    }
    
    @Override
    public IIcon getIcon(ForgeDirection side) {
        return Blocks.glass_pane.getBlockTextureFromSide(2);
    }
    
    @Override
    protected void addRecipes() {
        Core.registry.oreRecipe(toItem(),
                " # ",
                "#-#",
                " # ",
                '-', Core.registry.servorail_item,
                '#', Blocks.glass_pane);
    }
}
