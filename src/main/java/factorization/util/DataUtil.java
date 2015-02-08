package factorization.util;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StringUtils;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;

import java.io.DataInput;
import java.io.IOException;

public final class DataUtil {

    public static void writeTank(NBTTagCompound tag, FluidTank tank, String name) {
        FluidStack ls = tank.getFluid();
        if (ls == null) {
            return;
        }
        NBTTagCompound liquid_tag = new NBTTagCompound();
        ls.writeToNBT(liquid_tag);
        tag.setTag(name, liquid_tag);
    }

    public static void readTank(NBTTagCompound tag, FluidTank tank, String name) {
        NBTTagCompound liquid_tag = tag.getCompoundTag(name);
        FluidStack ls = FluidStack.loadFluidStackFromNBT(liquid_tag);
        tank.setFluid(ls);
    }

    static public NBTTagCompound readTag(DataInput input, NBTSizeTracker tracker) throws IOException {
        return CompressedStreamTools.func_152456_a(input, tracker);
    }

    static public ItemStack readStack(DataInput input, NBTSizeTracker tracker) throws IOException {
        ItemStack is = ItemStack.loadItemStackFromNBT(readTag(input, tracker));
        if (is == null || is.getItem() == null) {
            return null;
        }
        return is;
    }

    @Deprecated // Provide an NBTSizeTracker!
    static public NBTTagCompound readTag(DataInput input) throws IOException {
        return readTag(input, NBTSizeTracker.field_152451_a);
    }

    @Deprecated // Provide an NBTSizeTracker!
    static public ItemStack readStack(DataInput input) throws IOException {
        return readStack(input, NBTSizeTracker.field_152451_a);
    }

    public static NBTTagCompound item2tag(ItemStack is) {
        NBTTagCompound tag = new NBTTagCompound();
        is.writeToNBT(tag);
        tag.removeTag("id");
        String name = getName(is);
        if (name != null) {
            tag.setString("name", name);
        }
        return tag;
    }

    public static ItemStack tag2item(NBTTagCompound tag, ItemStack defaultValue) {
        if (tag == null || tag.hasNoTags()) return defaultValue.copy();
        if (tag.hasKey("id")) {
            // Legacy
            ItemStack is = ItemStack.loadItemStackFromNBT(tag);
            if (is == null) return defaultValue.copy();
            return is;
        }
        String itemName = tag.getString("name");
        if (StringUtils.isNullOrEmpty(itemName)) return defaultValue.copy();
        byte stackSize = tag.getByte("Count");
        short itemDamage = tag.getShort("Damage");
        Item it = getItemFromName(itemName);
        if (it == null) return defaultValue.copy();
        ItemStack ret = new ItemStack(it, stackSize, itemDamage);
        if (tag.hasKey("tag", Constants.NBT.TAG_COMPOUND)) {
            ret.setTagCompound(tag.getCompoundTag("tag"));
        }
        return ret;
    }

    public static TileEntity cloneTileEntity(TileEntity orig) {
        NBTTagCompound tag = new NBTTagCompound();
        orig.writeToNBT(tag);
        return TileEntity.createAndLoadEntity(tag);
    }

    public static Block getBlock(ItemStack is) {
        if (is == null) return null;
        return Block.getBlockFromItem(is.getItem());
    }

    public static Block getBlock(Item it) {
        return Block.getBlockFromItem(it);
    }

    public static Block getBlock(int id) {
        return Block.getBlockById(id);
    }

    public static Item getItem(int id) {
        return Item.getItemById(id);
    }

    public static Item getItem(Block block) {
        return Item.getItemFromBlock(block);
    }

    public static int getId(Block block) {
        return Block.getIdFromBlock(block);
    }

    public static int getId(Item it) {
        return Item.getIdFromItem(it);
    }

    public static int getId(ItemStack is) {
        if (is == null) return 0;
        return Item.getIdFromItem(is.getItem());
    }

    public static String getName(Item it) {
        return Item.itemRegistry.getNameForObject(it);
    }

    public static String getName(ItemStack is) {
        return getName(is == null ? null : is.getItem());
    }

    public static String getName(Block b) {
        return Block.blockRegistry.getNameForObject(b);
    }

    public static Block getBlockFromName(String blockName) {
        return (Block) Block.blockRegistry.getObject(blockName);
    }

    public static Item getItemFromName(String itemName) {
        return (Item) Item.itemRegistry.getObject(itemName);
    }

}
