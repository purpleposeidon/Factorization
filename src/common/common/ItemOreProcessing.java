package factorization.common;

import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;

public class ItemOreProcessing extends Item {
    public static final int IRON = 0, GOLD = 1, LEAD = 2, TIN = 3, COPPER = 4; //damage value mapping
    private static final int[] colorMap = {
        0xF0F0F0,
        0xFFFB00,
        0x2F2C3C,
        0xD7F7FF,
        0xFF6208
    };
    public static final String[] en_names = {"Iron", "Gold", "Lead", "Tin", "Copper"};
    static boolean enabled[] = new boolean[colorMap.length];
    String type;
    protected ItemOreProcessing(int itemID, int icon, String type) {
        super(itemID);
        setHasSubtypes(true);
        setIconIndex(icon);
        this.type = type;
        setTextureFile(Core.texture_file_item);
    }

    @Override
    public int getColorFromDamage(int damage, int renderPass) {
        if (damage < 0 || damage > colorMap.length) {
            return 0;
        }
        return colorMap[damage];
    }
    
    @Override
    public String getItemNameIS(ItemStack is) {
        return "item.oreProcessing" + type + is.getItemDamage();
    }
    
    void addEnglishNames(String prefix, String postfix) {
        for (int i = IRON; i <= COPPER; i++) {
            Core.proxy.addName("item.oreProcessing" + type + i, prefix + en_names[i] + postfix);
        }
    }
    
    public static void enable(int oreID) {
        enabled[oreID] = true;
    }
}
