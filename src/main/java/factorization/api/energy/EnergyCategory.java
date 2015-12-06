package factorization.api.energy;

/**
 * Various general types of energy.
 */
public enum EnergyCategory {
    /**
     * Linear and rotational energy; mass in relative motion. Falling anvils and turning waterwheels.
     */
    KINETIC,

    /**
     * Energy held in substances that is released under certain conditions, often after some threshold of input energy
     * is breached. Gunpowder can explode; steak can be digested. Uranium and hydrogen can undergo fission or fusion.
     * Sand can hover in the air.
     * <p/>
     * Not really a unit of energy; more of a unit of storage. Usefully converting chemical energy between other
     * forms tends to be slightly difficult.
     */
    POTENTIAL,

    /**
     * Positive pressure, such as from steam and compressed air.
     * The atmosphere is a pressure source to a relative vacuum.
     */
    PRESSURE,

    /**
     * Sub-atomic particles moving at or near the speed of light.
     * Photons, being light/electromagnetic radiation, are here.
     * Also includes free-moving protons, electrons, etc.
     */
    RADIATION,

    /**
     * Heat. A byproduct, or sometimes direct product, of many reactions. The fire crackles. Magma rumbles deep
     * in the nether.
     */
    THERMAL,

    /**
     * Electrons moving through, typically, metal wires. Alternating and Direct current. Also includes magnetism.
     * Lightning strikes the ground. The magnet block pulls the door shut.
     */
    ELECTRIC,

    /**
     * Redstone. Strangely easy to create. Is fundamentally suppressive, but this suppression is often itself
     * suppressed. Should be interpreted as a quick redstone pulse.
     */
    SIGNAL,

    /**
     * Periodic motion along an elastic medium. Waves roll along the ocean; two tectonic plates slide past
     * one another, producing tremors; the noteblock plays a tone.
     */
    OSCILLATION,

    /**
     * Maybe it's sufficiently advanced technology. Maybe it's the eldritch.
     */
    MAGIC
}
