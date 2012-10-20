package factorization.api;

public interface IChargeConductor extends ICoord {
    /**
     * Do not return null.
     */
    public Charge getCharge();

    /**
     * @return a string to be shown along with energy information when right-clicked with a charge meter
     */
    public String getInfo();
}
