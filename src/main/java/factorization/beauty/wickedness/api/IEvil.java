package factorization.beauty.wickedness.api;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;

import java.util.Random;

public interface IEvil {
    /**
     * @return a unique ID, such as "neptunepink:ultimate_evil"
     */
    String getId();

    /**
     * @return The ideal primary color.
     */
    int getMainColor();

    /**
     * @return The ideal secondary color.
     */
    int getSecondColor();

    /**
     * @param egg The location of the egg
     * @param evilBit The information about the egg, including an optional NBT tag compound to store instance-specific data.
     */
    void setup(TileEntity egg, EvilBit evilBit);

    /**
     * Do the evil thing.
     * @param egg Location
     * @param evilBit additional data
     * @param nearbyPlayer A player, if evilBit.playerRange >= 0. If a player needed to be found, then this method
     *                     is not called, and a tick does not occur. If no player is needed, the the parameter is null.
     * @return true if entheas should be consumed.
     */
    boolean act(TileEntity egg, EvilBit evilBit, EntityPlayer nearbyPlayer, Random rand);
}
