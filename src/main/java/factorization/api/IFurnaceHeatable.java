package factorization.api;

public interface IFurnaceHeatable {
    /**
     * @return true if you want to recieve heat, and something useful can be done with it. Return false if there is no valid recipe.
     */
    public boolean acceptsHeat();

    /**
     * Called when the FurnaceHeater gives heat. May be called many times per tick.
     *
     * The behavior of vanilla furnaces is to have its fuel time increased up to some arbitrary level,
     * and then boost the cook progress time.
     */
    public void giveHeat();

    /**
     * Vanilla furnaces are expensive to transition between started and stopped; they cause a chunk redraw and a lighting update.
     * This method instructs the heater to wait until it has a suffiently large buffer to keep running for a bit;
     * otherwise there may be unsightly flashing if the heater doesn't have enough power to stay on steadily.
     *
     * @return true if starting is laggy
     */
    public boolean hasLaggyStart();

    /**
     * @return true if the heatable is cooking something. Used with hasLaggyStart().
     */
    public boolean isStarted();
}
