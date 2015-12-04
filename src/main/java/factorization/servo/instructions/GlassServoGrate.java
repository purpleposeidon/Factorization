package factorization.servo.instructions;

import factorization.shared.Core;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IIcon;

public class GlassServoGrate extends WoodenServoGrate {
    @Override
    public String getName() {
        return "fz.decorator.servoGrateGlass";
    }
    
    @Override
    public IIcon getIcon(EnumFacing side) {
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
