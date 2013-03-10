package factorization.common;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.DamageSource;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.ISpecialArmor.ArmorProperties;
import factorization.api.IExoUpgrade;
import factorization.common.Core.TabType;

public class ExoCobblestoneDrive extends Item implements IExoUpgrade {

    protected ExoCobblestoneDrive(int par1) {
        super(par1);
        setUnlocalizedName("factorization:exo/cobbledrive");
        Core.tab(this, TabType.MISC);
        setMaxStackSize(1);
    }
    
    @Override
    public boolean canUpgradeArmor(ItemStack is, int armorIndex) {
        return armorIndex != 0;
    }

    @Override
    public ItemStack tickUpgrade(EntityPlayer player, ItemStack armor, ItemStack upgrade,
            boolean isEnabled) {
        if (!isEnabled) {
            return null;
        }
        if (player.worldObj.isRemote) {
            return null;
        }
        if (!FactorizationUtil.itemCanFire(player.worldObj, upgrade, 40)) {
            return null;
        }
        EntityItem cob = new EntityItem(player.worldObj, player.posX, player.posY, player.posZ, new ItemStack(Block.cobblestone));
        cob.age = 6000 - 5 * 20;
        cob.motionY = 0.1;
        player.worldObj.spawnEntityInWorld(cob);
        return upgrade;
    }

    @Override
    public void addArmorProperties(ItemStack is, ArmorProperties armor) {
    }

    @Override
    public int getArmorDisplay(ItemStack is) {
        return 0;
    }

    @Override
    public boolean damageArmor(EntityLiving entity, ItemStack stack, DamageSource source,
            int damage, int slot) {
        return false;
    }

    @Override
    public String getDescription() {
        return "Generates cobblestone";
    }

    @Override
    public void addInformation(ItemStack is, EntityPlayer player, List infoList, boolean verbose) {
        infoList.add("Exo-Upgrade");
        Core.brand(infoList);
    }
}
