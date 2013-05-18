package factorization.common.servo;

import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;


public abstract class Instruction extends Decorator {
    public abstract Icon getIcon(ForgeDirection side);
}
