package factorization.api;

/**
 * This must only be implemented on TileEntities.
 */
public interface IChargeConductor extends ICoord, IMeterInfo {
    /**
     * Do not return null.
     */
    public Charge getCharge();
}
