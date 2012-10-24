package factorization.common;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.src.CreativeTabs;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import factorization.common.Core.TabType;

public class ItemOreProcessing extends Item {
    public static ArrayList<String> OD_ores = new ArrayList(), OD_ingots = new ArrayList();
    public static enum OreType {
        IRON(0, 0xF0F0F0, "Iron", "oreIron", "ingotIron"),
        GOLD(1, 0xFFFB00, "Gold", "oreGold", "ingotGold"),
        LEAD(2, 0x2F2C3C, "Lead", null, "ingotLead"),
        TIN(3, 0xD7F7FF, "Tin", "oreTin", "ingotTin"),
        COPPER(4, 0xFF6208, "Copper", "oreCopper", "ingotCopper"),
        SILVER(5, 0x7B96B9, "Silver", null, "ingotSilver"),
        GALENA(6, 0x687B99, "Galena", "oreSilver", null)
        ;
        int ID;
        int color;
        String en_name;
        String OD_ore, OD_ingot;
        boolean enabled = false;
        ItemStack processingResult = null;
        private OreType(int ID, int color, String en_name, String OD_ore, String OD_ingot) {
            this.ID = ID;
            this.color = color;
            this.en_name = en_name;
            this.OD_ore = OD_ore;
            this.OD_ingot = OD_ingot;
            if (OD_ore != null) {
                OD_ores.add(OD_ore);
            }
            if (OD_ingot != null) {
                OD_ingots.add(OD_ingot);
            }
        }
        
        public void enable() {
            this.enabled = true;
        }
        
        public static OreType fromOreClass(String oreClass) {
            for (OreType ot : values()) {
                if (ot.OD_ingot != null && ot.OD_ingot.equals(oreClass)) {
                    return ot;
                }
                if (ot.OD_ore != null && ot.OD_ore.equals(oreClass)) {
                    return ot;
                }
            }
            return null;
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
