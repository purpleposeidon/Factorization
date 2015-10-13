package factorization.truth.api;

import net.minecraft.item.ItemStack;

import java.util.Collection;

public interface ITypesetter {
    /**
     * The text is run in a nested context. Proper instruction is given in factorization:doc/README.txt
     * @param text Source to typeset
     */
    void write(String text) throws TruthError;

    /**
     * @param name Name of the variable
     * @return The value of the variable, or the empty string if that variable is not defined.
     */
    String getVariable(String name);

    /**
     * @return the ResourcePack domain that the documentation is being hosted from
     */
    String getDomain();

    /**
     * Writes an ItemStack. (This can also be done via {@link ITypesetter#write(String)})
     * @param stack The itemstack.
     */
    void write(ItemStack stack);

    /**
     * Writes a list of ItemStacks. (Not really possible with {@link ITypesetter#write(String)})
     * @param stacks A list of ItemStacks, preferably not empty.
     */
    void write(ItemStack[] stacks);

    /**
     * Collection version of {@link ITypesetter#write(ItemStack[])}.
     * @param stacks Collection of items.
     */
    void write(Collection<ItemStack> stacks);
}
