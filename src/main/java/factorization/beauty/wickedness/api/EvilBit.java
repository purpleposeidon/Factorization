package factorization.beauty.wickedness.api;

import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;
import net.minecraft.nbt.NBTTagCompound;

import java.io.IOException;

public class EvilBit implements IDataSerializable {
    /**
     * @return the main color. This is the main color of the egg, and should be used whenever possible.
     */
    public int getMainColor() {
        return color1;
    }

    /**
     * @return the secondary color. This is the color the egg's spots, and should be used if there's someplace reasonable for it.
     */
    public int getSecondColor() {
        return color2;
    }

    /**
     * How many ticks have happened. Ticks do not occur if it needs a player, but there is no player.
     */
    public int ticks = 0;

    /**
     * How often IEvil.act() should be called.
     */
    public int tickSpeed = 40;

    /**
     * How close a player should be. Set to -1 if the act of evil doesn't need a player nearby.
     */
    public int playerRange = 8;

    /**
     * How much entheas a tick should cost.
     */
    public int cost = 1;

    /**
     * @return a tag to store custom data in, if necessary.
     */
    public NBTTagCompound getTag() {
        return tag;
    }

    /**
     *

     /**
     * This should return false if:
     *      - The behavior could be used as a mob spawner
     *      - It is especially destructive
     *      - It gives out free diamonds or something
     *
     * Should probably be false for 90% of cases
     */
    public boolean reasonablyInfinite = false;


    public EvilBit(int color1, int color2) {
        this.color1 = color1;
        this.color2 = color2;
    }

    private int color1, color2;
    private NBTTagCompound tag = new NBTTagCompound();

    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        color1 = data.as(Share.PRIVATE, "color1").putInt(color1);
        color2 = data.as(Share.PRIVATE, "color2").putInt(color2);
        ticks = data.as(Share.PRIVATE, "ticks").putInt(ticks);
        tickSpeed = data.as(Share.PRIVATE, "tickSpeed").putInt(tickSpeed);
        playerRange = data.as(Share.PRIVATE, "playerRange").putInt(playerRange);
        tag = data.as(Share.PRIVATE, "evilTag").putTag(tag);
        cost = data.as(Share.PRIVATE, "cost").putInt(cost);
        reasonablyInfinite = data.as(Share.PRIVATE, "reasonablyInfinite").putBoolean(reasonablyInfinite);
        return this;
    }
}
