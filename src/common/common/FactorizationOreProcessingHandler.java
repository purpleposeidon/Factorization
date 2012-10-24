package factorization.common;

import java.util.ArrayList;
import java.util.HashMap;

import net.minecraft.src.Block;
import net.minecraft.src.FurnaceRecipes;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.OreDictionary.OreRegisterEvent;
import factorization.common.ItemOreProcessing.OreType;

public class FactorizationOreProcessingHandler {
    private HashMap<String, ItemStack> bestIngots = new HashMap();
    
    /** This adds an ore to the processing chain: Grinds the ore into gravel */
    void addProcessingFront(OreType oreType, ItemStack ore) {
        ItemStack dirty = Core.registry.ore_dirty_gravel.makeStack(oreType);
        TileEntityGrinder.addRecipe(ore, dirty, 1.4F);
    }
    
    /** This (maybe) adds an ore to the processing ends (converting anything to ingots). It only does this if the ingot is better. */
    void addProcessingEnds(OreType oreType, ItemStack ore, ItemStack ingot) {
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
            FurnaceRecipes.smelting().addSmelting(is.itemID, is.getItemDamage(), ingot);
        }
        
        //SLAG
        TileEntitySlagFurnace.SlagRecipes.register(ore, 1.2F, ingot, 0.4F, Block.stone);
        TileEntitySlagFurnace.SlagRecipes.register(dirty, 1.42857142857143F, ingot, 0.2F, Block.dirt);
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
        
        //dirty gravel -> clean gravel
        TileEntityMixer.addRecipe(
                new ItemStack[] { dirty, new ItemStack(Item.bucketWater) },
                new ItemStack[] { clean, new ItemStack(Item.bucketEmpty), new ItemStack(Core.registry.sludge) });
        if (oreType == OreType.GALENA) {
            ItemStack reduced_silver = Core.registry.ore_reduced.makeStack(OreType.SILVER);
            ItemStack reduced_lead = Core.registry.ore_reduced.makeStack(OreType.LEAD);
            ItemStack crystal_silver = Core.registry.ore_crystal.makeStack(OreType.SILVER);
            ItemStack crystal_lead = Core.registry.ore_crystal.makeStack(OreType.LEAD);
            
            TileEntitySlagFurnace.SlagRecipes.register(clean, 1.1F, reduced_silver, 1.6F, reduced_lead);
            TileEntityCrystallizer.addRecipe(reduced_silver, crystal_silver, 1.5F, new ItemStack(Core.registry.acid), 0);
            TileEntityCrystallizer.addRecipe(reduced_lead, crystal_lead, 1.5F, new ItemStack(Core.registry.acid), 0);
        } else {
            //clean gravel -> reduced chunks
            TileEntitySlagFurnace.SlagRecipes.register(clean, 1, reduced, 0.42857142857143F, reduced);
            //reduced chunks -> crystals
            TileEntityCrystallizer.addRecipe(reduced, crystal, 1.5F, new ItemStack(Core.registry.acid), 0);
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
                } else {
                    addProcessingEnds(oreType, ore, ingot);
                    oreType.processingResult = ingot;
                }
            }
        }
    }

    @ForgeSubscribe
    public void registerOre(OreRegisterEvent evt) {
        handleNewOre(evt.Name, evt.Ore);
    }
    
    void addDictOres() {
        for (String oreClass : ItemOreProcessing.OD_ores) {
            ItemStack bestIngot = null;
            Iterable<ItemStack> oreList = OreDictionary.getOres(oreClass);
            if (oreList == null || !oreList.iterator().hasNext()) {
                continue;
            }
            for (ItemStack ore : oreList) {
                ItemStack smeltsTo = FurnaceRecipes.smelting().getSmeltingResult(ore);
                if (smeltsTo == null) {
                    continue;
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
    }
    
}
