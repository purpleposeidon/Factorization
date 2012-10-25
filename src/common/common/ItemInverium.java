package factorization.common;

import java.util.List;

import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ItemStack;

public class ItemInverium extends ItemCraftingComponent {
    int frame_count;
    public ItemInverium(int id, String itemName, int icon, int frame_count) {
        super(id, itemName, icon);
        this.frame_count = frame_count;
    }

    @Override
    public boolean hasEffect(ItemStack is) {
        return true;
    }
    
    @Override
    public int getIconFromDamage(int par1) {
        long frame = (System.currentTimeMillis() / 300) % frame_count;
        return icon + (int) frame;
        //return icon + ((int) (System.currentTimeMillis() / 1000) % frame_count);
    }
    
    @Override
    public int getColorFromDamage(ItemStack is, int renderPass) {
        int now = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
        int r = (int) (Math.abs(Math.cos(now/32000))*0x20) + 0xD0;
        int g = Math.min(0xFF, r*2);
        int b = 0x20;
        return (r << 16) + (g << 8) + b;
    }
    
    @Override
    public void addInformation(ItemStack is, EntityPlayer player, List infoList, boolean verbose) {
        if (is.getItemDamage() == 1) {
            infoList.add("Temporary recipe");
        }
        super.addInformation(is, player, infoList, verbose);
    }
}
