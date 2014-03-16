package factorization.oreprocessing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import factorization.api.IActOnCraft;
import factorization.shared.Core;
import factorization.shared.Core.TabType;
import factorization.shared.ItemFactorization;

public class ItemOreProcessing extends ItemFactorization implements IActOnCraft {
    public static ArrayList<String> OD_ores = new ArrayList(), OD_ingots = new ArrayList();
    public static enum OreType {
        IRON(0, 0xD8D8D8, "Iron", "oreIron", "ingotIron"),
        GOLD(1, 0xEEEB28, "Gold", "oreGold", "ingotGold"),
        LEAD(2, 0x2F2C3C, "Lead", "oreLead", "ingotLead"),
        TIN(3, 0xD7F7FF, "Tin", "oreTin", "ingotTin"),
        COPPER(4, 0xD68C39, "Copper", "oreCopper", "ingotCopper"),
        SILVER(5, 0x7B96B9, "Silver", null, "ingotSilver"),
        GALENA(6, 0x687B99, "Galena", "oreSilver", null),
        //no more aluminum. Bye-bye, aluminum.
        COBALT(8, 0x2376DD, "Cobalt", "oreCobalt", "ingotCobalt"),
        ARDITE(9, 0xF48A00, "Ardite", "oreArdite", "ingotArdite"),
        DARKIRON(10, 0x5000D4, "Dark Iron", "oreFzDarkIron", "ingotFzDarkIron"),
        INVALID(0, 0xFFFFFF, "Invalid", null, null);
        ;
        static {
            COBALT.surounding_medium = new ItemStack(Blocks.netherrack);
            ARDITE.surounding_medium = new ItemStack(Blocks.netherrack);
        }
        
        public int ID;
        int color;
        String en_name;
        String OD_ore, OD_ingot;
        public boolean enabled = false;
        ItemStack processingResult = null;
        ItemStack surounding_medium = new ItemStack(Blocks.stone);
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
        
        static OreType[] vals = null;
        public static OreType fromID(int id) {
            if (vals == null) {
                int max = 0;
                for (OreType ot : OreType.values()) {
                    max = Math.max(max, ot.ID);
                }
                vals = new OreType[max + 1];
                Arrays.fill(vals, INVALID);
                for (OreType ot : OreType.values()) {
                    if (ot == INVALID) continue;
                    vals[ot.ID] = ot;
                }
            }
            if (id < 0 || id >= vals.length) {
                return INVALID;
            }
            return vals[id];
        }
        
        public static OreType fromIS(ItemStack is) {
            if (is == null) {
                return INVALID;
            }
            return fromID(is.getItemDamage());
        }
    }
    
    String stateName;

    public ItemOreProcessing(String stateName) {
        super("ore/" + stateName, TabType.MATERIALS);
        setHasSubtypes(true);
        this.stateName = stateName;
    }

    @Override
    public int getColorFromItemStack(ItemStack is, int renderPass) {
        return OreType.fromIS(is).color;
    }
    
    @Override
    public String getUnlocalizedName(ItemStack is) {
        return "item.factorization:ore/" + stateName + "/" + OreType.fromIS(is);
    }

    @Override
    public void getSubItems(Item id, CreativeTabs tab, List list) {
        for (OreType oreType : OreType.values()) {
            if (oreType.enabled) {
                boolean show = true;
                if ((this == Core.registry.ore_crystal || this == Core.registry.ore_reduced) && oreType == OreType.GALENA) {
                    show = false;
                }
                if (this == Core.registry.ore_dirty_gravel || this == Core.registry.ore_clean_gravel) {
                    if (oreType == OreType.SILVER) {
                        show = false;
                    }
                }
                if (show) {
                    list.add(new ItemStack(this, 1, oreType.ID));
                }
            }
        }
    }
    
    public ItemStack makeStack(OreType ot) {
        return new ItemStack(this, 1, ot.ID);
    }

    @Override
    public void onCraft(ItemStack is, IInventory craftMatrix, int craftSlot, ItemStack result, EntityPlayer player) {
        if (result == null || player == null) {
            return;
        }
        if (player.worldObj == null) {
            if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
                return;
            }
        } else if (player.worldObj.isRemote) {
            return;
        }
        if (result.getItem() != Core.registry.ore_clean_gravel) {
            return;
        }
        if (is.getItem() != Core.registry.ore_dirty_gravel) {
            return;
        }
        boolean any = false;
        if (Math.random() > 0.25) {
            return;
        }
        any = true;
        ItemStack toAdd = new ItemStack(Core.registry.sludge);
        if (!player.inventory.addItemStackToInventory(toAdd)) {
            player.dropPlayerItemWithRandomChoice(new ItemStack(Core.registry.sludge, 1), false);
        }
    }
}
