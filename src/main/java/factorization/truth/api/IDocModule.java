package factorization.truth.api;

import net.minecraft.item.ItemStack;

public interface IDocModule {
    /**
     * Opens the manual & points it to the item the mouse is hovering over. Only works in GuiContainers.
     */
    void openPageForHilightedItem();

    /**
     * Opens the manual & points it at an item
     * @param is The item to look for.
     * @param forceOpen Ordinarily this will fail if in survival mode and the player doesn't have a manual available.
     *                  Setting forceOpen to true makes it work regardless.
     * @return true if something was found
     */
    boolean openBookForItem(ItemStack is, boolean forceOpen);
}
