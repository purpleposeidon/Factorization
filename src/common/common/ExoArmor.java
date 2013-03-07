package factorization.common;

import java.util.List;
import java.util.Random;

import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumArmorMaterial;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Icon;
import net.minecraftforge.common.IArmorTextureProvider;
import net.minecraftforge.common.ISpecialArmor;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.ExoStateShader;
import factorization.api.ExoStateType;
import factorization.api.IExoUpgrade;
import factorization.common.ExoCore.ExoPlayerState;

public class ExoArmor extends ItemArmor
        implements ISpecialArmor, IArmorTextureProvider {
    public int slotCount = 2;
    public ExoArmor(int par1, int armorType) {
        super(par1, EnumArmorMaterial.CHAIN, 0, armorType);
        setMaxDamage(0); //never break!
        setUnlocalizedName("item.exoArmor" + armorType);
    }

    //exo features
    ExoArmor setSlotCount(int count) {
        slotCount = count;
        return this;
    }

    public ItemStack getStackInSlot(ItemStack is, int slot) {
        if (slot < 0 || slot >= slotCount) {
            return null;
        }
        NBTTagCompound tag = is.getTagCompound();
        if (tag == null) {
            return null;
        }
        String index = "slot" + slot;
        if (!tag.hasKey(index)) {
            return null;
        }
        return ItemStack.loadItemStackFromNBT(tag.getCompoundTag(index));
    }

    public IExoUpgrade getUpgradeInSlot(ItemStack is, int slot) {
        return getUpgrade(getStackInSlot(is, slot));
    }

    public IExoUpgrade getUpgrade(ItemStack i) {
        if (i == null || !(i.getItem() instanceof IExoUpgrade)) {
            return null;
        }
        if (i.stackSize == 0) {
            return null;
        }
        return (IExoUpgrade) i.getItem();
    }

    public void setStackInSlot(ItemStack is, int slot, ItemStack stack) {
        if (slot < 0 || slot >= slotCount) {
            return;
        }
        if (is.getTagCompound() == null) {
            is.setTagCompound(new NBTTagCompound());
        }
        if (stack == null) {
            is.getTagCompound().setTag("slot" + slot, new NBTTagCompound());
            return;
        }
        NBTTagCompound itemTag = new NBTTagCompound();
        stack.writeToNBT(itemTag);
        is.getTagCompound().setCompoundTag("slot" + slot, itemTag);
    }

    public boolean isValidUpgrade(ItemStack theArmor, ItemStack is) {
        if (is == null) {
            return false;
        }
        Item item = is.getItem();
        if (item instanceof IExoUpgrade) {
            IExoUpgrade upgrade = (IExoUpgrade) is.getItem();
            return upgrade.canUpgradeArmor(theArmor, this.armorType);
        }
        if (isStandardArmor(is)
                && ((ItemArmor) is.getItem()).armorType == this.armorType) {
            return true;
        }
        return false;
    }
    
    public static boolean isStandardArmor(ItemStack is) {
        if (is == null) {
            return false;
        }
        Item it = is.getItem();
        return it instanceof ItemArmor && !(it instanceof ISpecialArmor);
    }

    public void setExoStateType(ItemStack is, int slot, ExoStateType mst) {
        NBTTagCompound tag = FactorizationUtil.getTag(is);
        tag.setInteger("MST" + slot, mst.ordinal());
    }
    
    public ExoStateType getExoStateType(ItemStack is, int slot) {
        NBTTagCompound tag = FactorizationUtil.getTag(is);
        int typeOrdinal = tag.getInteger("MST" + slot);
        try {
            return ExoStateType.values()[typeOrdinal];
        } catch (ArrayIndexOutOfBoundsException e) {
            setExoStateType(is, slot, ExoStateType.NEVER);
            return ExoStateType.NEVER;
        }
    }
    
    public void setExoStateShader(ItemStack is, int slot, ExoStateShader mss) {
        NBTTagCompound tag = FactorizationUtil.getTag(is);
        tag.setInteger("MSS" + slot, mss.ordinal());
    }
    
    public ExoStateShader getExoStateShader(ItemStack is, int slot) {
        NBTTagCompound tag = FactorizationUtil.getTag(is);
        int typeOrdinal = tag.getInteger("MSS" + slot);
        try {
            return ExoStateShader.values()[typeOrdinal];
        } catch (ArrayIndexOutOfBoundsException e) {
            setExoStateShader(is, slot, ExoStateShader.NORMAL);
            return ExoStateShader.NORMAL;
        }
    }

    static void onTickPlayer(EntityPlayer player, ExoPlayerState mps) {
        //TODO: Forge has added armor ticking (Would need to update)
        for (ItemStack armorStack : player.inventory.armorInventory) {
            if (armorStack == null) {
                continue;
            }
            if (armorStack.getItem() instanceof ExoArmor) {
                ((ExoArmor) armorStack.getItem()).tickArmor(player, mps, armorStack);
            }
        }
    }

    void tickArmor(EntityPlayer player, ExoPlayerState mps, ItemStack armorStack) {
        for (int slot = 0; slot < slotCount; slot++) {
            ItemStack is = getStackInSlot(armorStack, slot);
            if (is == null) {
                continue;
            }
            IExoUpgrade up = getUpgrade(is);
            if (up != null) {
                ExoStateShader mss = getExoStateShader(armorStack, slot);
                ExoStateType mst = getExoStateType(armorStack, slot);
                boolean active = mps.getIsActive(mst, mss);
                //System.out.println(mst.when(mss) + " = " + active);
                ItemStack ret = up.tickUpgrade(player, armorStack, is, active);
                if (ret == null) {
                    continue;
                }
                setStackInSlot(armorStack, slot, FactorizationUtil.normalize(ret));
            }
        }
    }

    static Random rand = new Random();

    double randDelta() {
        return (rand.nextGaussian() - 0.5) / 4;
    }

    //vanilla armor feature
    @Override
    public int getItemEnchantability() {
        return -1;
    }

    public String getArmorTextureFile(ItemStack itemstack) {
        //presumably we'll have to change this depending on what type of armor we are
        //XXX NOTE: LexManos needs to put IArmorTextureProvider in common
        //For now, the client uses render.ExoArmorTextured
        if (armorType == 2) {
            return Core.texture_dir + "exo_armor_2.png";
        }
        return Core.texture_dir + "exo_armor_1.png";
    }

    @Override
    public boolean getShareTag() {
        return true;
    }

    @Override
    public ArmorProperties getProperties(EntityLiving player, ItemStack armor, DamageSource source,
            double damage, int slot) {
        ArmorProperties prop = new ArmorProperties(0, 0, 0);
        ExoArmor ma = (ExoArmor) armor.getItem();
        boolean found_vanilla_armor = false;
        for (int i = 0; i < slotCount; i++) {
            ItemStack is = ma.getStackInSlot(armor, i);
            if (is == null) {
                continue;
            }
            if (isStandardArmor(is)) {
                if (found_vanilla_armor) {
                    continue;
                }
                found_vanilla_armor = true;
                ItemArmor ar = (ItemArmor) is.getItem();
                prop.AbsorbRatio += ar.damageReduceAmount / 25D;
                prop.AbsorbMax += ar.getMaxDamage() + 1 - is.getItemDamage();
            }
            IExoUpgrade up = getUpgrade(is);
            if (up != null) {
                up.addArmorProperties(is, prop);
            }
        }
        return prop;
    }

    @Override
    public int getArmorDisplay(EntityPlayer player, ItemStack armor, int slot) {
        int ret = 0;
        ExoArmor ma = (ExoArmor) armor.getItem();
        boolean found_vanilla_armor = false;
        for (int i = 0; i < slotCount; i++) {
            ItemStack is = ma.getStackInSlot(armor, i);
            if (is == null) {
                continue;
            }
            if (isStandardArmor(is)) {
                if (found_vanilla_armor) {
                    continue;
                }
                found_vanilla_armor = true;
                ret += ((ItemArmor) is.getItem()).damageReduceAmount;
                continue;
            }
            IExoUpgrade up = getUpgrade(is);
            if (up != null) {
                ret += up.getArmorDisplay(is);
            }
        }
        return ret;
    }

    @Override
    public void damageArmor(EntityLiving entity, ItemStack armor, DamageSource source, int damage,
            int slot) {
        ExoArmor ma = (ExoArmor) armor.getItem();
        boolean found_vanilla_armor = false;
        for (int i = 0; i < slotCount; i++) {
            ItemStack is = ma.getStackInSlot(armor, i);
            if (is == null) {
                continue;
            }
            if (isStandardArmor(is)) {
                if (found_vanilla_armor) {
                    continue;
                }
                found_vanilla_armor = true;
                is.damageItem(damage, entity);
                if (is.stackSize <= 0) {
                    is = null;
                }
                ma.setStackInSlot(armor, i, is);
                continue;
            }
            IExoUpgrade up = getUpgrade(is);
            if (up != null) {
                if (up.damageArmor(entity, is, source, damage, slot)) {
                    ma.setStackInSlot(armor, i, is);
                }
            }
        }
    }

    @SideOnly(Side.CLIENT)
    FzIcon[] parts = new FzIcon[] {
        new FzIcon("exo/helmet.png"),
        new FzIcon("exo/chest.png"),
        new FzIcon("exo/pants.png"),
        new FzIcon("exo/boot.png")
    };
    
    @Override
    public Icon getIconFromDamage(int par1) {
        return parts[armorType];
    }
    
    @Override
    public void registerIcon(IconRegister reg) {
        FzIcon.registerNew(reg);
    }

    @Override
    public void addInformation(ItemStack is, EntityPlayer player, List infoList, boolean verbose) {
        super.addInformation(is, player, infoList, verbose);
        for (int i = 0; i < slotCount; i++) {
            ItemStack upgrade = getStackInSlot(is, i);
            if (upgrade == null) {
                continue;
            }
            String s = upgrade.getItem().getItemDisplayName(upgrade);
            if (s != null && s.length() > 0) {
                if (upgrade.getItem() instanceof IExoUpgrade) {
                    ExoStateType mst = getExoStateType(is, i);
                    ExoStateShader mss = getExoStateShader(is, i);
                    s += "  " + mss.brief() + mst.brief();
                }
                infoList.add(s);
            }
        }
        Core.brand(infoList);
    }
}
