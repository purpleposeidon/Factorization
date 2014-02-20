package factorization.shared;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.shared.Core.TabType;

public class ItemFactorization extends Item {
    private int spriteNumber = 1;

    public ItemFactorization(int itemId, String name, TabType tabType) {
        super(itemId);
        setBlockName("factorization:" + name.replace('.', '/'));
        Core.tab(this, tabType);
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    protected String getIconString() {
        return unlocalizedName;
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public final void addInformation(ItemStack is, EntityPlayer player, List list, boolean verbose) {
        Core.brand(is, player, list, verbose);
    }
    
    protected void addExtraInformation(ItemStack is, EntityPlayer player, List list, boolean verbose) {}
    
    @Override
    @SideOnly(Side.CLIENT)
    public int getSpriteNumber() {
        return spriteNumber;
    }
    
    public void setSpriteNumber(int num) {
        this.spriteNumber = num;
    }
}
