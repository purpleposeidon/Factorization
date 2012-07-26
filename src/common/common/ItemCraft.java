package factorization.common;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.src.Container;
import net.minecraft.src.CraftingManager;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.IInventory;
import net.minecraft.src.IRecipe;
import net.minecraft.src.InventoryCrafting;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.TileEntity;
import net.minecraft.src.forge.ForgeHooks;
import net.minecraft.src.forge.ITextureProvider;

public class ItemCraft extends Item implements ITextureProvider {
    private final int slot_length = 9;
    static List<IRecipe> recipes = new ArrayList();

    public ItemCraft(int i) {
        super(i);
        maxStackSize = 1;
        setHasSubtypes(true);
    }

    public static void addStamperRecipe(IRecipe recipe) {
        recipes.add(recipe);
    }

    boolean isSlotSet(ItemStack is, int i) {
        return getSlot(is, i) != null;
    }

    private ItemStack getSlot(ItemStack is, int i) {
        NBTTagCompound tag = is.getTagCompound();
        if (tag == null) {
            return null;
        }
        NBTTagCompound slot = (NBTTagCompound) tag.getTag("slot" + i);
        if (slot == null) {
            return null;
        }
        return ItemStack.loadItemStackFromNBT(slot);
    }

    private void setSlot(ItemStack is, int i, ItemStack origWhat) {
        ItemStack what = origWhat.copy();
        what.stackSize = 1;
        NBTTagCompound tag = is.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            is.setTagCompound(tag);
        }

        NBTTagCompound saved = new NBTTagCompound();
        what.writeToNBT(saved);
        tag.setTag("slot" + i, saved);
        craftAt(is, true, null);
    }


    // @Override -- XXX NOTE Can't override due to server
    public void addInformation(ItemStack is, List list) {
        // super.addInformation(is, list); // XXX NOTE Can't call due to server
        String line = "";
        ArrayList<String> toAdd = new ArrayList<String>();
        int count = 0;
        if (is.getItemDamage() > 1) {
            return;
        }

        for (int i = 0; i < 9; i++) {
            ItemStack here = getSlot(is, i);
            if (here == null) {
                line += "-";
            } else {
                line += Core.instance.translateItemStack(here);
                count++;
            }
            if (i % 3 == 2) {
                // last of the line
                if (line.length() > 0) {
                    toAdd.add(line);
                }
                line = "";
            } else {
                line += " ";
            }
        }

        if (count != 0) {
            list.addAll(toAdd);
        } else {
            list.add("Empty");
        }
    }

    public boolean addItem(ItemStack is, int i, ItemStack what) {
        if (i < 0 || 8 < i) {
            throw new RuntimeException("out of range");
        }
        if (what.getItem() instanceof ItemCraft) {
            // No nesting! Jesus
            return false;
        }
        if (getSlot(is, i) != null) {
            return false;
        }
        setSlot(is, i, what);
        return true;
    }

    class DummyContainer extends Container {
        @Override
        public boolean canInteractWith(EntityPlayer entityplayer) {
            return false;
        }

        @Override
        public void onCraftMatrixChanged(IInventory iinventory) {
        }
    }

    InventoryCrafting getCrafter(ItemStack is) {
        InventoryCrafting craft = new InventoryCrafting(new DummyContainer(), 3, 3);
        for (int i = 0; i < slot_length; i++) {
            craft.setInventorySlotContents(i, getSlot(is, i));
        }
        return craft;
    }

    ItemStack findMatchingRecipe(InventoryCrafting craft) {
        for (IRecipe recipe : recipes) {
            if (recipe.matches(craft)) {
                return recipe.getCraftingResult(craft);
            }
        }
        return CraftingManager.getInstance().findMatchingRecipe(craft);
    }

    public ArrayList<ItemStack> craftAt(ItemStack is, boolean fake, TileEntity where) {
        // Return the crafting result, and any leftover ingredients (buckets)
        // If the crafting recipe fails, return our contents.
        if (!(is.getItem() instanceof ItemCraft)) {
            return null;
        }
        if (is.getItemDamage() == Core.registry.diamond_shard_packet.getItemDamage()) {
            ItemStack cp = Core.registry.diamond_shard_packet.copy();
            cp.setItemDamage(0);
            return craftAt(cp, fake, where);
        }

        ArrayList<ItemStack> ret = new ArrayList<ItemStack>();
        InventoryCrafting craft = getCrafter(is);

        ItemStack result;

        result = findMatchingRecipe(craft);
        if (result == null) {
            is.setItemDamage(0);
        }
        else if (is.getItemDamage() == 0) {
            is.setItemDamage(1);
        }
        if (result == null) {
            // crafting failed, dump everything
            for (int i = 0; i < slot_length; i++) {
                ItemStack here = getSlot(is, i);
                if (here != null) {
                    ret.add(here);
                }
            }
        } else {
            ret.add(result);
            if (!fake) {
                if (Core.registry.diamond_shard_recipe.matches(craft) && where != null) {
                    Sound.shardMake.playAt(where);
                }
                ForgeHooks.onTakenFromCrafting(null, result, craft);
            }

            for (int i = 0; i < craft.getSizeInventory(); i++) {
                ItemStack here = craft.getStackInSlot(i);
                if (here == null) {
                    continue;
                }
                if (fake) {
                    // maybe there's some crazy mod that tracks items
                    here = here.copy();
                }
                here.stackSize -= 1;
                if (here.getItem().hasContainerItem()) {
                    ret.add(new ItemStack(here.getItem().getContainerItem()));
                }

                if (here.stackSize > 0) {
                    ret.add(here);
                }
            }
        }

        return ret;
    }

    public boolean dumpToTable(ItemStack is, InventoryCrafting craft) {
        if (craft.getSizeInventory() < 9) {
            return false;
        }

        for (int i = 0; i < craft.getSizeInventory(); i++) {
            ItemStack here = craft.getStackInSlot(i);
            if (here.getItem() == this) {
                continue;
            }
            if (here != null && getSlot(is, i) != null) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String getItemNameIS(ItemStack itemstack) {
        return "Craftpacket";
    }

    @Override
    public String getItemName() {
        return "Craftpacket";
    }

    @Override
    public String getTextureFile() {
        return Core.texture_file_item;
    }

    public boolean isValidCraft(ItemStack is) {
        return is.getItemDamage() != 0;
    }

    // @Override -- XXX NOTE: Can't override due to server
    public int getIconFromDamage(int damage) {
        if (damage == 0) {
            return 2;
        }
        return 3;
    }

    // @Override -- XXX Waiting for MCP update
    public boolean getShareNBT() {
        return true;
    }

    // @Override -- XXX TODO SERVER get___?
    public boolean func_46003_i() {
        return getShareNBT();
    }

    // @Override -- XXX TODO CLIENT get___?
    public boolean func_46056_k() {
        return getShareNBT();
    }
}
