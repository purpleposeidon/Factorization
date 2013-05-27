package factorization.common;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;
import factorization.api.IActOnCraft;
import factorization.common.Core.TabType;

public class ItemOreProcessing extends Item implements IActOnCraft {
    public static ArrayList<String> OD_ores = new ArrayList(), OD_ingots = new ArrayList();
    public static enum OreType {
        IRON(0, 0xD8D8D8, "Iron", "oreIron", "ingotIron"),
        GOLD(1, 0xEEEB28, "Gold", "oreGold", "ingotGold"),
        LEAD(2, 0x2F2C3C, "Lead", null, "ingotLead"),
        TIN(3, 0xD7F7FF, "Tin", "oreTin", "ingotTin"),
        COPPER(4, 0xD68C39, "Copper", "oreCopper", "ingotCopper"),
        SILVER(5, 0x7B96B9, "Silver", null, "ingotSilver"),
        GALENA(6, 0x687B99, "Galena", "oreSilver", null)
        ;
        int ID;
        int color;
        String en_name;
        String OD_ore, OD_ingot;
        boolean enabled = false;
        ItemStack processingResult = null;
        private OreType(int ID, int color, String en_name, String OD_ore, String OD_ingot) {
            this.ID = ID;
            this.color = color;
            this.en_name = en_name;
            this.OD_ore = OD_ore;
            this.OD_ingot = OD_ingot;
            if (OD_ore != null) {
                OD_ores.add(OD_ore);
            }
            if (OD_ingot != null) {
                OD_ingots.add(OD_ingot);
            }
        }
        
        public void enable() {
            if (!this.enabled) {
                ItemStack dirty = Core.registry.ore_dirty_gravel.makeStack(this);
                ItemStack clean = Core.registry.ore_clean_gravel.makeStack(this);
                ItemStack reduced = Core.registry.ore_reduced.makeStack(this);
                ItemStack crystal = Core.registry.ore_crystal.makeStack(this);
                OreDictionary.registerOre("dirtyGravel" + this.en_name, dirty);
                OreDictionary.registerOre("cleanGravel" + this.en_name, clean);
                OreDictionary.registerOre("reduced" + this.en_name, reduced);
                OreDictionary.registerOre("crystalline" + this.en_name, crystal);
            }
            this.enabled = true;
        }
        
        public static OreType fromOreClass(String oreClass) {
            for (OreType ot : values()) {
                if (ot.OD_ingot != null && ot.OD_ingot.equals(oreClass)) {
                    return ot;
                }
                if (ot.OD_ore != null && ot.OD_ore.equals(oreClass)) {
                    return ot;
                }
            }
            return null;
        }
    }
    
    String stateName;

    protected ItemOreProcessing(int itemID, int icon, String stateName) {
        super(itemID);
        setHasSubtypes(true);
        this.stateName = stateName;
        Core.tab(this, TabType.MATERIALS);
        setUnlocalizedName("factorization:ore/" + stateName);
    }

    @Override
    public int getColorFromItemStack(ItemStack is, int renderPass) {
        try {
            return OreType.values()[is.getItemDamage()].color;
        } catch (ArrayIndexOutOfBoundsException e) {
            return 0xFFFF00;
        }
    }
    
    @Override
    public String getUnlocalizedName(ItemStack is) {
        return "item.factorization:ore/" + stateName + "/" + OreType.values()[is.getItemDamage()];
    }

    @Override
    public void getSubItems(int id, CreativeTabs tab, List list) {
        for (OreType oreType : OreType.values()) {
            if (oreType.enabled) {
                boolean show = true;
                if ((this == Core.registry.ore_crystal || this == Core.registry.ore_reduced) && oreType == OreType.GALENA) {
                    show = false;
                }
                if (this == Core.registry.ore_dirty_gravel || this == Core.registry.ore_clean_gravel) {
                    if (oreType == OreType.SILVER || oreType == OreType.LEAD) {
                        show = false;
                    }
                }
                if (show) {
                    list.add(new ItemStack(this, 1, oreType.ID));
                }
            }
        }
    }

    @Override
    public void addInformation(ItemStack is, EntityPlayer player, List infoList, boolean verbose) {
        Core.brand(is, infoList);
    }
    
    public ItemStack makeStack(OreType ot) {
        return new ItemStack(this, 1, ot.ID);
    }

    @Override
    public void onCraft(ItemStack is, IInventory craftMatrix, int craftSlot, ItemStack result, EntityPlayer player) {
        if (result == null) {
            return;
        }
        if (result.getItem() != Core.registry.ore_clean_gravel) {
            return;
        }
        if (is.getItem() != Core.registry.ore_dirty_gravel) {
            return;
        }
        if (player != null && player.worldObj != null) {
            if (player.worldObj.isRemote) {
                return;
            }
        }
        boolean any = false;
        for (int stack_index = 0; stack_index < is.stackSize; stack_index++) {
            if (Math.random() > 0.25) {
                continue;
            }
            any = true;
            for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
                ItemStack pi = player.inventory.getStackInSlot(i);
                if (pi != null && pi.getItem() == Core.registry.sludge) {
                    if (pi.stackSize < pi.getMaxStackSize()) {
                        pi.stackSize++;
                        return;
                    }
                }
            }
            for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
                ItemStack pi = player.inventory.getStackInSlot(i);
                if (pi == null) {
                    player.inventory.setInventorySlotContents(i, new ItemStack(Core.registry.sludge, 1));
                    return;
                }
            }
            player.dropPlayerItem(new ItemStack(Core.registry.sludge, 1));
        }
        if (any) {
            Core.proxy.updatePlayerInventory(player);
        }
    }
}
