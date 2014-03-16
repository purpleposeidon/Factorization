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

    public ItemFactorization(String name, TabType tabType) {
        setUnlocalizedName("factorization:" + name.replace('.', '/'));
        Core.tab(this, tabType);
    }
    
    String accessableUnlocalizedName;
    
    @Override
    public Item setUnlocalizedName(String name) {
        accessableUnlocalizedName = name;
        return super.setUnlocalizedName(name);
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    protected String getIconString() {
        return accessableUnlocalizedName;
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
