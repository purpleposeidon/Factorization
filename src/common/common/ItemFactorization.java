package factorization.common;

import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.common.Core.TabType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ItemFactorization extends Item {

    public ItemFactorization(int itemId, String name, TabType tabType) {
        super(itemId);
        setUnlocalizedName("factorization:" + name.replace('.', '/'));
    }
    @Override
    @SideOnly(Side.CLIENT)
    public final void addInformation(ItemStack is, EntityPlayer player, List list, boolean verbose) {
        Core.brand(is, player, list, verbose);
    }
    
    protected void addExtraInformation(ItemStack is, EntityPlayer player, List list, boolean verbose) {}
}
