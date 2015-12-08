package factorization.servo.instructions;

import factorization.shared.Core;
import net.minecraft.init.Blocks;

public class GlassServoGrate extends WoodenServoGrate {
    @Override
    public String getName() {
        return "fz.decorator.servoGrateGlass";
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
