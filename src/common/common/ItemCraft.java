package factorization.common;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.SlotCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Icon;
import net.minecraft.world.World;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Coord;
import factorization.common.Core.TabType;

public class ItemCraft extends Item {
    private final int slot_length = 9;
    static List<IRecipe> recipes = new ArrayList();

    public ItemCraft(int i) {
        super(i);
        maxStackSize = 1;
        setHasSubtypes(true);
        Core.tab(this, TabType.MISC);
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

    private void setSlot(ItemStack is, int i, ItemStack origWhat, TileEntity where) {
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
        craftAt(is, true, where);
    }

    @Override
    // -- XXX NOTE Can't override due to server
    public void addInformation(ItemStack is, EntityPlayer player, List list, boolean verbose) {
        // super.addInformation(is, list); // XXX NOTE Can't call due to server
        String line = "";
        ArrayList<String> toAdd = new ArrayList<String>();
        int count = 0;
        if (is.getItemDamage() > 1) {
            if (is.getItemDamage() == Core.registry.diamond_shard_packet.getItemDamage() && is != Core.registry.diamond_shard_packet) {
                addInformation(Core.registry.diamond_shard_packet, player, list, verbose);
            } else {
                Core.brand(list);
                return;
            }
        }

        for (int i = 0; i < 9; i++) {
            ItemStack here = getSlot(is, i);
            if (here == null) {
                line += "-";
            } else {
                line += Core.proxy.translateItemStack(here);
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
        Core.brand(list);
    }

    public boolean addItem(ItemStack is, int i, ItemStack what, TileEntity where) {
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
        setSlot(is, i, what, where);
        return true;
    }

    InventoryCrafting getCrafter(ItemStack is) {
        InventoryCrafting craft = FactorizationUtil.makeCraftingGrid();
        for (int i = 0; i < slot_length; i++) {
            craft.setInventorySlotContents(i, getSlot(is, i));
        }
        return craft;
    }

    ItemStack findMatchingRecipe(InventoryCrafting craft, World world) {
        for (IRecipe recipe : recipes) {
            if (recipe.matches(craft, world)) {
                return recipe.getCraftingResult(craft);
            }
        }
        return CraftingManager.getInstance().findMatchingRecipe(craft, world);
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

        final ArrayList<ItemStack> ret = new ArrayList<ItemStack>();
        InventoryCrafting craft = getCrafter(is);

        ItemStack result;

        if (where == null || where.worldObj == null) {
            result = findMatchingRecipe(craft, null);
        } else {
            result = findMatchingRecipe(craft, where.worldObj);
        }
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
            if (fake) {
                ret.add(result);
                return ret;
            }
            Coord pos = null;
            if (where == null) {
                //pos = new Coord(DimensionManager.getWorld(0), 0, -20, 0); //ugh. Lame... 
            } else {
                pos = new Coord(where);
            }
            EntityPlayer fakePlayer = FactorizationUtil.makePlayer(pos, "CraftPacket");
            if (where != null) {
                if (Core.registry.diamond_shard_recipe.matches(craft, where.worldObj)) {
                    Sound.shardMake.playAt(where);
                }
                fakePlayer.worldObj = where.worldObj;
                fakePlayer.posX = where.xCoord;
                fakePlayer.posY = where.yCoord;
                fakePlayer.posZ = where.zCoord;
            }

            IInventory craftResult = new InventoryCraftResult();
            craftResult.setInventorySlotContents(0, result);
            SlotCrafting slot = new SlotCrafting(fakePlayer, craft, craftResult, 0, 0, 0);
            slot.onPickupFromSlot(fakePlayer, result);
            ret.add(result);
            FactorizationUtil.addInventoryToArray(craft, ret);
            FactorizationUtil.addInventoryToArray(fakePlayer.inventory, ret);
        }

        return ret;
    }

    @Override
    public String getItemNameIS(ItemStack itemstack) {
        return "Craftpacket";
    }

    @Override
    public String getItemName() {
        return "Craftpacket";
    }

    public boolean isValidCraft(ItemStack is) {
        return is.getItemDamage() != 0;
    }

    @SideOnly(Side.CLIENT)
    FzIcon complete = new FzIcon("craft/packet_complete"), incomplete = new FzIcon("craft/packet_incomplete");
    
    @Override
    public void registerIcon(IconRegister reg) {
        FzIcon.registerNew(reg);
    }
    
    @Override
    public Icon getIconFromDamage(int damage) {
        if (damage == 0) {
            return incomplete;
        }
        return complete;
    }

    @Override
    public boolean getShareTag() {
        return true;
    }
    
    @Override
    public void getSubItems(int id, CreativeTabs tab, List list) {
        list.add(new ItemStack(Core.registry.item_craft));
        list.add(Core.registry.diamond_shard_packet);
    }
}
