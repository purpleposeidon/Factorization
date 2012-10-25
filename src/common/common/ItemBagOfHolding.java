package factorization.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import factorization.api.IActOnCraft;
import factorization.common.Core.TabType;

import net.minecraft.src.CreativeTabs;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.IInventory;
import net.minecraft.src.InventoryPlayer;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.NBTTagList;
import net.minecraft.src.TileEntity;
import net.minecraft.src.World;
import net.minecraftforge.common.ISidedInventory;
import net.minecraftforge.common.ForgeDirection;

public class ItemBagOfHolding extends Item implements IActOnCraft {
    //XXX: Sending NBT data of all the items might not be a good idea. We might force it to not be shared, and use the damage value for the pearl count.
    public ItemBagOfHolding(int id) {
        super(id);
        setMaxStackSize(1);
        Core.tab(this, TabType.TOOLS);
    }

    final String pearlcount = "pearlcount";

    public void init(ItemStack is) {
        NBTTagCompound tag = is.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            is.setTagCompound(tag);
        }
        if (!tag.hasKey(pearlcount)) {
            tag.setInteger(pearlcount, 0);
        }
    }

    public void addPearl(ItemStack is) {
        init(is);
        NBTTagCompound tag = is.getTagCompound();
        int i = 1 + tag.getInteger(pearlcount);
        tag.setInteger(pearlcount, i);
    }

    int getNumOfCols(ItemStack is) {
        if (is == null) {
            return 3;
        }
        init(is);
        return 3 + is.getTagCompound().getInteger("pearlcount");
    }
    
    private ArrayList<ItemStack> padRow(ArrayList<ItemStack> row, int columnCount) {
        columnCount -= row.size();
        while (columnCount > 0) {
            columnCount--;
            row.add(null);
        }
        return row;
    }

    ArrayList<ItemStack> getRow(ItemStack is, int row) {
        int colCount = getNumOfCols(is);
        ArrayList<ItemStack> ret = new ArrayList();
        
        NBTTagCompound tag = is.getTagCompound();
        if (tag == null) {
            return padRow(ret, colCount);
        }
        NBTTagList items = tag.getTagList("row" + row);
        if (items == null || items.tagCount() != colCount) {
            return padRow(ret, colCount);
        }
        for (int i = 0; i < items.tagCount(); i++) {
            NBTTagCompound item = (NBTTagCompound) items.tagAt(i);
            ret.add(ItemStack.loadItemStackFromNBT(item));
        }
        return padRow(ret, colCount);
    }

    void writeRow(ItemStack is, ArrayList<ItemStack> items, int row) {
        init(is);
        NBTTagList list = new NBTTagList("row" + row);
        for (ItemStack i : items) {
            NBTTagCompound add = new NBTTagCompound();
            if (i != null) {
                list.appendTag(i.writeToNBT(add));
            } else {
                list.appendTag(add);
            }
        }
        NBTTagCompound tag = is.getTagCompound();
        tag.setTag("row" + row, list);

    }

    public void useBag(EntityPlayer player, boolean reverse) {
        if (reverse) {
            swapItemsReverse(player);
        }
        else {
            swapItems(player);
        }
        Core.proxy.pokePocketCrafting();
    }

    int findBag(EntityPlayer player) {
        InventoryPlayer inv = player.inventory;
        for (int i = 0; i < inv.mainInventory.length; i++) {
            ItemStack is = inv.mainInventory[i];
            if (is == null) {
                continue;
            }
            if (is.getItem() == this) {
                return i;
            }
        }
        return -1;
    }

    void swapItems(EntityPlayer player) {
        int i = findBag(player);
        if (i == -1) {
            return;
        }
        InventoryPlayer inv = player.inventory;

        int bag_col = i % 9;
        int bag_height = i / 9;
        ItemStack is = inv.mainInventory[i];
        // might at some point want to use the height as a parameter?
        if (bag_col == 8) {
            return;
        }
        for (int row = 0; row < 4; row++) {
            ArrayList<ItemStack> items = getRow(is, row);
            while (items.size() < getNumOfCols(is)) {
                items.add(null);
            }

            for (int j = row * 9 + bag_col + 1; j < (1 + row) * 9; j++) {
                items.add(inv.mainInventory[j]);
                ItemStack removed = items.remove(0);
                inv.mainInventory[j] = removed;
                if (removed != null) {
                    removed.animationsToGo = 3;
                }
            }
            writeRow(is, items, row);
        }
    }

    void swapItemsReverse(EntityPlayer player) {
        int i = findBag(player);
        if (i == -1) {
            return;
        }
        InventoryPlayer inv = player.inventory;

        int bag_col = i % 9;
        int bag_height = i / 9;
        ItemStack is = inv.mainInventory[i];
        // might at some point want to use the height as a parameter?
        if (bag_col == 8) {
            return;
        }
        for (int row = 0; row < 4; row++) {
            ArrayList<ItemStack> items = getRow(is, row);
            while (items.size() < getNumOfCols(is)) {
                items.add(0, null);
            }

            for (int j = (1 + row) * 9 - 1; j >= row * 9 + bag_col + 1; j--) {
                items.add(0, inv.mainInventory[j]);
                ItemStack removed = items.remove(items.size() - 1);
                inv.mainInventory[j] = removed;
                if (removed != null) {
                    removed.animationsToGo = 3;
                }
            }
            writeRow(is, items, row);
        }
    }

    @Override
    public String getItemName() {
        //XXX Bluh localization plz2fix
        return "Bag of Holding";
    }

    @Override
    public String getItemNameIS(ItemStack par1ItemStack) {
        return getItemName();
    }

    @Override
    // XXX NOTE: Can't override due to... the *fucking* server. >:|
    public void addInformation(ItemStack is, EntityPlayer player, List list, boolean verbose) {
        // Bag of Holding
        // Stores 5 columns
        if (is.getTagCompound() == null) {
            Core.brand(list);
            return;
        }
        int n = getNumOfCols(is);
        list.add("Stores " + n + " columns");
        Core.brand(list);
    }

    @Override
    public boolean onItemUseFirst(ItemStack is, EntityPlayer player, World world, int x, int y,
            int z, int side) {
        // Dump contents into chest. But only on shift-click.
        if (!player.isSneaking()) {
            return false;
        }

        TileEntity te = world.getBlockTileEntity(x, y, z);
        if (!(te instanceof IInventory)) {
            return false;
        }
        IInventory inv = (IInventory) te;
        int inv_start = 0;
        int inv_end = inv.getSizeInventory();
        if (inv instanceof ISidedInventory) {
            ISidedInventory sinv = (ISidedInventory) inv;
            ForgeDirection orient = ForgeDirection.getOrientation(side);
            inv_start = sinv.getStartInventorySide(orient);
            inv_end = inv_start = sinv.getSizeInventorySide(orient);
        }
        // NOTE: We *could* consider double chests properly. But it maybe it's
        // more useful this way?
        boolean did_something = false;
        int slot = inv_start;

        for (int row = 0; row < 4; row++) {
            ArrayList<ItemStack> items = getRow(is, row);
            boolean changed_row = false;
            while (items.size() > 0) {
                if (items.get(0) == null) {
                    items.remove(0);
                    continue;
                }
                if (slot == inv_end) {
                    break;
                }
                if (inv.getStackInSlot(slot) == null) {
                    inv.setInventorySlotContents(slot, items.get(0));
                    items.remove(0);
                    changed_row = true;
                }
                slot += 1;
            }
            if (changed_row) {
                writeRow(is, items, row);
                did_something = true;
            }
        }

        return true;
    }

    @Override
    public String getTextureFile() {
        return Core.texture_file_item;
    }

    public int getIconFromDamage(int damage) {
        return 16 + ((int) (System.currentTimeMillis() / 50000) % 5);
    }

    @Override
    // ...
    public boolean hasEffect(ItemStack par1ItemStack) {
        return System.currentTimeMillis() / 75000 % 7 == 5;
    }

    public boolean insertItem(ItemStack is, ItemStack add) {
        ArrayList<ArrayList<ItemStack>> contents = new ArrayList(4);
        for (int i = 0; i < 4; i++) {
            contents.add(getRow(is, i));
        }

        boolean did_something = false;
        int num_cols = getNumOfCols(is);
        boolean should_add = false;
        done: for (int col = 0; col < num_cols; col++) {
            for (int row_id = 0; row_id < 4; row_id++) {
                ArrayList<ItemStack> row = contents.get(row_id);
                ItemStack here = row.get(col);
                if (here == null && should_add) {
                    should_add = false;
                    here = add.copyItemStack(add);
                    here.stackSize = 0;
                    row.set(col, here);
                }
                if (here == null || !here.isItemEqual(add)) {
                    continue;
                }
                should_add = true;
                int free = here.getMaxStackSize() - here.stackSize;
                if (free <= 0) {
                    continue;
                }
                int delta = Math.min(free, add.stackSize);
                add.stackSize -= delta;
                here.stackSize += delta;
                did_something = true;
                if (add.stackSize <= 0) {
                    break done;
                }
            }
        }

        if (did_something) {
            is.animationsToGo = 5;
            // and maybe pop up some ender particles?
            for (int row_id = 0; row_id < 4; row_id++) {
                writeRow(is, contents.get(row_id), row_id);
            }
        }
        return did_something;
    }

    @Override
    //-- XXX Waiting for MCP update
    public boolean getShareTag() {
        //Note: This function might be removed/unnecessary next MC version or something...
        return true;
    }

    @Override
    public void onCraft(ItemStack is, IInventory craftMatrix, int craftSlot, ItemStack result,
            EntityPlayer player) {
        init(result);
        //a bit of lovely code to see if the craftMatrix (which isn't a CraftingInventory or whatever)
        //actually matches the recipe
        boolean bag = false, pearl = false, dark_iron = false, leather = false;
        boolean bad = false;
        for (int i = 0; i < craftMatrix.getSizeInventory(); i++) {
            ItemStack here = craftMatrix.getStackInSlot(i);
            if (here == null) {
                continue;
            }
            Item it = here.getItem();
            if (it == this) {
                if (bag) {
                    bad = true;
                    break;
                }
                bag = true;
            }
            if (it == Item.enderPearl) {
                if (pearl) {
                    bad = true;
                    break;
                }
                pearl = true;
            }
            if (it == Core.registry.dark_iron) {
                if (dark_iron) {
                    bad = true;
                    break;
                }
                dark_iron = true;
            }
            if (it == Item.leather) {
                if (leather) {
                    bad = true;
                    break;
                }
                leather = true;
            }
        }
        if (!bad && bag && pearl && dark_iron && leather) {
            result.setTagCompound(is.getTagCompound());
            addPearl(result);
        }
    }
}
