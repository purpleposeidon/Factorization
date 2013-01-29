package factorization.api;

public interface IChargeConductor extends ICoord, IMeterInfo {
    /**
     * Do not return null.
     */
    public Charge getCharge();
}
