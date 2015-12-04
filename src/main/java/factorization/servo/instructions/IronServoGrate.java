package factorization.servo.instructions;

import factorization.shared.Core;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IIcon;

public class IronServoGrate extends WoodenServoGrate {
    @Override
    public String getName() {
        return "fz.decorator.servoGrateIron";
    }
    
    @Override
    public IIcon getIcon(EnumFacing side) {
        return Blocks.iron_bars.getBlockTextureFromSide(2);
    }
    
    @Override
    protected void addRecipes() {
        Core.registry.oreRecipe(toItem(),
                " # ",
                "#-#",
                " # ",
                '-', Core.registry.servorail_item,
                '#', Blocks.iron_bars);
    }
}
