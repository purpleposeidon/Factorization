package factorization.api.energy;


import com.google.common.base.Preconditions;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A single unit of some kind of power, with energy sufficient to do one 'work action'.
 * A 'work action' is equivalent to a machine doing something, such as smelting an ore, breaking a block,
 * or pushing a block.
 * <p/>
 * There are no fractional work actions. (Well, there may be fractional work actions internal to a machine, for, eg,
 * transferring a liquid between tanks.) In general there should be a one-to-one correspondence between receiving a
 * WorkUnit and something useful/significant/meaningful happening. It is reasonable for a machine to use multiple
 * WorkUnits (possibly of different kinds!) for different steps, such as to spin up, warm up, and fire.
 * <p/>
 * WorkUnit converters should have some inefficiency, such as using 1 input unit for each 16 units converted.
 * <p/>
 * Some types of energy need special information. For example, a machine powered by pressure might want to know what
 * the fluid is; a gearbox needs to keep in sync with the crankshaft. You can make this information available by
 * extending this class.
 */
public class WorkUnit {
    @Nonnull
    public final EnergyCategory category;

    @Nonnull
    public final ResourceLocation name;

    @Nonnull
    final Manager.ListenerList listener;

    /**
     * Gets or creates the prototype WorkUnit of the given name & category.
     * @param category The EnergyCategory.
     * @param name The name.
     * @return A WorkUnit (that may or may not be of WorkUnit.class)
     */
    @Nonnull
    public static WorkUnit get(@Nonnull EnergyCategory category, @Nonnull ResourceLocation name) {
        WorkUnit unit = find(category, name);
        if (unit != null) return unit;
        return new WorkUnit(category, name);
    }

    /**
     * @param category The EnergyCategory.
     * @param name The name.
     * @return a WorkUnit that has already been registered.
     * @see WorkUnit#get(EnergyCategory, ResourceLocation)
     */
    @Nullable
    public static WorkUnit find(@Nonnull EnergyCategory category, @Nonnull ResourceLocation name) {
        WorkUnit unit = Manager.prototypesByName.get(name);
        if (unit != null && unit.category != category) {
            throw new IllegalArgumentException("WorkUnits have mismatched categories: " + category + "/" + name + " vs prototype " + unit);
        }
        return unit;
    }

    /**
     * Creates a prototype WorkUnit. If a 'new' instance is needed (eg for custom fields), {@link WorkUnit#produce()}
     * should be used.
     * Use {@link WorkUnit#find(EnergyCategory, ResourceLocation)} if you are looking for the WorkUnit of another mod.
     *
     * @param category The {@link EnergyCategory}.
     * @param name     The name of the energy type, presumably with the same domain as the creating mod.
     */
    protected WorkUnit(@Nonnull EnergyCategory category, @Nonnull ResourceLocation name) {
        Preconditions.checkNotNull(category, "null energy class");
        Preconditions.checkNotNull(name, "null name");
        this.category = category;
        this.name = name;
        this.listener = new Manager.ListenerList();
        Manager.registerUnitPrototype(this);
    }

    protected WorkUnit(WorkUnit orig) {
        this.category = orig.category;
        this.name = orig.name;
        this.listener = orig.listener;
    }

    /**
     * Makes a clone; used on prototype WorkUnit instance.
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
     * It's pretty abstract. Ignoring this must always be acceptable from a conservation of energy standpoint,
     * but may cause ugly rendering.
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
     * Set a property. This should <b>NOT</b> be called on a prototype; use {@link WorkUnit#produce()}.
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

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return category + "/" + name;
    }
}
