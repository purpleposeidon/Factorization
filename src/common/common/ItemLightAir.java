package factorization.common;

import java.util.List;

import net.minecraft.src.ItemBlock;
import net.minecraft.src.ItemStack;

public class ItemLightAir extends ItemBlock {
    public ItemLightAir() {
        super(Core.lightair_id + Core.block_item_id_offset);
        setMaxDamage(0);
        setHasSubtypes(true);
    }

    public int getMetadata(int i) {
        return i;
    }

    @Override
    public String getItemNameIS(ItemStack itemstack) {
        // I don't think this actually gets called...
        int md = itemstack.getItemDamage();
        return "Bright Air";
    }

    @Override
    public String getItemName() {
        return "item.lightair";
    }

    @Override
    public void addInformation(ItemStack is, List infoList) {
        super.addInformation(is, infoList);
        Core.brand(infoList);
    }
}
