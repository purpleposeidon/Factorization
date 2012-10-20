package factorization.common;

import java.util.List;

import net.minecraft.src.Block;
import net.minecraft.src.CreativeTabs;
import net.minecraft.src.DamageSource;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.Material;
import net.minecraftforge.common.ISpecialArmor.ArmorProperties;
import factorization.api.Coord;
import factorization.api.IExoUpgrade;
import factorization.common.Core.TabType;

class ExoBuoyantBarrel extends Item implements IExoUpgrade {

    protected ExoBuoyantBarrel(int par1) {
        super(par1);
        setItemName("exo.buoyantbarrel");
        setIconIndex(16 * 10);
        Core.tab(this, TabType.MISC);
        setMaxStackSize(1);
        setTextureFile(Core.texture_file_item);
    }
    
    @Override
    public boolean canUpgradeArmor(ItemStack is, int armorIndex) {
        return true;
    }

    @Override
    public ItemStack tickUpgrade(EntityPlayer player, ItemStack armor, ItemStack upgrade,
            boolean enabled) {
        if (!enabled) {
            return null;
        }
        Coord head = new Coord(player.worldObj, player.posX, player.posY - 0.5, player.posZ);
        Block b = head.getBlock();
        boolean headInWater = false;
        if (b != null) {
            headInWater = b.blockMaterial == Material.water;
        }
        if (player.isInWater()) {
            float maxSpeed = 0.5F;
            float accel = 0.020F;
            if (headInWater) {
                accel = 0.1F;
            }
            if (armor.getItem() == Core.registry.exo_foot) {
                //bonus for water-walking...
                maxSpeed = 0.8F;
                accel *= 1.2;
            }
            if (player.motionY < maxSpeed) {
                player.motionY += accel;
                if (player.motionY > maxSpeed) {
                    player.motionY = maxSpeed;
                }
            }
        }
        return null;
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
        return "Rise quickly underwater";
    }

    @Override
    public void addInformation(ItemStack is, List list) {
        list.add("Exo-Upgrade");
        Core.brand(list);
    }
}