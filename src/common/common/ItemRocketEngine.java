package factorization.common;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class ItemRocketEngine extends ItemBlockProxy {

    protected ItemRocketEngine(int par1, ItemStack proxy) {
        super(par1, proxy);
        setItemName("rocketEngine");
        setTextureFile(Core.texture_file_item);
        setIconIndex(7);
    }

    @Override
    public void addInformation(ItemStack is, EntityPlayer player, List infoList, boolean verbose) {
        Core.brand(infoList);
    }
}
