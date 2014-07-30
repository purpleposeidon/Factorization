package factorization.colossi;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import factorization.shared.Core;

public class ColossalBlockItem extends ItemBlock {

    public ColossalBlockItem(Block block) {
        super(block);
        setMaxDamage(0);
        setHasSubtypes(true);
    }
    
    @Override
    public int getMetadata(int md) {
        return md;
    }
    
    
    @Override
    public String getUnlocalizedName(ItemStack itemstack) {
        return getUnlocalizedName() + "." + itemstack.getItemDamage();
    }
    
    @Override
    public void addInformation(ItemStack is, EntityPlayer player, List list, boolean verbose) {
        Core.brand(is, player, list, verbose);
    }

}
