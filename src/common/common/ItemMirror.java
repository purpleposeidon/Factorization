package factorization.common;

import java.util.List;

import net.minecraft.src.Block;
import net.minecraft.src.CreativeTabs;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.Item;
import net.minecraft.src.ItemBlock;
import net.minecraft.src.ItemStack;
import net.minecraft.src.World;

public class ItemMirror extends ItemBlockProxy {

    protected ItemMirror(int par1) {
        super(par1, Core.registry.mirror_item_hidden);
        setItemName("mirror");
        setTextureFile(Core.texture_file_item);
        setIconIndex(9);
    }

    @Override
    public void addInformation(ItemStack is, EntityPlayer player, List infoList, boolean verbose) {
        Core.brand(infoList);
    }
}
