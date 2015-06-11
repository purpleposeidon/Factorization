package factorization.api.crafting;

public interface IVexatiousCrafting<MachineType> {

    /**
     * Return true if the recipe matches. Will be called on deserialization, as the active recipe is not saved.
     */
    boolean matches(MachineType machine);

    /**
     * Called when a match is found and crafting has begun. May be called multiple times.
     */
    void onCraftingStart(MachineType machine);

    /**
     * Called when crafting is complete. Only called once.
     */
    void onCraftingComplete(MachineType machine);

    /**
     * Recipes may match, but be unusable due output slots already being full. This checks for that.
     */
    boolean isUnblocked(MachineType machine);
}
