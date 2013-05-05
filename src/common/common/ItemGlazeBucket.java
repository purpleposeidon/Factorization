package factorization.common;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumMovingObjectType;
import net.minecraft.util.Icon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Coord;
import factorization.common.Core.TabType;
import factorization.common.TileEntityGreenware.ClayLump;
import factorization.common.TileEntityGreenware.ClayState;

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
            return BlockIcons.uv_test;
            //Or could return the error icon.
            //But I think this'll look less terribly awful if a block goes away.
        }
        return block.getIcon(getBlockSide(is), getBlockMd(is));
    }
    
    @Override
    public String getUnlocalizedName(ItemStack is) {
        String base = super.getUnlocalizedName(is);
        NBTTagCompound tag = FactorizationUtil.getTag(is);
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
        tag.setInteger("remaining", remaining);
        return remaining > 0;
    }
    
    private short getBlockId(ItemStack is) {
        return FactorizationUtil.getTag(is).getShort("bid");
    }
    
    private byte getBlockMd(ItemStack is) {
        return FactorizationUtil.getTag(is).getByte("bmd");
    }
    
    private byte getBlockSide(ItemStack is) {
        return FactorizationUtil.getTag(is).getByte("bsd");
    }
    
    private boolean isMimic(ItemStack is) {
        return FactorizationUtil.getTag(is).getBoolean("mimic");
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
    
    void add(ItemStack is) {
        if (!done) {
            subItems.add(is);
        }
    }
    
    void setGid(ItemStack is, String unique_id) {
        FactorizationUtil.getTag(is).setString("gid", unique_id);
    }
    
    public ItemStack makeCraftingGlaze(String unique_id) {
        ItemStack is = new ItemStack(this);
        NBTTagCompound tag = FactorizationUtil.getTag(is);
        tag.setBoolean("fake", true);
        setGid(is, unique_id);
        //add(is);
        return is;
    }
    
    private ItemStack makeGlazeWith(int id, int md, int side) {
        ItemStack is = new ItemStack(this);
        NBTTagCompound tag = FactorizationUtil.getTag(is);
        tag.setShort("bid", (short)id);
        tag.setByte("bmd", (byte)md);
        tag.setByte("bsd", (byte)side);
        tag.setInteger("remaining", MAX_CHARGES);
        add(is);
        return is;
    }
    
    public ItemStack makeMimicingGlaze(int id, int md, int side) {
        ItemStack is = makeGlazeWith(id, md, side);
        setMimicry(is);
        return is;
    }
    
    public ItemStack make(BasicGlazes glaze) {
        ItemStack is = makeGlazeWith(Core.registry.resource_block.blockID, glaze.metadata, 0);
        setGid(is, glaze.name());
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
        NBTTagCompound tag = FactorizationUtil.getTag(is);
        if (!tag.hasNoTags()) {
            list.add("Ceramic Glaze");
        }
        if (hint != null) {
            Item hi = hint.getItem();
            hi.addInformation(hint, player, list, verbose);
            if (hi == Item.itemsList[Core.registry.resource_block.blockID]) {
                return; //No Core.brand
            }
        }
        Core.brand(is, list);
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
        if (mop.typeOfHit != EnumMovingObjectType.TILE || mop.subHit == -1) {
            return is;
        }
        TileEntityGreenware clay = (new Coord(w, mop.blockX, mop.blockY, mop.blockZ)).getTE(TileEntityGreenware.class);
        if (clay == null) {
            return is;
        }
        ClayState state = clay.getState();
        switch (state) {
        case WET:
        case DRY:
            Core.notify(player, clay.getCoord(), "Use a Furnace Heater");
            return is;
        case HIGHFIRED:
            Core.notify(player, clay.getCoord(), "Already high-fired");
            return is;
        }
        if (useCharge(is)) {
            ClayLump part = clay.parts.get(mop.subHit);
            part.icon_id = getBlockId(is);
            part.icon_md = getBlockMd(is);
            clay.changeLump(mop.subHit, part);
            clay.glazesApplied = true;
            return is;
        } else {
            return new ItemStack(this);
        }
    }
}
