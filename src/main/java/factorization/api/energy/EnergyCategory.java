package factorization.api.energy;

/**
 * Various general types of energy.
 */
public enum EnergyCategory {
    /**
     * Mass moving in a straight line. Anvils fall; a camshaft drives a reciprocating follower.
     */
    LINEAR,

    /**
     * Mass rotating around a point, typically a center of mass. A waterwheel drives a shaft; gears invert the axis
     * of rotation
     */
    ROTATIONAL,

    /**
     * Energy held in substances that is released under certain conditions, often after some threshold of input energy
     * is breached. Gunpowder can explode; steak can be digested. Uranium and hydrogen can undergo fission or fusion.
     * Sand can hover in the air until a block update causes it to fall.
     * <p/>
     * Not really a unit of energy; more of a unit of storage. Usefully converting chemical energy between other
     * forms tends to be slightly difficult.
     */
    POTENTIAL,

    /**
     * Positive pressure, such as from steam and compressed air.
     * The atmosphere is a pressure source to a relative vacuum.
     * Steam drives a piston. Hydrolic oil drives a piston.
     */
    PRESSURE,

    /**
     * Sub-atomic particles moving at or near the speed of light.
     * Photons, being light/electromagnetic radiation, are here.
     * Also includes protons, electrons, etc.
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
     * Redstone signal. Strangely easy to create. Is fundamentally suppressive, but this suppression is often itself
     * suppressed. Receiving a SIGNAL should probably be interpreted as a quick redstone pulse. Implementing this
     * behavior is not at all obligatory, particularly in blocky contexts.
     */
    SIGNAL,

    /**
     * Periodic motion along an elastic medium. Waves crash against rocks; two tectonic plates slide past
     * one another, producing tremors; the noteblock plays a tone.
     */
    OSCILLATION,

    /**
     * Maybe it's sufficiently advanced technology. Maybe it's the eldritch.
     */
    MAGIC
}
