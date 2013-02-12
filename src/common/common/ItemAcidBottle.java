package factorization.common;

import java.util.List;

import factorization.common.Core.TabType;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class ItemAcidBottle extends ItemCraftingComponent {

    public ItemAcidBottle(int id, String itemName, int icon) {
        super(id, itemName, icon);
        Core.tab(this, TabType.MISC);
        setTextureFile(Item.potion.getTextureFile());
        setMaxStackSize(16);
    }
    
    @Override
    public boolean requiresMultipleRenderPasses() {
        return true;
    }
    
    @Override
    public int getIconFromDamageForRenderPass(int damage, int renderPass) {
        return Item.potion.getIconFromDamageForRenderPass(damage, renderPass);
    }
    
    @Override
    public int getColorFromItemStack(ItemStack stack, int renderPass) {
        if (renderPass == 0) {
            if (stack.getItemDamage() > 0) {
                return 0xFBD9B7;
            }
            return 0xB7EBFB;
        }
        return super.getColorFromItemStack(stack, renderPass);
    }
    
    @Override
    public String getItemNameIS(ItemStack stack) {
        String name = super.getItemNameIS(stack);
        if (stack.getItemDamage() > 0) {
            name += "regia";
        }
        return name;
    }

    public int getMaxItemUseDuration(ItemStack par1ItemStack) {
        return 32;
    }

    public EnumAction getItemUseAction(ItemStack par1ItemStack) {
        return EnumAction.drink;
    }

    public ItemStack onItemRightClick(ItemStack is, World w, EntityPlayer player) {
        player.setItemInUse(is, getMaxItemUseDuration(is));
        return is;
    }

    @Override
    public ItemStack onFoodEaten(ItemStack is, World w, EntityPlayer player) {
        is.stackSize--;
        Sound.acidBurn.playAt(player);
        if (w.isRemote) {
            return is;
        }
        FactorizationHack.damageEntity(player, FactorizationHack.acidBurn, is.getItemDamage() > 0 ? 15 : 10);
        player.getFoodStats().addStats(-20, 0);
        return is;
    }

    @Override
    public void addInformation(ItemStack is, EntityPlayer player, List infoList, boolean verbose) {
        super.addInformation(is, player, infoList, verbose);
        //Core.brand(infoList); taken care of in super
    }
    
    @Override
    public void getSubItems(int id, CreativeTabs tab, List list) {
        super.getSubItems(id, tab, list);
        list.add(Core.registry.aqua_regia);
    }
}
