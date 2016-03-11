package factorization.api.energy;

@SuppressWarnings("UnnecessaryInterfaceModifier")
public interface IEnergyNet {
    /**
     * This method is called at registration time.
     * @param unit The unit
     * @return true if the enet can do something useful with the power unit.
     */
    boolean canHandlePower(WorkUnit unit);

    /**
     * Inject power into the energy net, making it available to its connected machinery.
     * @param generator The generator/transformer/battery/etc
     * @param unit The unit
     * @return true if the power was taken. Do not return true if the energynet can't handle the unit; wasting unused
     * power is the generator's job.
     */
    boolean propagatePower(IContext generator, WorkUnit unit);

    /**
     * A worker has been created. Note that this may be called multiple times with differing units.
     * @param context The worker's context.
     * @param unit A unit that the worker accepts.
     */
    void workerAdded(IContext context, WorkUnit unit);

    /**
     * @param context The machine that needs power.
     */
    void workerNeedsPower(IContext context);

    /**
     * @param context The machine that was removed from the world. Note that callbacks will not be given when things are
     *                unloaded; enets must manage this themselves using {@link net.minecraftforge.event.world.ChunkEvent.Unload}.
     */
    void workerDestroyed(IContext context);

    /**
     * Registers a new class of energy net.
     */
    public static void register(IEnergyNet watcher) {
        Manager.registerNet(watcher);
    }

    /**
     * Try to inject power into an energy net.
     * @param source The generator
     * @param unit The unit
     * @return true if the unit was taken.
     */
    public static boolean offer(IContext source, WorkUnit unit) {
        return Manager.offer(source, unit);
    }

}
