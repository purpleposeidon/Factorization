package factorization.common;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

public class ItemBlockResource extends ItemBlock {
    public ItemBlockResource(int id) {
        super(id);
        setMaxDamage(0);
        setHasSubtypes(true);
    }

    public int getMetadata(int i) {
        return i;
    }
    
    @Override
    public String getUnlocalizedName(ItemStack itemstack) {
        // I don't think this actually gets called...
        int md = itemstack.getItemDamage();
        if (md < ResourceType.values().length && md >= 0) {
            ResourceType rs = ResourceType.values()[md];
            return getUnlocalizedName() + "." + rs;
        } 
        return getUnlocalizedName() + ".unknownMd" + md;
    }
    
    @Override
    public void addInformation(ItemStack is, EntityPlayer player, List list, boolean verbose) {
        Core.brand(is, list);
    }
}
