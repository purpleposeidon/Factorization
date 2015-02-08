package factorization.ceramics;

import java.util.ArrayList;
import java.util.List;

import factorization.shared.*;
import factorization.util.DataUtil;
import factorization.util.ItemUtil;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Coord;
import factorization.ceramics.TileEntityGreenware.ClayLump;
import factorization.ceramics.TileEntityGreenware.ClayState;
import factorization.common.BlockIcons;
import factorization.notify.Notice;
import factorization.shared.Core.TabType;

public class ItemGlazeBucket extends ItemFactorization {
    public static final int MAX_CHARGES = 64;
    
    public ItemGlazeBucket() {
        super("ceramics/glaze_bucket", TabType.ART);
        setMaxStackSize(1);
        setMaxDamage(0);
        setNoRepair();
        Core.tab(this, TabType.ART);
    }
    
    @Override
    public IIcon getIcon(ItemStack is, int renderPass, EntityPlayer player, ItemStack usingItem, int useRemaining) {
        if (renderPass == 0) {
            return getIconFromDamage(0);
        }
        Block block = getBlockId(is);
        if (block == null) {
            return BlockIcons.uv_test;
            //Or could return the error icon.
            //But I think this'll look less terribly awful if a block goes away.
        }
        try {
            int side = getBlockSide(is);
            if (side == -1) side = 1;
            return block.getIcon(side, getBlockMd(is));
        } catch (Throwable t) {
            if (!spammed) {
                t.printStackTrace();
                spammed = true;
            }
            return BlockIcons.error;
        }
    }
    
    private boolean spammed = false;
    
    @Override
    public String getUnlocalizedName(ItemStack is) {
        String base = super.getUnlocalizedName(is);
        NBTTagCompound tag = ItemUtil.getTag(is);
        if (tag.hasKey("gid")) {
            String key = tag.getString("gid");
            return base + "." + key;
        }
        return base;
    }
    
    @Override
    public String getItemStackDisplayName(ItemStack is) {
        String base = super.getItemStackDisplayName(is);
        if (isMimic(is)) {
            ItemStack hint = getSource(is);
            if (hint != null && hint.getItem() != null) {
                return Core.translate(getUnlocalizedName() + ".mimicry_prefix") + " " + hint.getDisplayName();
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
        NBTTagCompound tag = ItemUtil.getTag(is);
        return tag.getInteger("remaining");
    }
    
    public boolean useCharge(ItemStack is) {
        NBTTagCompound tag = ItemUtil.getTag(is);
        int remaining = tag.getInteger("remaining");
        if (remaining <= 0) {
            return false;
        }
        remaining--;
        tag.setInteger("remaining", remaining);
        return remaining > 0;
    }
    
    private Block getBlockId(ItemStack is) {
        return DataUtil.getBlock(ItemUtil.getTag(is).getShort("bid"));
    }
    
    private byte getBlockMd(ItemStack is) {
        return ItemUtil.getTag(is).getByte("bmd");
    }
    
    private byte getBlockSide(ItemStack is) {
        return ItemUtil.getTag(is).getByte("bsd");
    }
    
    private boolean isMimic(ItemStack is) {
        return ItemUtil.getTag(is).getBoolean("mimic");
    }
    
    private ArrayList<ItemStack> subItems = new ArrayList<ItemStack>();
    private boolean done;
    
    @Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item itemId, CreativeTabs tab, List list) {
        list.addAll(subItems);
    }
    
    public void doneMakingStandardGlazes() {
        done = true;
    }
    
    public void notDoneMakingStandardGlazes() {
        done = false;
    }
    
    public void addGlaze(ItemStack is) {
        if (subItems.isEmpty()) {
            subItems.add(Core.registry.empty_glaze_bucket.copy());
        }
        if (!done) {
            subItems.add(is);
        }
    }
    
    void setGid(ItemStack is, String unique_id) {
        ItemUtil.getTag(is).setString("gid", unique_id);
    }
    
    int md_for_nei = 0;
    
    public ItemStack makeCraftingGlaze(String unique_id) {
        ItemStack is = new ItemStack(this);
        NBTTagCompound tag = ItemUtil.getTag(is);
        tag.setBoolean("fake", true);
        setGid(is, unique_id);
        is.setItemDamage(md_for_nei++);
        return is;
    }
    
    private ItemStack makeGlazeWith(Block id, int md, int side) {
        ItemStack is = new ItemStack(this);
        NBTTagCompound tag = ItemUtil.getTag(is);
        tag.setShort("bid", (short) DataUtil.getId(id));
        tag.setByte("bmd", (byte)md);
        tag.setByte("bsd", (byte)side);
        tag.setInteger("remaining", MAX_CHARGES);
        addGlaze(is);
        return is;
    }
    
    public ItemStack makeMimicingGlaze(Block id, int md, int side) {
        ItemStack is = makeGlazeWith(id, md, side);
        setMimicry(is);
        return is;
    }
    
    public void setMimicry(ItemStack is) {
        NBTTagCompound tag = ItemUtil.getTag(is);
        tag.setBoolean("mimic", true);
    }
    
    public boolean isUsable(ItemStack is) {
        if (ItemUtil.getTag(is).getBoolean("fake")) {
            return false;
        }
        return getBlockId(is) != null;
    }
    
    ItemStack getSource(ItemStack is) {
        Block b = getBlockId(is);
        if (b ==  null) {
            return null;
        }
        return new ItemStack(b, 1, getBlockMd(is));
    }

    @Override
    public ItemStack onItemRightClick(ItemStack is, World w, EntityPlayer player) {
        if (w.isRemote) {
            return is;
        }
        MovingObjectPosition mop = getMovingObjectPositionFromPlayer(w, player, true);
        if (!isUsable(is)) {
            return is;
        }
        if (mop == null) {
            return is;
        }
        if (mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || mop.subHit == -1) {
            return is;
        }
        TileEntityGreenware clay = (new Coord(w, mop.blockX, mop.blockY, mop.blockZ)).getTE(TileEntityGreenware.class);
        if (clay == null) {
            return is;
        }
        ClayState state = clay.getState();
        ClayLump part = clay.parts.get(mop.subHit);
        boolean repairMissingBlock = part.icon_id == null || part.icon_id == Blocks.air || (part.icon_id == Core.registry.resource_block && part.icon_md > 0xF);
        if (player.capabilities.isCreativeMode) {
            if (state != ClayState.HIGHFIRED) {
                clay.totalHeat = TileEntityGreenware.highfireHeat + 1;
            }
        } else {
            switch (state) {
            case WET:
            case DRY:
                new Notice(clay, "Use a {ITEM_NAME} to bisque").withItem(Core.registry.heater_item).send(player);
                return is;
            case HIGHFIRED:
                if (!repairMissingBlock) {
                    new Notice(clay, "Already high-fired").send(player);
                    return is;
                }
            default: break;
            }
        }
        Block id = getBlockId(is);
        byte md = getBlockMd(is);
        byte sd = getBlockSide(is);
        if (part.icon_id == id && part.icon_md == md && part.icon_side == sd) {
            return is;
        }
        if (getCharges(is) > 0) {
            part.icon_id = id;
            part.icon_md = md;
            part.icon_side = sd;
            clay.changeLump(mop.subHit, part);
            clay.glazesApplied = true;
            if (!player.capabilities.isCreativeMode && !repairMissingBlock) {
                useCharge(is);
                if (getCharges(is) <= 0) {
                    return Core.registry.empty_glaze_bucket.copy();
                }
            }
        }
        return is;
    }
}
