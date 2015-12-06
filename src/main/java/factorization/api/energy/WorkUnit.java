package factorization.api.energy;


import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A single unit of some kind of power, with energy sufficient to do one 'work action'.
 * A 'work action' is equivalent to a machine doing something, such as smelting an ore, breaking a block,
 * or pushing a block.
 * <p/>
 * There are no fractional work actions. (Well, there may be fractional work actions internal to a machine, for, eg,
 * transferring a liquid between tanks.) In general there should be a one-to-one correspondence between receiving a
 * WorkUnit and something happening that the player can see. It may also be reasonable for a machine to use multiple
 * WorkUnits (possibly of different kinds!) for different steps, such as to spin up, warm up, and fire.
 * <p/>
 * WorkUnit converters should have some inefficiency, such as using 1 input unit for each 16 units converted.
 * <p/>
 * Some types of energy need special information. For example, a machine powered by pressure might want to know what
 * the fluid is; a gearbox needs to keep in sync with the crankshaft.
 */
public class WorkUnit {
    @Nonnull
    public final EnergyCategory category;

    /**
     * The name of the energy type, namespaced by some particular mod's modid.
     */
    @Nonnull
    public final String name;

    /**
     * Creates a new WorkUnit. Duplicate instances of an already existing WorkUnit should usually be made using
     * {@link WorkUnit#produce()}.
     *
     * @param category The {@link EnergyCategory}.
     * @param ownerMod The modid of the classical owner of the WorkUnit. The mod need not be installed.
     * @param name     The name of the energy type.
     */
    public WorkUnit(@Nonnull EnergyCategory category, @Nonnull String ownerMod, @Nonnull String name) {
        Preconditions.checkNotNull(category, "null energy class");
        Preconditions.checkNotNull(ownerMod, "null owner modid");
        Preconditions.checkNotNull(name, "null name");
        this.category = category;
        this.name = (ownerMod + ":" + name).intern();
    }

    protected WorkUnit(@Nonnull WorkUnit self) {
        this.category = self.category;
        this.name = self.name;
    }

    /**
     * Makes a clone; typically used on a template WorkUnit instance.
     *
     * @return A WorkUnit instance that equals() this WorkUnit. All properties must be in their default state.
     * @see WorkUnit#WorkUnit(WorkUnit)
     */
    @Nonnull
    public WorkUnit produce() {
        return this;
    }

    /**
     * Used to synchronize with some property, such as rotational speed, or the glowing of heat.
     * It's pretty abstract. Ignoring this must always be acceptable, but may possibly give ugly visuals.
     *
     * @param parameterType The class of the type of the return value.
     * @param <T>           Also the type of the return value.
     * @return A visual parameter associated with a particular instance of an WorkUnit. May be null.
     * @see WorkUnit#with(Class, Object)
     */
    @Nullable
    public <T> T get(@Nonnull Class<T> parameterType) {
        return null;
    }

    /**
     * Set a property.
     *
     * @param type The class of the property.
     * @param val  The value of the property.
     * @param <T>  The type of the property.
     * @return a new WorkUnit with the property set, or else the same WorkUnit if the property is unsupported.
     * @see WorkUnit#get(Class)
     */
    @Nonnull
    public <T> WorkUnit with(@Nonnull Class<T> type, @Nullable T val) {
        return this;
    }

    @Override
    public final boolean equals(Object obj) {
        if (!(obj instanceof WorkUnit)) return false;
        WorkUnit other = (WorkUnit) obj;
        return other.name.equals(name);
    }
}
