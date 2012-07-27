package factorization.api;


public interface IChargeConductor extends ICoord {
    /**
     * Generators: Increment charge every tick, up to a generator-specific point. Maybe 10? <br/>
     * Sinks: Consume charge only every few ticks. This gives time for charge to spread to other conductors. Also buffer up some charge. <br/>
     * Batteries: Use internal storage to try to keep the charge level at a certain amount. Every tick, add or remove 1 unit from the storage to try to keep the
     * charge at (maybe?) 3. This'll keep batteries from charging other batteries tho, unless there was a transformer or something.
     */
    public Charge getCharge();
}
