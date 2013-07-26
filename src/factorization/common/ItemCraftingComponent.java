package factorization.common;

import factorization.common.Core.TabType;


public class ItemCraftingComponent extends ItemFactorization { //NORELEASE: Move stuff that isn't expensive into a single ID
    public ItemCraftingComponent(int id, String name) {
        super(id, name, TabType.MATERIALS);
    }
}
