package factorization.common;

import java.util.List;
import java.util.Random;

import net.minecraft.src.DamageSource;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.EnumArmorMaterial;
import net.minecraft.src.Item;
import net.minecraft.src.ItemArmor;
import net.minecraft.src.ItemStack;
import net.minecraft.src.NBTTagCompound;
import net.minecraftforge.common.ISpecialArmor;
import factorization.api.IMechaUpgrade;
import factorization.api.MechaStateShader;
import factorization.api.MechaStateType;
import factorization.common.MechaCore.MechaPlayerState;

public class MechaArmor extends ItemArmor
        implements ISpecialArmor {
    public int slotCount = 2;
    public MechaArmor(int par1, int armorType) {
        super(par1, EnumArmorMaterial.CHAIN, 0, armorType);
        setMaxDamage(0); //never break!
        setItemName("item.mechaArmor" + armorType);
    }

    //mecha features
    MechaArmor setSlotCount(int count) {
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

    public IMechaUpgrade getUpgradeInSlot(ItemStack is, int slot) {
        return getUpgrade(getStackInSlot(is, slot));
    }

    public IMechaUpgrade getUpgrade(ItemStack i) {
        if (i == null || !(i.getItem() instanceof IMechaUpgrade)) {
            return null;
        }
        if (i.stackSize == 0) {
            return null;
        }
        return (IMechaUpgrade) i.getItem();
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

    public boolean isValidUpgrade(ItemStack is) {
        if (is == null) {
            return false;
        }
        Item item = is.getItem();
        if (item instanceof IMechaUpgrade) {
            return true;
        }
        if (is.getItem().getClass() == ItemArmor.class) {
            if (((ItemArmor) is.getItem()).armorType == armorType) {
                return true;
            }
        }
        return false;
    }

    public void setMechaStateType(ItemStack is, int slot, MechaStateType mst) {
        NBTTagCompound tag = FactorizationUtil.getTag(is);
        tag.setInteger("MST" + slot, mst.ordinal());
    }
    
    public MechaStateType getMechaStateType(ItemStack is, int slot) {
        NBTTagCompound tag = FactorizationUtil.getTag(is);
        int typeOrdinal = tag.getInteger("MST" + slot);
        try {
            return MechaStateType.values()[typeOrdinal];
        } catch (ArrayIndexOutOfBoundsException e) {
            setMechaStateType(is, slot, MechaStateType.NEVER);
            return MechaStateType.NEVER;
        }
    }
    
    public void setMechaStateShader(ItemStack is, int slot, MechaStateShader mss) {
        NBTTagCompound tag = FactorizationUtil.getTag(is);
        tag.setInteger("MSS" + slot, mss.ordinal());
    }
    
    public MechaStateShader getMechaStateShader(ItemStack is, int slot) {
        NBTTagCompound tag = FactorizationUtil.getTag(is);
        int typeOrdinal = tag.getInteger("MSS" + slot);
        try {
            return MechaStateShader.values()[typeOrdinal];
        } catch (ArrayIndexOutOfBoundsException e) {
            setMechaStateShader(is, slot, MechaStateShader.NORMAL);
            return MechaStateShader.NORMAL;
        }
    }

    static void onTickPlayer(EntityPlayer player, MechaPlayerState mps) {
        for (ItemStack armorStack : player.inventory.armorInventory) {
            if (armorStack == null) {
                continue;
            }
            if (armorStack.getItem() instanceof MechaArmor) {
                ((MechaArmor) armorStack.getItem()).tickArmor(player, mps, armorStack);
            }
        }
    }

    void tickArmor(EntityPlayer player, MechaPlayerState mps, ItemStack armorStack) {
        for (int slot = 0; slot < slotCount; slot++) {
            ItemStack is = getStackInSlot(armorStack, slot);
            if (is == null) {
                continue;
            }
            IMechaUpgrade up = getUpgrade(is);
            if (up != null) {
                MechaStateShader mss = getMechaStateShader(armorStack, slot);
                MechaStateType mst = getMechaStateType(armorStack, slot);
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
        //For now, the client uses render.MechaArmorTextured
        if (armorType == 2) {
            return Core.texture_dir + "mecha_armor_2.png";
        }
        return Core.texture_dir + "mecha_armor_1.png";
    }

    @Override
    public boolean getShareTag() {
        return true;
    }

    @Override
    public ArmorProperties getProperties(EntityLiving player, ItemStack armor, DamageSource source,
            double damage, int slot) {
        ArmorProperties prop = new ArmorProperties(0, 0, 0);
        MechaArmor ma = (MechaArmor) armor.getItem();
        boolean found_vanilla_armor = false;
        for (int i = 0; i < slotCount; i++) {
            ItemStack is = ma.getStackInSlot(armor, i);
            if (is == null) {
                continue;
            }
            if (is.getItem().getClass() == ItemArmor.class) {
                if (found_vanilla_armor) {
                    continue;
                }
                found_vanilla_armor = true;
                ItemArmor ar = (ItemArmor) is.getItem();
                prop.AbsorbRatio += ar.damageReduceAmount / 25D;
                prop.AbsorbMax += ar.getMaxDamage() + 1 - is.getItemDamage();
            }
            IMechaUpgrade up = getUpgrade(is);
            if (up != null) {
                up.addArmorProperties(is, prop);
            }
        }
        return prop;
    }

    @Override
    public int getArmorDisplay(EntityPlayer player, ItemStack armor, int slot) {
        int ret = 0;
        MechaArmor ma = (MechaArmor) armor.getItem();
        boolean found_vanilla_armor = false;
        for (int i = 0; i < slotCount; i++) {
            ItemStack is = ma.getStackInSlot(armor, i);
            if (is == null) {
                continue;
            }
            if (is.getItem().getClass() == ItemArmor.class) {
                if (found_vanilla_armor) {
                    continue;
                }
                found_vanilla_armor = true;
                ret += ((ItemArmor) is.getItem()).damageReduceAmount;
                continue;
            }
            IMechaUpgrade up = getUpgrade(is);
            if (up != null) {
                ret += up.getArmorDisplay(is);
            }
        }
        return ret;
    }

    @Override
    public void damageArmor(EntityLiving entity, ItemStack armor, DamageSource source, int damage,
            int slot) {
        MechaArmor ma = (MechaArmor) armor.getItem();
        boolean found_vanilla_armor = false;
        for (int i = 0; i < slotCount; i++) {
            ItemStack is = ma.getStackInSlot(armor, i);
            if (is == null) {
                continue;
            }
            if (is.getItem().getClass() == ItemArmor.class) {
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
            IMechaUpgrade up = getUpgrade(is);
            if (up != null) {
                if (up.damageArmor(entity, is, source, damage, slot)) {
                    ma.setStackInSlot(armor, i, is);
                }
            }
        }
    }

    @Override
    public String getTextureFile() {
        return Core.texture_file_item;
    }

    @Override
    //seeerveerr
    public int getIconFromDamage(int par1) {
        return (4 + armorType) * 16;
    }

    @Override
    public void addInformation(ItemStack is, List infoList) {
        super.addInformation(is, infoList);
        for (int i = 0; i < slotCount; i++) {
            ItemStack upgrade = getStackInSlot(is, i);
            if (upgrade == null) {
                continue;
            }
            String s = upgrade.getItem().getItemDisplayName(upgrade);
            if (s != null && s.length() > 0) {
                MechaStateType mst = getMechaStateType(is, i);
                MechaStateShader mss = getMechaStateShader(is, i);
                s += "  " + mss.brief() + mst.brief();
                infoList.add(s);
            }
        }
        Core.brand(infoList);
    }
}
