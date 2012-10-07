package factorization.common;

import java.util.List;

import factorization.common.Core.TabType;

import net.minecraft.src.CreativeTabs;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;

public class ItemOreProcessing extends Item {
    public static enum OreType {
        IRON(0, 0xF0F0F0, "Iron"),
        GOLD(1, 0xFFFB00, "Gold"),
        LEAD(2, 0x2F2C3C, "Lead"),
        TIN(3, 0xD7F7FF, "Tin"),
        COPPER(4, 0xFF6208, "Copper"),
        SILVER(5, 0x7B96B9, "Silver"),
        GALENA(6, 0x687B99, "Galena")
        ;
        int ID;
        int color;
        String en_name;
        boolean enabled = false;
        private OreType(int ID, int color, String en_name) {
            this.ID = ID;
            this.color = color;
            this.en_name = en_name;
        }
        
        public void enable() {
            this.enabled = true;
        }
    }
    
    String stateName;

    protected ItemOreProcessing(int itemID, int icon, String stateName) {
        super(itemID);
        setTextureFile(Core.texture_file_item);
        setHasSubtypes(true);
        setIconIndex(icon);
        this.stateName = stateName;
        Core.tab(this, TabType.MATERIALS);
        setItemName("itemOreProcessing" + stateName);
    }

    @Override
    public int getColorFromDamage(int damage, int renderPass) {
        try {
            return OreType.values()[damage].color;
        } catch (ArrayIndexOutOfBoundsException e) {
            return 0xFFFF00;
        }
    }

    @Override
    public String getItemNameIS(ItemStack is) {
        return "item.oreProcessing" + stateName + is.getItemDamage();
    }

    void addEnglishNames(String prefix, String postfix) {
        for (OreType oreType : OreType.values()) {
            Core.proxy.addName("item.oreProcessing" + stateName + oreType.ID, prefix + oreType.en_name + postfix);
        }
    }

    @Override
    public void getSubItems(int id, CreativeTabs tab, List list) {
        for (OreType oreType : OreType.values()) {
            if (oreType.enabled) {
                list.add(new ItemStack(this, 1, oreType.ID));
            }
        }
    }

    @Override
    public void addInformation(ItemStack is, List infoList) {
        super.addInformation(is, infoList);
        Core.brand(infoList);
    }
    
    public ItemStack makeStack(OreType ot) {
        return new ItemStack(this, 1, ot.ID);
    }
}
