package factorization.common;

import net.minecraft.src.Block;
import net.minecraft.src.DamageSource;
import net.minecraft.src.EntityItem;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraftforge.common.ISpecialArmor.ArmorProperties;
import factorization.api.IMechaUpgrade;

public class MechaCobblestoneDrive extends Item implements IMechaUpgrade {

    protected MechaCobblestoneDrive(int par1) {
        super(par1);
        setItemName("mecha.cobbledrive");
        setIconIndex(16*10 + 1);
    }
    
    @Override
    public String getTextureFile() {
        return Core.texture_file_item;
    }

    @Override
    public ItemStack tickUpgrade(EntityPlayer player, ItemStack armor, ItemStack upgrade,
            boolean isEnabled) {
        if (!isEnabled) {
            return null;
        }
        if (!Core.isCannonical()) {
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
}
