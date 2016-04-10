package factorization.shared;

import factorization.shared.Core.TabType;



public class ItemCraftingComponent extends ItemFactorization {
    public ItemCraftingComponent(String name) {
        super(name, TabType.MATERIALS);
    }
    
    public ItemCraftingComponent(String name, TabType type) {
        super(name, type);
    }

    public ItemCraftingComponent(String diamond_cutting_head, TabType type, boolean hasStandardItemModel) {
        super(diamond_cutting_head, type, hasStandardItemModel);
    }
}
