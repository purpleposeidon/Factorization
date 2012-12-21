package factorization.common;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class ItemMirror extends ItemBlockProxy {

    protected ItemMirror(int par1) {
        super(par1, Core.registry.mirror_item_hidden);
        setItemName("mirror");
        setTextureFile(Core.texture_file_block);
        setIconIndex(Texture.mirrorStart);
    }

    @Override
    public void addInformation(ItemStack is, EntityPlayer player, List infoList, boolean verbose) {
        Core.brand(infoList);
    }
}
