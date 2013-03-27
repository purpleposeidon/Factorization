package factorization.common;

import java.util.List;

import factorization.common.Core.TabType;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Icon;

public class ItemGlazeBucket extends Item {
    public static final int MAX_CHARGES = 32;
    
    public ItemGlazeBucket(int itemId) {
        super(itemId);
        setUnlocalizedName("factorization:ceramics/glaze_bucket");
        setMaxStackSize(1);
        setMaxDamage(0);
        setNoRepair();
        Core.tab(this, TabType.ART);
    }
    
    @Override
    public Icon getIcon(ItemStack is, int renderPass, EntityPlayer player, ItemStack usingItem, int useRemaining) {
        if (renderPass == 0) {
            return getIconFromDamage(0);
        }
        int id = getBlockId(is);
        Block block = Block.blocksList[id];
        if (block == null) {
            return BlockIcons.uv_test; //NORELEASE
            //return BlockIcons.transparent;
        }
        return block.getBlockTextureFromSideAndMetadata(getBlockSide(is), getBlockMd(is));
    }
    
    @Override
    public String getUnlocalizedName(ItemStack is) {
        String base = super.getUnlocalizedName(is);
        NBTTagCompound tag = FactorizationUtil.getTag(is);
        if (tag.hasKey("id")) {
            String key = tag.getString("id");
            return base + "." + key;
        }
        return base;
    }
    
    @Override
    public String getItemDisplayName(ItemStack is) {
        String base = super.getItemDisplayName(is);
        if (isMimic(is)) {
            ItemStack hint = getSource(is);
            if (hint != null) {
                return base + ": " + hint.getDisplayName();
            }
        }
        return base;
    }
    
    public float getFullness(ItemStack is) {
        int c = getCharges(is);
        if (c >= 32) {
            return 1F;
        }
        if (c <= 0) {
            return 0F;
        }
        return c/(float)MAX_CHARGES;
    }
    
    public int getCharges(ItemStack is) {
        NBTTagCompound tag = FactorizationUtil.getTag(is);
        return tag.getInteger("remaining");
    }
    
    public boolean useCharge(ItemStack is) {
        NBTTagCompound tag = FactorizationUtil.getTag(is);
        int remaining = tag.getInteger("remaining");
        if (remaining <= 0) {
            return false;
        }
        remaining--;
        return true;
    }
    
    private int getBlock(ItemStack is, String key) {
        return FactorizationUtil.getTag(is).getInteger(key);
    }
    
    private int getBlockId(ItemStack is) {
        return FactorizationUtil.getTag(is).getInteger("bid");
    }
    
    private int getBlockMd(ItemStack is) {
        return FactorizationUtil.getTag(is).getInteger("bmd");
    }
    
    private int getBlockSide(ItemStack is) {
        return FactorizationUtil.getTag(is).getInteger("bsd");
    }
    
    private boolean isMimic(ItemStack is) {
        return FactorizationUtil.getTag(is).getBoolean("mimic");
    }
    
    public ItemStack makeCraftingGlaze(String unique_id) {
        ItemStack is = new ItemStack(this);
        NBTTagCompound tag = FactorizationUtil.getTag(is);
        tag.setBoolean("fake", true);
        tag.setString("id", unique_id);
        return is;
    }
    
    public ItemStack makeBasicGlaze(int id, int md, int side) {
        ItemStack is = new ItemStack(this);
        NBTTagCompound tag = FactorizationUtil.getTag(is);
        tag.setInteger("bid", id);
        tag.setInteger("bmd", md);
        tag.setInteger("bsd", side);
        tag.setInteger("remaining", MAX_CHARGES);
        return is;
    }
    
    public void setMimicry(ItemStack is) {
        NBTTagCompound tag = FactorizationUtil.getTag(is);
        tag.setBoolean("mimic", true);
    }
    
    public boolean isUsable(ItemStack is) {
        if (FactorizationUtil.getTag(is).getBoolean("fake")) {
            return false;
        }
        return Block.blocksList[getBlockId(is)] != null;
    }
    
    ItemStack getSource(ItemStack is) {
        Block b = Block.blocksList[getBlockId(is)];
        if (b ==  null) {
            return null;
        }
        return new ItemStack(b, 1, getBlockMd(is));
    }
    
    @Override
    public void addInformation(ItemStack is, EntityPlayer player, List list, boolean verbose) {
        ItemStack hint = getSource(is);
        if (hint != null) {
            hint.getItem().addInformation(hint, player, list, verbose);
        }
        Core.brand(list);
    }

}
