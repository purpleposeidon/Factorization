package factorization.api.wind;

import net.minecraft.util.EnumFacing;

public interface IWindmill {
    /**
     * @return The radius of the windmill, in blocks
     */
    int getWindmillRadius();

    /**
     * @return The axis the windmill's sails rotate around
     */
    EnumFacing getDirection();
}
