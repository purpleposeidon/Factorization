package factorization.shared;

import factorization.shared.Core.TabType;
import factorization.util.FzUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

public class ItemFactorization extends Item {
    public ItemFactorization(String name, TabType tabType) {
        this(name, tabType, true);
    }

    public ItemFactorization(String name, TabType tabType, boolean hasStandardItemModel) {
        FzUtil.initItem(this, name, tabType);
        if (hasStandardItemModel) {
            Core.proxy.standardItemModel(this);
        }
    }

    String accessableUnlocalizedName;
    
    @Override
    public Item setUnlocalizedName(String name) {
        accessableUnlocalizedName = name;
        return super.setUnlocalizedName(name);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public final void addInformation(ItemStack is, EntityPlayer player, List<String> list, boolean verbose) {
        Core.brand(is, player, list, verbose);
    }
    
    protected void addExtraInformation(ItemStack is, EntityPlayer player, List<String> list, boolean verbose) {}
}
