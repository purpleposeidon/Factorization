package factorization.servo.instructions;

import factorization.shared.Core;
import net.minecraft.init.Blocks;

public class IronServoGrate extends WoodenServoGrate {
    @Override
    public String getName() {
        return "fz.decorator.servoGrateIron";
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
