package factorization.api.energy;

public interface IEnergyNet {
    void add(IContext context, WorkUnit unit);
    void destroyed(IContext context);
    void needsPower(IContext context);

    /**
     * Registers a new class of energy net.
     */
    public static void register(IEnergyNet watcher, WorkUnit... units) {
        WorkerBoss.registerNet(watcher, units);
    }

}
