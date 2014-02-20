package factorization.oreprocessing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidContainerRegistry.FluidContainerData;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.OreDictionary.OreRegisterEvent;
import factorization.oreprocessing.ItemOreProcessing.OreType;
import factorization.oreprocessing.TileEntitySlagFurnace.SlagRecipes;
import factorization.shared.Core;

public class FactorizationOreProcessingHandler {
    private static final String waterBucket = "fz.waterBucketLike";
    private HashMap<String, ItemStack> bestIngots = new HashMap();
    public static final float GRIND_MULTIPLY = 2F;
    public static final float REDUCE_MULTIPLY = 2.5F;
    public static final float CRYSTALLIZE_MULTIPLY = 3F;
    
    public static final float GRIND = GRIND_MULTIPLY;
    public static final float WASH = 1F;
    public static final float REDUCE = REDUCE_MULTIPLY/GRIND_MULTIPLY;
    public static final float CRYSTALLIZE = CRYSTALLIZE_MULTIPLY/REDUCE_MULTIPLY;
    
    /** This adds an ore to the processing chain: Grinds the ore into gravel */
    void addProcessingFront(OreType oreType, ItemStack ore) {
        ItemStack dirty = Core.registry.ore_dirty_gravel.makeStack(oreType);
        TileEntityGrinder.addRecipe(ore, dirty, GRIND);
    }
    
    /** This (maybe) adds an ore to the processing ends (converting anything to ingots). It only does this if the ingot is better. */
    void addProcessingEnds(OreType oreType, ItemStack ore, ItemStack ingot) {
        //Everything can be slagged
        oreType.enable();
        if (oreType != OreType.SILVER && oreType != OreType.GALENA) {
            TileEntitySlagFurnace.SlagRecipes.register(ore, 1.2F, ingot, 0.4F, oreType.surounding_medium);
        } else if (oreType == OreType.SILVER) {
            //TileEntitySlagFurnace.SlagRecipes.register(ore, 1.2F, new ItemStack(Core.registry.lead_ingot), 1F, ingot);
        }
        if (oreType.processingResult != null) {
            return;
        }
        oreType.processingResult = ingot;
        
        ItemStack dirty = Core.registry.ore_dirty_gravel.makeStack(oreType);
        ItemStack clean = Core.registry.ore_clean_gravel.makeStack(oreType);
        ItemStack reduced = Core.registry.ore_reduced.makeStack(oreType);
        ItemStack crystal = Core.registry.ore_crystal.makeStack(oreType);

        //All processing steps can be smelted
        ItemStack smeltable[];
        if (oreType == OreType.LEAD || oreType == OreType.SILVER) {
            smeltable = new ItemStack[] { reduced, crystal };
        } else if (oreType == OreType.GALENA) {
            smeltable = new ItemStack[] { dirty, clean };
        } else {
            smeltable = new ItemStack[] { dirty, clean, reduced, crystal };
        }
        for (ItemStack is : smeltable) {
            float xp = FurnaceRecipes.smelting().getExperience(ore);
            FurnaceRecipes.smelting().addSmelting(is.itemID, is.getItemDamage(), ingot, xp);
        }
        if (oreType != OreType.SILVER) {
            TileEntitySlagFurnace.SlagRecipes.register(dirty, 1.1F, ingot, 0.2F, Blocks.dirt);
            //Or it could output reduced chunks instead.
        }
    }
    
    ArrayList<OreType> createdBodies = new ArrayList();
    /** Creates the processing chain from gravel to crystals */
    void createProccessingBody(OreType oreType) {
        if (createdBodies.contains(oreType)) {
            return;
        }
        createdBodies.add(oreType);
        ItemStack dirty = Core.registry.ore_dirty_gravel.makeStack(oreType);
        ItemStack clean = Core.registry.ore_clean_gravel.makeStack(oreType);
        ItemStack reduced = Core.registry.ore_reduced.makeStack(oreType);
        ItemStack crystal = Core.registry.ore_crystal.makeStack(oreType);
        ItemStack clean8 = clean.copy();
        clean8.stackSize = 8;
        
        //dirty gravel -> clean gravel
        Core.registry.shapelessOreRecipe(clean, waterBucket, dirty);
        Core.registry.shapelessOreRecipe(clean8, waterBucket, dirty, dirty, dirty, dirty, dirty, dirty, dirty, dirty);
        if (oreType == OreType.GALENA) {
            ItemStack reduced_silver = Core.registry.ore_reduced.makeStack(OreType.SILVER);
            ItemStack reduced_lead = Core.registry.ore_reduced.makeStack(OreType.LEAD);
            ItemStack crystal_silver = Core.registry.ore_crystal.makeStack(OreType.SILVER);
            ItemStack crystal_lead = Core.registry.ore_crystal.makeStack(OreType.LEAD);
            
            TileEntitySlagFurnace.SlagRecipes.register(clean, REDUCE, reduced_lead, REDUCE, reduced_silver);
            TileEntityCrystallizer.addRecipe(reduced_silver, crystal_silver, CRYSTALLIZE, Core.registry.aqua_regia);
            TileEntityCrystallizer.addRecipe(reduced_lead, crystal_lead, CRYSTALLIZE, Core.registry.aqua_regia);
        } else {
            //clean gravel -> reduced chunks
            int r = (int)REDUCE;
            TileEntitySlagFurnace.SlagRecipes.register(clean, r, reduced, REDUCE - r, reduced);
            //reduced chunks -> crystals
            if (oreType != OreType.LEAD) {
                TileEntityCrystallizer.addRecipe(reduced, crystal, CRYSTALLIZE, Core.registry.aqua_regia);
            }
        }
    }
    
    void handleNewOre(String oreClass, ItemStack ore) {
        OreType oreType = OreType.fromOreClass(oreClass);
        if (oreType == null) {
            return;
        }
        if (ItemOreProcessing.OD_ores.contains(oreClass)) {
            oreType.enable();
            addProcessingFront(oreType, ore);
            createProccessingBody(oreType);
            ItemStack ingot = bestIngots.get(oreClass);
            if (ingot == null) {
                ingot = oreType.processingResult;
            }
            if (ingot == null) {
                ingot = FurnaceRecipes.smelting().getSmeltingResult(ore);
            }
            if (ingot != null) {
                if (oreType == OreType.GALENA) {
                    addProcessingEnds(OreType.LEAD, ore, new ItemStack(Core.registry.lead_ingot));
                    addProcessingEnds(OreType.SILVER, ore, new ItemStack(Core.registry.silver_ingot));
                    addProcessingEnds(OreType.GALENA, ore, new ItemStack(Core.registry.silver_ingot));
                } else {
                    addProcessingEnds(oreType, ore, ingot);
                    oreType.processingResult = ingot;
                }
            }
        }
    }

    @SubscribeEvent
    public void registerOre(OreRegisterEvent evt) {
        handleNewOre(evt.Name, evt.Ore);
    }
    
    void loadWater() {
        FluidStack h20 = FluidContainerRegistry.getFluidForFilledItem(new ItemStack(Items.bucketWater));
        for (FluidContainerData container : FluidContainerRegistry.getRegisteredFluidContainerData()) {
            FluidStack liq = container.fluid;
            if (h20.isFluidEqual(liq) && liq.amount == FluidContainerRegistry.BUCKET_VOLUME && container.filledContainer != null) {
                OreDictionary.registerOre(waterBucket, container.filledContainer);
            }
        }
    }
    
    public void addDictOres() {
        loadWater();
        
        for (OreType type : ItemOreProcessing.OreType.values()) {
            String oreClass = type.OD_ore;
            String ingotClass = type.OD_ingot;
            
            if (oreClass == null) {
                continue;
            }
            
            ItemStack bestIngot = null;
            Iterable<ItemStack> oreList = OreDictionary.getOres(oreClass);
            if (oreList == null || !oreList.iterator().hasNext()) {
                continue;
            }
            for (ItemStack ore : oreList) {
                ItemStack smeltsTo = FurnaceRecipes.smelting().getSmeltingResult(ore);
                if (smeltsTo == null) {
                    if (ingotClass == null) {
                        break;
                    }
                    Iterable<ItemStack> ingotList = OreDictionary.getOres(ingotClass);
                    if (ingotList == null) {
                        continue;
                    }
                    Iterator<ItemStack> it = ingotList.iterator();
                    if (!it.hasNext()) {
                        continue;
                    }
                    bestIngot = it.next();
                    break;
                }
                if (bestIngot == null || ore.getItemDamage() != 0) {
                    bestIngot = smeltsTo;
                }
            }
            if (bestIngot == null) {
                continue;
            }
            bestIngots.put(oreClass, bestIngot);
            for (ItemStack ore : oreList) {
                handleNewOre(oreClass, ore);
            }
        }
        MinecraftForge.EVENT_BUS.register(this);
    }
    
}
