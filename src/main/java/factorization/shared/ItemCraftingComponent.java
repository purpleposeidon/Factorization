package factorization.shared;

import factorization.shared.Core.TabType;



public class ItemCraftingComponent extends ItemFactorization {
    public ItemCraftingComponent(String name) {
        super(name, TabType.MATERIALS);
    }
    
    public ItemCraftingComponent(String name, TabType type) {
        super(name, type);
    }
}
