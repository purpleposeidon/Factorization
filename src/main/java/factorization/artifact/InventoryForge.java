package factorization.artifact;

import factorization.shared.Core;
import factorization.util.ItemUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerRepair;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.StringUtils;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.regex.Pattern;

public class InventoryForge implements IInventory {
    final EntityPlayer player;
    public String name, lore;
    final Container container;
    public String error_message = "notools";

    static final int SLOT_FIRST = 0, SLOT_SECOND = 1;
    static final int SLOT_DYE_1 = 2, SLOT_DYE_2 = 3;
    static final int SLOT_OUT = 4;
    static final int SLOT_POTENT_START = 5, SLOT_POTENT_END = SLOT_POTENT_START + 3;
    static final int SLOT_ENCHANT_START = SLOT_POTENT_END, SLOT_ENCHANT_END = SLOT_ENCHANT_START + 8;
    static final int SIZE = SLOT_ENCHANT_END;

    ItemStack[] inv = new ItemStack[SIZE];
    public byte[] warnings = new byte[SIZE];

    public InventoryForge(EntityPlayer player, Container container) {
        this.player = player;
        this.container = container;
    }

    @Override
    public int getSizeInventory() {
        return SIZE;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return inv[slot];
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        ItemStack ret = inv[slot].splitStack(amount);
        inv[slot] = ItemUtil.normalize(inv[slot]);
        if (slot == SLOT_OUT && amount > 0 && !player.worldObj.isRemote) {
            ItemStack got = new ArtifactBuilder(false).buildArtifact(player, name, lore);
            InspirationManager.makeArtifact(player, got);
            return got;
        }
        return ret;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        return null;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        inv[slot] = stack;
        if (slot == SLOT_OUT && !player.worldObj.isRemote) {
            new ArtifactBuilder(false).buildArtifact(player, name, lore);
        }
    }

    @Override
    public String getInventoryName() {
        return "factorization.inventory.forge";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 1;
    }

    @Override
    public void markDirty() {
        if (player.worldObj.isRemote) return;
        inv[SLOT_OUT] = new ArtifactBuilder(true).buildArtifact(player, name, lore);
        container.detectAndSendChanges();
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return true;
    }

    @Override
    public void openInventory() {

    }

    @Override
    public void closeInventory() {

    }


    static String[] dyes = {
            "dyeBlack",
            "dyeRed",
            "dyeGreen",
            "dyeBrown",
            "dyeBlue",
            "dyePurple",
            "dyeCyan",
            "dyeLightGray",
            "dyeGray",
            "dyePink",
            "dyeLime",
            "dyeYellow",
            "dyeLightBlue",
            "dyeMagenta",
            "dyeOrange",
            "dyeWhite"};
    static ArrayList[] dyeList = new ArrayList[16];
    static {
        for (int i = 0; i < dyes.length; i++) {
            dyeList[i] = OreDictionary.getOres(dyes[i]);
        }
    }

    public static int getDyeIndex(ItemStack stack) {
        if (stack == null) return -1;
        for (int i = 0; i < dyeList.length; i++) {
            ArrayList<ItemStack> list = dyeList[i];
            for (ItemStack dye : list) {
                if (ItemUtil.couldMerge(stack, dye)) return i;
            }
        }
        return -1;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (!InspirationManager.canMakeArtifact(player)) {
            error_message = "goaway";
            container.detectAndSendChanges();
            return false;
        }
        if (slot == SLOT_FIRST || slot == SLOT_SECOND) {
            ItemStack a = inv[SLOT_FIRST];
            ItemStack b = inv[SLOT_SECOND];
            if (a != null && b != null) return false;
            ItemStack template = a != null ? a : b;
            return ItemUtil.couldMerge(template, stack);
        }
        if (slot == SLOT_DYE_1 || slot == SLOT_DYE_2) {
            return getDyeIndex(stack) >= 0;
        }
        if (slot == SLOT_OUT) return false;
        if (slot >= SLOT_POTENT_START && slot < SLOT_POTENT_END) {
            return stack.getItem() == Core.registry.item_potency && stack.getItemDamage() == 0;
        }
        if (slot >= SLOT_ENCHANT_START && slot < SLOT_ENCHANT_END) {
            return true;
        }
        return false;
    }

    class ArtifactBuilder {
        final boolean simulate;

        ArtifactBuilder(boolean simulate) {
            this.simulate = simulate;
        }

        void consume(int slot) {
            warnings[slot] = 0;
            if (simulate) return;
            inv[slot] = ItemUtil.normalDecr(inv[slot]);
        }

        String takeDye(int slot) {
            int index = getDyeIndex(inv[slot]);
            if (index < 0) return "";
            consume(slot);
            String color_map = "042l15978daebdcno";
            return "§" + color_map.charAt(index);
        }

        ItemStack err(String msg) {
            error_message = msg;
            return null;
        }

        int cleanWork(ItemStack stack) {
            NBTTagCompound tag = stack.getTagCompound();
            if (tag == null) return 0;
            int i = stack.getRepairCost();
            stack.setRepairCost(0);
            return i;
        }

        ItemStack buildArtifact(EntityPlayer player, String name, String lore) {
            if (player.worldObj.isRemote) return inv[SLOT_OUT];
            // warn on all slots
            for (int i = 0; i < warnings.length; i++) warnings[i] = (byte)(inv[i] == null ? 0 : 1);
            error_message = null;

            // Make sure we've got 2 of the tool
            final ItemStack first = inv[SLOT_FIRST];
            final ItemStack second = inv[SLOT_SECOND];
            if (first == null && second == null) return err("notools");
            if (first == null || second == null) return err("pair");
            if (!ItemUtil.couldMerge(first, second)) return err("notsame");
            if (!TileEntityLegendarium.isTool(first)) return err("nottool");
            ItemStack output = first.copy();
            consume(SLOT_FIRST);
            consume(SLOT_SECOND);

            // Take the potency bottles
            // Out of order because the potency's expensive & non-negotiable
            for (int i = SLOT_POTENT_START; i < SLOT_POTENT_END; i++) {
                if (!ItemUtil.is(inv[i], Core.registry.item_potency)) return err("potency");
                Core.registry.item_potency.onUpdate(inv[i], player.worldObj, player, 0, false);
                if (!ItemPotency.validBottle(inv[i], player)) return err("potency");
                consume(i);
            }

            // Apply the enchantments
            // Out of order so that the anvil doesn't change the name afterwards
            int enchants = 0;
            int repairCount = cleanWork(output);
            for (int i = SLOT_ENCHANT_START; i < SLOT_ENCHANT_END; i++) {
                ItemStack ench = inv[i];
                if (ench == null) continue;
                ContainerRepair anvil = new ContainerRepair(player.inventory, player.worldObj, -1, -1, -1, player);
                anvil.putStackInSlot(0, output.copy());
                anvil.putStackInSlot(1, ench.copy());
                anvil.updateRepairOutput();
                ItemStack upgraded = anvil.getSlot(2).getStack();
                if (upgraded == null) continue;
                cleanWork(upgraded);
                if (ItemUtil.couldMerge(output, upgraded)) continue;
                consume(i);
                output = upgraded;
                enchants++;
            }
            output.setRepairCost(repairCount + 1);
            if (enchants <= 0) return err("nobooks");

            // Set the name & lore
            if (!StringUtils.isNullOrEmpty(name)) {
                output.setStackDisplayName(takeDye(SLOT_DYE_1) + name);
            } else {
                return err("noname");
            }
            if (!StringUtils.isNullOrEmpty(lore)) {
                ItemUtil.setLore(output, lore.split(Pattern.quote("|")));
            }
            err("lorehint");
            return output;
        }
    }
}
