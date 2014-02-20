package factorization.ceramics;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Coord;
import factorization.ceramics.TileEntityGreenware.ClayLump;
import factorization.ceramics.TileEntityGreenware.ClayState;
import factorization.common.BlockIcons;
import factorization.notify.Notify;
import factorization.shared.Core;
import factorization.shared.Core.TabType;
import factorization.shared.FzUtil;
import factorization.shared.ItemFactorization;

public class ItemGlazeBucket extends ItemFactorization {
    public static final int MAX_CHARGES = 64;
    
    public ItemGlazeBucket(int itemId) {
        super(itemId, "ceramics/glaze_bucket", TabType.ART);
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
        int id = getBlockId(is);
        Block block = id;
        if (block == null) {
            return BlockIcons.uv_test;
            //Or could return the error icon.
            //But I think this'll look less terribly awful if a block goes away.
        }
        return block.getIcon(getBlockSide(is), getBlockMd(is));
    }
    
    @Override
    public String getUnlocalizedName(ItemStack is) {
        String base = super.getUnlocalizedName(is);
        NBTTagCompound tag = FzUtil.getTag(is);
        if (tag.hasKey("gid")) {
            String key = tag.getString("gid");
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
        NBTTagCompound tag = FzUtil.getTag(is);
        return tag.getInteger("remaining");
    }
    
    public boolean useCharge(ItemStack is) {
        NBTTagCompound tag = FzUtil.getTag(is);
        int remaining = tag.getInteger("remaining");
        if (remaining <= 0) {
            return false;
        }
        remaining--;
        tag.setInteger("remaining", remaining);
        return remaining > 0;
    }
    
    private short getBlockId(ItemStack is) {
        return FzUtil.getTag(is).getShort("bid");
    }
    
    private byte getBlockMd(ItemStack is) {
        return FzUtil.getTag(is).getByte("bmd");
    }
    
    private byte getBlockSide(ItemStack is) {
        return FzUtil.getTag(is).getByte("bsd");
    }
    
    private boolean isMimic(ItemStack is) {
        return FzUtil.getTag(is).getBoolean("mimic");
    }
    
    private ArrayList<ItemStack> subItems = new ArrayList<ItemStack>();
    private boolean done;
    
    @Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(int itemId, CreativeTabs tab, List list) {
        list.addAll(subItems);
    }
    
    public void doneMakingStandardGlazes() {
        done = true;
    }
    
    public void notDoneMakingStandardGlazes() {
        done = false;
    }
    
    public void addGlaze(ItemStack is) {
        if (!done) {
            subItems.add(is);
        }
    }
    
    void setGid(ItemStack is, String unique_id) {
        FzUtil.getTag(is).setString("gid", unique_id);
    }
    
    int md_for_nei = 0;
    
    public ItemStack makeCraftingGlaze(String unique_id) {
        ItemStack is = new ItemStack(this);
        NBTTagCompound tag = FzUtil.getTag(is);
        tag.setBoolean("fake", true);
        setGid(is, unique_id);
        is.setItemDamage(md_for_nei++);
        //add(is);
        return is;
    }
    
    private ItemStack makeGlazeWith(Block id, int md, int side) {
        ItemStack is = new ItemStack(this);
        NBTTagCompound tag = FzUtil.getTag(is);
        tag.setShort("bid", (short)id);
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
    
    public ItemStack make(BasicGlazes glaze) {
        ItemStack is = makeGlazeWith(Core.registry.resource_block, glaze.metadata, 0);
        setGid(is, glaze.name());
        is.setItemDamage(md_for_nei++);
        return is;
    }
    
    public void setMimicry(ItemStack is) {
        NBTTagCompound tag = FzUtil.getTag(is);
        tag.setBoolean("mimic", true);
    }
    
    public boolean isUsable(ItemStack is) {
        if (FzUtil.getTag(is).getBoolean("fake")) {
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
        if (player.capabilities.isCreativeMode) {
            if (state != ClayState.HIGHFIRED) {
                clay.totalHeat = TileEntityGreenware.highfireHeat + 1;
            }
        } else {
            switch (state) {
            case WET:
            case DRY:
                Notify.withItem(Core.registry.heater_item);
                Notify.send(player, clay.getCoord(), "Use a {ITEM_NAME} to bisque");
                return is;
            case HIGHFIRED:
                Notify.send(player, clay.getCoord(), "Already high-fired");
                return is;
            default: break;
            }
        }
        ClayLump part = clay.parts.get(mop.subHit);
        short id = getBlockId(is);
        byte md = getBlockMd(is);
        byte sd = getBlockSide(is);
        if (part.icon_id == id && part.icon_md == md && part.icon_side == sd) {
            return is;
        }
        if (player.capabilities.isCreativeMode || useCharge(is)) {
            part.icon_id = id;
            part.icon_md = md;
            part.icon_side = sd;
            clay.changeLump(mop.subHit, part);
            clay.glazesApplied = true;
            return is;
        } else {
            return new ItemStack(this);
        }
    }
}
