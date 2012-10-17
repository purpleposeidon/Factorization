package factorization.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import net.minecraft.src.AchievementList;
import net.minecraft.src.Block;
import net.minecraft.src.CraftingManager;
import net.minecraft.src.CreativeTabs;
import net.minecraft.src.EntityItem;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.FurnaceRecipes;
import net.minecraft.src.IChunkProvider;
import net.minecraft.src.IInventory;
import net.minecraft.src.IRecipe;
import net.minecraft.src.InventoryPlayer;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.MapColor;
import net.minecraft.src.Material;
import net.minecraft.src.ModLoader;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.World;
import net.minecraft.src.WorldGenMinable;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.DungeonHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.OreDictionary.OreRegisterEvent;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.ICraftingHandler;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.IWorldGenerator;
import cpw.mods.fml.common.Side;
import cpw.mods.fml.common.TickType;
import cpw.mods.fml.common.registry.GameRegistry;
import factorization.api.IActOnCraft;
import factorization.common.ItemOreProcessing.OreType;

public class Registry implements ICraftingHandler, IWorldGenerator, ITickHandler {
    static public final int MechaKeyCount = 3;

    public ItemFactorization item_factorization;
    public ItemBlockResource item_resource;
    public BlockFactorization factory_block, factory_rendering_block = null;
    public BlockLightAir lightair_block;
    public BlockResource resource_block;

    public ItemStack cutter_item, router_item, maker_item, stamper_item, packager_item,
            barrel_item,
            queue_item, lamp_item, air_item, sentrydemon_item,
            slagfurnace_item, battery_item_hidden, solar_turbine_item, heater_item,
            mirror_item_hidden,
            leadwire_item, grinder_item, mixer_item, crystallizer_item,
            greenware_item;
    public ItemStack silver_ore_item, silver_block_item, lead_block_item,
            dark_iron_block_item, mechaworkshop_item;
    public ItemStack is_factory, is_lamp, is_lightair;
    public ItemCraft item_craft;
    public ItemBagOfHolding bag_of_holding;
    public ItemPocketTable pocket_table;
    public ItemDemon tiny_demon, bound_tiny_demon;
    public ItemWandOfCooling wand_of_cooling;
    public ItemCraftingComponent diamond_shard;
    public IRecipe diamond_shard_recipe;
    public ItemStack diamond_shard_packet;
    public IRecipe boh_upgrade_recipe;
    public ItemWrathIgniter wrath_igniter;
    public ItemCraftingComponent silver_ingot, lead_ingot, dark_iron;
    public ItemCraftingComponent mecha_chasis;
    public MechaArmor mecha_head, mecha_chest, mecha_leg, mecha_foot;
    public MechaBuoyantBarrel mecha_buoyant_barrel;
    public MechaCobblestoneDrive mecha_cobble_drive;
    public MechaMountedPiston mecha_mounted_piston;
    public ItemMachineUpgrade router_item_filter, router_machine_filter, router_speed,
            router_thorough, router_throughput, router_eject;
    public ItemMachineUpgrade barrel_enlarge;
    public ItemStack fake_is;
    public ItemCraftingComponent acid, magnet, insulated_coil, motor, fan, diamond_cutting_head;
    public ItemChargeMeter charge_meter;
    public ItemMirror mirror;
    public ItemBattery battery;
    public ItemOreProcessing ore_dirty_gravel, ore_clean_gravel, ore_reduced, ore_crystal;
    public ItemCraftingComponent sludge;
    public ItemCraftingComponent inverium;
    public ItemSculptingTool sculpt_tool;
    public ItemAngularSaw angular_saw;
    public ItemCraftingComponent heatHole, logicMatrix, logicMatrixIdentifier, logicMatrixProgrammer;

    public Material materialMachine = new Material(MapColor.ironColor);

    WorldGenMinable silverGen;

    void makeBlocks() {
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            //Theoretically, not necessary. I bet BUKKIT would flip its shit tho.
            factory_rendering_block = new BlockFactorization(Core.factory_block_id);
            Block.blocksList[Core.factory_block_id] = null;
        }
        factory_block = new BlockFactorization(Core.factory_block_id);
        lightair_block = new BlockLightAir(Core.lightair_id);
        resource_block = new BlockResource(Core.resource_id);
        is_factory = new ItemStack(factory_block);
        is_lightair = new ItemStack(lightair_block);

        GameRegistry.registerBlock(factory_block, ItemFactorization.class);
        GameRegistry.registerBlock(lightair_block);
        GameRegistry.registerBlock(resource_block, ItemBlockResource.class);
        GameRegistry.registerCraftingHandler(this);
        GameRegistry.registerWorldGenerator(this);

        factory_block.setCreativeTab(CreativeTabs.tabRedstone);
    }

    void registerSimpleTileEntities() {
        FactoryType.registerTileEntities();
        //TileEntity renderers are registered in the client's mod_Factorization

        ModLoader.registerEntityID(TileEntityWrathLamp.RelightTask.class, "factory_relight_task", Core.entity_relight_task_id);
    }

    private void addName(Object what, String name) {
        Core.proxy.addName(what, name);
    }

    HashSet<Integer> added_ids = new HashSet();

    public int itemID(String name, int default_id) {
        int id = Integer.parseInt(Core.config.getOrCreateIntProperty(name, "item", default_id).value);
        if (added_ids.contains(default_id)) {
            throw new RuntimeException("Default ID already used: " + default_id);
        }
        if (Item.itemsList[id] != null) {
            throw new RuntimeException("Item ID conflict: " + id + " is already taken by "
                    + Item.itemsList[id] + "; tried to use it for Factorization " + name);
        }
        added_ids.add(default_id);
        return id;
    }

    void makeItems() {
        ore_dirty_gravel = new ItemOreProcessing(itemID("oreDirtyGravel", 9034), 2 * 16 + 4, "dirtyGravel");
        ore_clean_gravel = new ItemOreProcessing(itemID("oreCleanGravel", 9035), 2 * 16 + 5, "cleanGravel");
        ore_reduced = new ItemOreProcessing(itemID("oreReduced", 9036), 2 * 16 + 6, "reduced");
        ore_crystal = new ItemOreProcessing(itemID("oreCrystal", 9037), 2 * 16 + 7, "crystal");
        ore_dirty_gravel.addEnglishNames("Dirty ", " Gravel");
        ore_clean_gravel.addEnglishNames("Clean ", " Chunks");
        ore_reduced.addEnglishNames("Reduced ", " Chunks");
        ore_crystal.addEnglishNames("Crystalline ", "");
        sludge = new ItemCraftingComponent(itemID("sludge", 9039), "item.sludge", 2 * 16 + 8);
        addName(sludge, "Sludge");

        //ItemBlocks
        item_factorization = (ItemFactorization) Item.itemsList[Core.factory_block_id];
        item_resource = (ItemBlockResource) Item.itemsList[resource_block.blockID];

        //BlockFactorization stuff
        router_item = FactoryType.ROUTER.itemStack("Router");
        cutter_item = FactoryType.CUTTER.itemStack("Stack Cutter");
        barrel_item = FactoryType.BARREL.itemStack("Barrel");
        queue_item = FactoryType.QUEUE.itemStack("Queue");
        maker_item = FactoryType.MAKER.itemStack("Craftpacket Maker");
        stamper_item = FactoryType.STAMPER.itemStack("Craftpacket Stamper");
        lamp_item = FactoryType.LAMP.itemStack("Wrathlamp");
        packager_item = FactoryType.PACKAGER.itemStack("Packager");
        sentrydemon_item = FactoryType.SENTRYDEMON.itemStack("Sentry Demon");
        slagfurnace_item = FactoryType.SLAGFURNACE.itemStack("Slag Furnace");
        battery_item_hidden = FactoryType.BATTERY.itemStack("Battery Block");
        solar_turbine_item = FactoryType.SOLARTURBINE.itemStack("Solar Turbine");
        heater_item = FactoryType.HEATER.itemStack("Furnace Heater");
        mirror_item_hidden = FactoryType.MIRROR.itemStack("Reflective Mirror");
        leadwire_item = FactoryType.LEADWIRE.itemStack("Lead Wire");
        grinder_item = FactoryType.GRINDER.itemStack("Grinder");
        mixer_item = FactoryType.MIXER.itemStack("Mixer");
        crystallizer_item = FactoryType.CRYSTALLIZER.itemStack("Crystallizer");
        greenware_item = FactoryType.GREENWARE.itemStack("Clay Sculpture");

        //BlockResource stuff
        silver_ore_item = ResourceType.SILVERORE.itemStack("Silver Ore");
        silver_block_item = ResourceType.SILVERBLOCK.itemStack("Block of Silver");
        lead_block_item = ResourceType.LEADBLOCK.itemStack("Block of Lead");
        dark_iron_block_item = ResourceType.DARKIRONBLOCK.itemStack("Block of Dark Iron");
        mechaworkshop_item = ResourceType.MECHAMODDER.itemStack("Mecha-Modder");

        lead_ingot = new ItemCraftingComponent(itemID("leadIngot", 9014), "Lead Ingot", 16 * 3 + 3);
        silver_ingot = new ItemCraftingComponent(itemID("silverIngot", 9015), "Silver Ingot", 16 * 3 + 4);
        OreDictionary.registerOre("oreSilver", silver_ore_item);
        OreDictionary.registerOre("ingotSilver", new ItemStack(silver_ingot));
        OreDictionary.registerOre("ingotLead", new ItemStack(lead_ingot));
        addName(lead_ingot, "Lead Ingot");
        addName(silver_ingot, "Silver Ingot");

        //Darkness & Evil
        diamond_shard = new ItemCraftingComponent(itemID("diamondShard", 9006), "item.diamondshard", 3 * 16);
        addName(diamond_shard, "Diamond Shard");
        wrath_igniter = new ItemWrathIgniter(itemID("wrathIgniter", 9007));
        addName(wrath_igniter, "Wrath Igniter");
        dark_iron = new ItemCraftingComponent(itemID("darkIron", 9008), "item.darkiron", 3 * 16 + 2);
        addName(dark_iron, "Dark Iron Ingot");

        bag_of_holding = new ItemBagOfHolding(itemID("bagOfHolding", 9001));
        addName(bag_of_holding, "Bag of Holding");

        bound_tiny_demon = new ItemDemon(itemID("boundTinyDemon", 9003));
        tiny_demon = new ItemDemon(itemID("tinyDemon", 9004));
        addName(bound_tiny_demon, "Bound Tiny Demon");
        addName(tiny_demon, "Tiny Demon");
        logicMatrixProgrammer = new ItemMatrixProgrammer(itemID("logicMatrixProgrammer", 9043), "Logic Matrix Programmer", 1*16 + 6);
        DungeonHooks.addDungeonLoot(new ItemStack(logicMatrixProgrammer), 50); //XXX TODO: Temporary, put these on asteroids.
        DungeonHooks.addDungeonMob("Creeper", 1);
        String THATS_SOME_VERY_NICE_SOURCE_CODE_YOU_HAVE_THERE[] = {
                "##  ##",
                "##  ##",
                "  ##  ",
                " #### ",
                " #  # "
        };
        logicMatrix = new ItemCraftingComponent(itemID("logicMatrix", 9044), "Logic Matrix", 1*16 + 10);
        logicMatrixIdentifier = new ItemCraftingComponent(itemID("logicMatrixID", 9045), "Logic Matrix: Identifier", 1*16 + 11);
        heatHole = new ItemCraftingComponent(itemID("heatHole", 9046), "Heat Hole", 1*16 + 9);

        wand_of_cooling = new ItemWandOfCooling(itemID("wandOfCooling", 9005));
        addName(wand_of_cooling, "Wand of Cooling");

        router_item_filter = new ItemMachineUpgrade(itemID("routerItemFilter", 9016), "Item Filter", "Router Upgrade", FactoryType.ROUTER, 0);
        router_machine_filter = new ItemMachineUpgrade(itemID("routerMachineFilter", 9017), "Machine Filter", "Router Upgrade", FactoryType.ROUTER, 1);
        router_speed = new ItemMachineUpgrade(itemID("routerSpeed", 9018), "Speed Boost", "Router Upgrade", FactoryType.ROUTER, 2);
        router_thorough = new ItemMachineUpgrade(itemID("routerThorough", 9019), "Thoroughness", "Router Upgrade", FactoryType.ROUTER, 3);
        router_throughput = new ItemMachineUpgrade(itemID("routerThroughput", 9020), "Bandwidth", "Router Upgrade", FactoryType.ROUTER, 4);
        router_eject = new ItemMachineUpgrade(itemID("routerEject", 9031), "Ejector", "Router Upgrade", FactoryType.ROUTER, 5);

        barrel_enlarge = new ItemMachineUpgrade(itemID("barrelEnlarge", 9032), "Extra-Dimensional Storage", "Barrel Upgrade", FactoryType.BARREL, 6);

        //Electricity
        acid = new ItemAcidBottle(itemID("acid", 9024), "Sulfuric Acid", 16 * 3 + 5);
        magnet = new ItemCraftingComponent(itemID("magnet", 9025), "Magnet", 16 * 3 + 6);
        insulated_coil = new ItemCraftingComponent(itemID("coil", 9026), "Insulated Coil", 16 * 3 + 7);
        motor = new ItemCraftingComponent(itemID("motor", 9027), "Motor", 16 * 3 + 8);
        fan = new ItemCraftingComponent(itemID("fan", 9028), "Fan", 16 * 3 + 9);
        diamond_cutting_head = new ItemCraftingComponent(itemID("diamondCuttingHead", 9038), "Diamond Cutting Head", 16 * 3 + 10);
        charge_meter = new ItemChargeMeter(itemID("chargemeter", 9029));
        addName(charge_meter, "Charge Meter");
        mirror = new ItemMirror(itemID("mirror", 9030));
        addName(mirror, "Reflective Mirror");
        battery = new ItemBattery(itemID("battery", 9033));
        addName(battery, "Battery Block");

        //Industrial
        item_craft = new ItemCraft(itemID("itemCraftId", 9000));
        addName(item_craft, "Craftpacket");

        //Mecha-items
        mecha_head.setSlotCount(5);
        mecha_chest.setSlotCount(8);
        mecha_leg.setSlotCount(6);
        mecha_foot.setSlotCount(4);

        mecha_chasis = new ItemCraftingComponent(itemID("mechaChasis", 9009), "item.mechachasis", 5);
        addName(mecha_chasis, "Mecha-Chassis");
        //mecha_ITEMS created in make_recipes_side()
        //Mecha-armor uses up to Item ID 9013.
        addName(mecha_head, "Mecha-Helmet");
        addName(mecha_chest, "Mecha-Chestplate");
        addName(mecha_leg, "Mecha-Leggings");
        addName(mecha_foot, "Mecha-Boots");
        mecha_buoyant_barrel = new MechaBuoyantBarrel(itemID("mechaBouyantBarrel", 9021));
        mecha_cobble_drive = new MechaCobblestoneDrive(itemID("mechaCobbleDrive", 9022));
        mecha_mounted_piston = new MechaMountedPiston(itemID("mechaMountedPiston", 9023));
        addName(mecha_buoyant_barrel, "Buoyant Barrel");
        addName(mecha_cobble_drive, "Cobblestone Drive");
        addName(mecha_mounted_piston, "Mounted Piston");
        angular_saw = new ItemAngularSaw(itemID("angularSaw", 9042));
        addName(angular_saw, "Angular Saw");
        MinecraftForge.setToolClass(angular_saw, "pickaxe", 3);
        
        //ceramics
        sculpt_tool = new ItemSculptingTool(itemID("sculptTool", 9041));
        addName(sculpt_tool, "Sculpting Tool");
        
        //inverium = new ItemInverium(itemID("inverium", 9040), "item.inverium", 12*16 + 0, 11);
        inverium = new ItemInverium(itemID("inverium", 9040), "item.inverium", 12*16 + 0, 1);
        addName(inverium, "Inverium Drop");

        //Misc
        pocket_table = new ItemPocketTable(itemID("pocketCraftingTable", 9002));
        addName(pocket_table, "Pocket Crafting Table");
    }

    void recipe(ItemStack res, Object... params) {
        ModLoader.addRecipe(res, params);
    }

    void shapelessRecipe(ItemStack res, Object... params) {
        ModLoader.addShapelessRecipe(res, params);
    }

    void oreRecipe(ItemStack res, Object... params) {
        IRecipe rec = new ShapedOreRecipe(res, params);
        CraftingManager.getInstance().getRecipeList().add(rec);
    }

    void shapelessOreRecipe(ItemStack res, Object... params) {
        IRecipe rec = new ShapelessOreRecipe(res, params);
        CraftingManager.getInstance().getRecipeList().add(rec);
    }

    void makeRecipes() {
        recipe(new ItemStack(Block.stoneDoubleSlab, 2, 6),
                " - ",
                "-S-",
                " - ",
                '-', new ItemStack(Block.stoneSingleSlab, 1, 0),
                'S', Block.stone);
    recipe(new ItemStack(Block.stoneDoubleSlab, 2, 6),
        "-",
        "-",
        '-', new ItemStack(Block.stoneSingleSlab, 1, 6));
        shapelessRecipe(new ItemStack(dark_iron, 4), dark_iron_block_item);
        recipe(dark_iron_block_item,
                "II",
                "II",
                'I', dark_iron);

        // Bag of holding

        ItemStack BOH = new ItemStack(bag_of_holding, 1); //we don't call bag_of_holding.init(BOH) because that would show incorrect info
        recipe(BOH,
                "LOL",
                "ILI",
                " I ",
                'I', dark_iron,
                'O', Item.enderPearl,
                'L', Item.leather); // LOL!
        shapelessRecipe(BOH, BOH, dark_iron, Item.enderPearl, Item.leather);
        boh_upgrade_recipe = FactorizationUtil.createShapelessRecipe(BOH, BOH, dark_iron, Item.enderPearl, Item.leather);
        // Pocket Crafting Table (pocket table)
        recipe(new ItemStack(pocket_table),
                "# ",
                " |",
                '#', Block.workbench,
                '|', Item.stick);

        // tiny demons
        recipe(new ItemStack(logicMatrixIdentifier),
                "MiX",
                'M', logicMatrix,
                'i', Item.spiderEye,
                'X', logicMatrixProgrammer);
        recipe(new ItemStack(logicMatrixProgrammer),
                "MiX",
                'M', logicMatrix,
                'i', dark_iron,
                'X', logicMatrixProgrammer);
        
        TileEntityCrystallizer.addRecipe(new ItemStack(Block.stone), new ItemStack(logicMatrix), 1, new ItemStack(Item.potion), 1);
        // wand of cooling
        TileEntityCrystallizer.addRecipe(new ItemStack(Item.magmaCream), new ItemStack(heatHole), 1, new ItemStack(Item.potion), 1);
        recipe(new ItemStack(wand_of_cooling),
                " OD",
                " IO",
                "I  ",
                'O', Block.obsidian,
                'D', heatHole,
                'I', Item.ingotIron);
        recipe(new ItemStack(wand_of_cooling),
                "DO ",
                "OI ",
                "  I",
                'O', Block.obsidian,
                'D', heatHole,
                'I', Item.ingotIron);

        // diamond shard
        //How do we feel about 12 shards for 9 diamonds?
        diamond_shard_recipe = FactorizationUtil.createShapedRecipe(new ItemStack(diamond_shard, 18),
                "OTO",
                "TDT",
                "OTO",
                'O', Block.obsidian,
                'T', Block.tnt,
                'D', Block.blockDiamond);
        ItemCraft.addStamperRecipe(diamond_shard_recipe);
        diamond_shard_packet = new ItemStack(item_craft);
        diamond_shard_packet.setItemDamage(0xFF);
        for (int i : new int[] { 0, 2, 6, 8 }) {
            item_craft.addItem(diamond_shard_packet, i, new ItemStack(Block.obsidian));
        }
        for (int i : new int[] { 1, 3, 5, 7 }) {
            item_craft.addItem(diamond_shard_packet, i, new ItemStack(Block.tnt));
        }
        item_craft.addItem(diamond_shard_packet, 4, new ItemStack(Block.blockDiamond));
        recipe(diamond_shard_packet,
                "OTO",
                "TDT",
                "OTO",
                'O', Block.obsidian,
                'T', Block.tnt,
                'D', Block.blockDiamond);

        //wrathfire igniter
        recipe(new ItemStack(wrath_igniter),
                "D ",
                " B",
                'D', diamond_shard,
                'B', Block.netherBrick);
        //recipe(new ItemStack(wrath_igniter), // ah hell naw
        //		"D ",
        //		" B",
        //		'D', Item.diamond,
        //		'B', Block.netherBrick);

        //Resources
        recipe(new ItemStack(lead_ingot, 9), "#", '#', lead_block_item);
        recipe(new ItemStack(silver_ingot, 9), "#", '#', silver_block_item);
        oreRecipe(lead_block_item, "###", "###", "###", '#', "ingotLead");
        oreRecipe(silver_block_item, "###", "###", "###", '#', "ingotSilver");
        FurnaceRecipes.smelting().addSmelting(resource_block.blockID, 0 /* MD for silver */, new ItemStack(silver_ingot));

        //mecha armor
        recipe(new ItemStack(mecha_chasis),
                "III",
                "InI",
                "III",
                'I', Item.ingotIron,
                'n', Item.goldNugget);
        recipe(new ItemStack(mecha_head),
                "###",
                "# #",
                '#', mecha_chasis);
        recipe(new ItemStack(mecha_chest),
                "# #",
                "###",
                "###",
                '#', mecha_chasis);
        recipe(new ItemStack(mecha_leg),
                "###",
                "# #",
                "# #",
                '#', mecha_chasis);
        recipe(new ItemStack(mecha_foot),
                "# #",
                "# #",
                '#', mecha_chasis);
        //mecha armor upgrades

        recipe(new ItemStack(mecha_buoyant_barrel),
                "W_W",
                "PBP",
                "WVW",
                'W', Block.planks,
                '_', Block.pressurePlatePlanks,
                'P', Block.pistonBase,
                'B', barrel_item,
                'V', Item.boat);

        ItemStack is_cobble_drive = new ItemStack(mecha_cobble_drive);
        recipe(is_cobble_drive,
                "OPO",
                "WTL",
                "OOO",
                'O', Block.obsidian,
                'P', Block.pistonBase,
                'W', Item.bucketWater,
                'T', Item.pickaxeSteel,
                'L', Item.bucketLava);
        recipe(is_cobble_drive,
                "OPO",
                "LTW",
                "OOO",
                'O', Block.obsidian,
                'P', Block.pistonBase,
                'W', Item.bucketWater,
                'T', Item.pickaxeSteel,
                'L', Item.bucketLava);
        recipe(new ItemStack(mecha_mounted_piston),
                "CNC",
                "LSL",
                "CCC",
                'C', Block.cobblestone,
                'S', Block.pistonStickyBase,
                'N', Block.pistonBase,
                'L', Block.lever);
        recipe(new ItemStack(angular_saw),
                "OH",
                "MY",
                "! ",
                'O', new ItemStack(diamond_cutting_head),
                'M', new ItemStack(motor),
                'Y', new ItemStack(Item.ingotIron),
                '!', lead_ingot);
        
        //ceramics
        recipe(new ItemStack(sculpt_tool),
                " c",
                "/ ",
                'c', Item.clay,
                '/', Item.stick);
        ItemSculptingTool.addModeChangeRecipes();
        
        //inverium
        recipe(new ItemStack(inverium, 1, 1),
                "LGL",
                "GDG",
                "LGL",
                'L', lead_ingot,
                'G', Item.ingotGold,
                'D', Item.diamond);

        // BlockFactorization recipes
        // router
        recipe(router_item,
                "MMM",
                "oIO",
                "MMM",
                'M', dark_iron,
                'I', Item.egg,
                'o', Item.enderPearl,
                'O', Item.eyeOfEnder);
        recipe(router_item,
                "MMM",
                "OIo",
                "MMM",
                'M', dark_iron,
                'I', Item.egg,
                'o', Item.enderPearl,
                'O', Item.eyeOfEnder);
        // router upgrades
        recipe(new ItemStack(router_item_filter),
                "ITI",
                "GDG",
                "ICI",
                'I', dark_iron,
                'T', Block.torchRedstoneActive,
                'D', logicMatrixIdentifier,
                'G', Item.ingotGold,
                'C', Block.chest);

        oreRecipe(new ItemStack(router_machine_filter),
                "ITI",
                "SDS",
                "IBI",
                'I', dark_iron,
                'T', Block.torchRedstoneActive,
                'D', logicMatrixIdentifier,
                'S', "ingotSilver",
                'B', Item.book);

        recipe(new ItemStack(router_speed),
                "ISI",
                "SCS",
                "ISI",
                'I', dark_iron,
                'S', Item.sugar,
                'C', Item.cake);
        recipe(new ItemStack(router_thorough),
                "ISI",
                "SSS",
                "ISI",
                'I', dark_iron,
                'S', Block.slowSand);
        recipe(new ItemStack(router_throughput),
                "IBI",
                "B!B",
                "IBI",
                'I', dark_iron,
                'B', Item.blazePowder,
                '!', Item.egg);
        recipe(new ItemStack(router_eject),
                "IWI",
                "C_C",
                "IPI",
                'I', dark_iron,
                'W', Block.planks,
                'C', Block.cobblestone,
                '_', Block.pressurePlatePlanks,
                'P', Block.pistonBase);
        recipe(new ItemStack(barrel_enlarge),
                "IOI",
                "BWB",
                "ILI",
                'I', dark_iron,
                'W', barrel_item,
                'O', Item.enderPearl,
                'B', Item.blazeRod,
                'L', Item.leather);

        // Barrel
        recipe(barrel_item,
                "W-W",
                "W W",
                "WWW",
                'W', Block.wood,
                '-', new ItemStack(Block.woodSingleSlab, 1, -1));

        // Craft maker
        recipe(maker_item,
                "#p#",
                "# #",
                "#C#",
                '#', Block.cobblestone,
                'p', Block.pistonBase,
                'C', Block.workbench);
        fake_is = maker_item;

        // Craft stamper
        recipe(stamper_item,
                "#p#",
                "III",
                "#C#",
                '#', Block.cobblestone,
                'p', Block.pistonBase,
                'I', Item.ingotIron,
                'C', Block.workbench);

        //Packager
        recipe(packager_item,
                "#M#",
                "# #",
                "#S#",
                '#', Block.cobblestone,
                'M', maker_item,
                'S', stamper_item);

        // Wrath lamp
        oreRecipe(lamp_item,
                "ISI",
                "GWG",
                "ISI",
                'I', dark_iron,
                'S', "ingotSilver",
                'G', Block.thinGlass,
                'W', new ItemStack(wrath_igniter, 1, -1));

        //sentry demon
        //		recipe(sentrydemon_item,
        //				"###",
        //				"#D#",
        //				"###",
        //				'#', Block.fenceIron,
        //				'D', bound_tiny_demon);

        //Slag furnace
        recipe(slagfurnace_item,
                "CFC",
                "C C",
                "CFC",
                'C', Block.cobblestone,
                'F', Block.stoneOvenIdle);
        createOreProcessingPath(new ItemStack(Block.oreIron), new ItemStack(Item.ingotIron), ItemOreProcessing.OreType.IRON);
        createOreProcessingPath(new ItemStack(Block.oreGold), new ItemStack(Item.ingotGold), ItemOreProcessing.OreType.GOLD);
        for (Block redstone : Arrays.asList(Block.oreRedstone, Block.oreRedstoneGlowing)) {
            TileEntitySlagFurnace.SlagRecipes.register(redstone, 5.8F, Item.redstone, 0.2F, Block.stone);
            //most ores give 0.4F stone, but redstone is dense.
            //mining redstone normally gives 4 to 6 ore. 5.8F should get you a slightly better yield.
        }

        //mecha-workshop
        recipe(mechaworkshop_item,
                "MCM",
                "i i",
                "i i",
                'C', Block.workbench,
                'M', mecha_chasis,
                'i', Item.ingotIron);
        recipe(greenware_item,
                "c",
                "-",
                'c', Item.clay,
                '-', new ItemStack(Block.woodSingleSlab, 1, -1));

        //Electricity

        
        shapelessRecipe(new ItemStack(acid), Item.gunpowder, Item.gunpowder, Item.coal, Item.potion);
        shapelessOreRecipe(new ItemStack(acid), "dustSulfur", Item.coal, Item.potion);
        recipe(new ItemStack(fan),
                "I I",
                " I ",
                "I I",
                'I', Item.ingotIron);
        recipe(solar_turbine_item,
                "###",
                "#F#",
                "#M#",
                '#', Block.glass,
                'F', fan,
                'M', motor);
        oreRecipe(new ItemStack(charge_meter),
                "WSW",
                "W/W",
                "LIL",
                'W', Block.planks,
                'S', Item.sign,
                '/', Item.stick,
                'L', "ingotLead",
                'I', Item.ingotIron);
        oreRecipe(new ItemStack(battery, 1, 2),
                "ILI",
                "LAL",
                "ILI",
                'I', Item.ingotIron,
                'L', "ingotLead",
                'A', acid);
        for (int damage : new int[] { 1, 2 }) {
            recipe(new ItemStack(magnet),
                    "WWW",
                    "WIW",
                    "WBW",
                    'W', leadwire_item,
                    'I', Item.ingotIron,
                    'B', new ItemStack(battery, 1, damage));
            recipe(new ItemStack(battery, 1, damage - 1),
                    "W",
                    "B",
                    "W",
                    'W', leadwire_item,
                    'B', new ItemStack(battery, 1, damage));
        }

        oreRecipe(heater_item,
                "CCC",
                "L L",
                "CCC",
                'C', insulated_coil,
                'L', "ingotLead");
        oreRecipe(new ItemStack(insulated_coil),
                "LLL",
                "LCL",
                "LLL",
                'L', "ingotLead",
                'C', Block.blockClay);
        oreRecipe(new ItemStack(motor),
                "CIC",
                "CMC",
                "LIL",
                'C', insulated_coil,
                'M', magnet,
                'L', "ingotLead",
                'I', Item.ingotIron);
        oreRecipe(new ItemStack(mirror),
                "SSS",
                "S#S",
                "SSS",
                'S', "ingotSilver",
                '#', Block.thinGlass);
        ItemStack with_8 = leadwire_item.copy();
        with_8.stackSize = 8;
        oreRecipe(with_8,
                "LLL",
                "LLL",
                'L', "ingotLead");
        recipe(new ItemStack(diamond_cutting_head),
                "SSS",
                "SIS",
                "SSS",
                'S', diamond_shard,
                'I', Item.ingotIron);
        recipe(grinder_item,
                "I*I",
                "IMI",
                "LIL",
                'I', Item.ingotIron,
                '*', diamond_cutting_head,
                'M', motor,
                'L', lead_ingot);
        recipe(grinder_item,
                "IMI",
                "I*I",
                "LIL",
                'I', Item.ingotIron,
                '*', diamond_cutting_head,
                'M', motor,
                'L', lead_ingot);
        TileEntityGrinder.addRecipe(new ItemStack(Block.stone), new ItemStack(Block.cobblestone), 1);
        TileEntityGrinder.addRecipe(new ItemStack(Block.cobblestone), new ItemStack(Block.gravel), 1);
        TileEntityGrinder.addRecipe(new ItemStack(Block.gravel), new ItemStack(Block.sand), 1);
        TileEntityGrinder.addRecipe(new ItemStack(Block.grass), new ItemStack(Block.dirt), 1);
        TileEntityGrinder.addRecipe(new ItemStack(Block.mycelium), new ItemStack(Block.dirt), 1);
        TileEntityGrinder.addRecipe(new ItemStack(Block.oreDiamond), new ItemStack(Item.diamond), 2.25F);
        TileEntityGrinder.addRecipe(new ItemStack(Block.oreRedstone), new ItemStack(Item.redstone), 6.5F);
        TileEntityGrinder.addRecipe(new ItemStack(Block.oreLapis), new ItemStack(Item.dyePowder, 1, 4), 8);
        TileEntityGrinder.addRecipe(new ItemStack(Block.oreCoal), new ItemStack(Item.coal), 3.5F);
        recipe(mixer_item,
                " X ",
                "WMW",
                "LUL",
                'X', fan,
                'W', Item.bucketWater,
                'M', motor,
                'L', lead_ingot,
                'U', Item.cauldron);
        TileEntityMixer.addRecipe(
                new ItemStack[] { new ItemStack(sludge, 1), new ItemStack(Block.dirt), new ItemStack(Item.bucketWater) },
                new ItemStack[] { new ItemStack(Item.clay), new ItemStack(Item.bucketEmpty) });
        //		TileEntityMixer.addRecipe(
        //				new ItemStack[] { new ItemStack(Item.slimeBall), new ItemStack(Item.bucketMilk), new ItemStack(Block.leaves) },
        //				new ItemStack[] { new ItemStack(Item.slimeBall, 2), new ItemStack(Item.bucketEmpty) });
        recipe(crystallizer_item,
                "-",
                "S",
                "U",
                '-', Item.stick,
                'S', Item.silk,
                'W', Block.planks,
                'U', Item.cauldron);
        ItemStack lime = new ItemStack(Item.dyePowder, 1, 10);
        TileEntityCrystallizer.addRecipe(lime, new ItemStack(Item.slimeBall), 1, new ItemStack(Item.bucketMilk), 0);
        // Cutter
        //TODO: Remove the cutter
        //		recipe(cutter_item,
        //				"> P",
        //				" > ",
        //				"> P",
        //				'>', Item.shears,
        //				'P', Block.pistonBase);
        // Queue
        //		recipe(queue_item,
        //				" -P",
        //				" C_",
        //				"  P",
        //				'-', Block.pressurePlatePlanks,
        //				'P', Block.pistonBase,
        //				'_', new ItemStack(Block.stairSingle, 1, 0),
        //				'C', Block.chest);
    }

    void createOreProcessingPath(ItemStack ore, ItemStack ingot, ItemOreProcessing.OreType oreType) {
        //Smelt: 1.0 (implicit) [+]
        //Slag: 1.2 [+]
        //Grind -> Smelt: 1.4 (dirty ore gravel)
        //Grind -> Slag: 1.6 (dirty ore gravel)
        //Grind -> Wash -> Smelt: 1.8 (clean ore chunks)
        //Grind -> Wash -> Slag -> Smelt: 2.0 (reduced ore chunks)
        //Grind -> Wash -> Slag -> Crystallize -> Smelt: 3.0 (crystalline ore)
        oreType.enable();
        ItemStack dirty = ore_dirty_gravel.makeStack(oreType);
        ItemStack clean = ore_clean_gravel.makeStack(oreType);
        ItemStack reduced = ore_reduced.makeStack(oreType);
        ItemStack crystal = ore_crystal.makeStack(oreType);

        //All processing steps can be smelted
        for (ItemStack is : new ItemStack[] { dirty, clean, reduced, crystal }) {
            FurnaceRecipes.smelting().addSmelting(is.itemID, is.getItemDamage(), ingot);
        }

        //ORES
        TileEntitySlagFurnace.SlagRecipes.register(ore, 1.2F, ingot, 0.4F, Block.stone);
        TileEntityGrinder.addRecipe(ore, dirty, 1.4F);
        //DIRTY GRAVEL
        TileEntitySlagFurnace.SlagRecipes.register(dirty, 1.42857142857143F, ingot, 0.2F, Block.dirt);
        TileEntityMixer.addRecipe(
                new ItemStack[] { dirty, new ItemStack(Item.bucketWater) },
                new ItemStack[] { clean, new ItemStack(Item.bucketEmpty), new ItemStack(sludge) });
        //CLEAN CHUNKS
        TileEntitySlagFurnace.SlagRecipes.register(clean, 1, reduced, 0.42857142857143F, reduced);
        //REDUCED CHUNKS
        TileEntityCrystallizer.addRecipe(reduced, crystal, 1.5F, new ItemStack(acid), 0);
    }
    
    void createDualOreProcessingPath(ItemStack ore, ItemStack ingotA, ItemStack ingotB, ItemOreProcessing.OreType oreType_start, ItemOreProcessing.OreType oreType_A, ItemOreProcessing.OreType oreType_B) {
        oreType_start.enable();
        oreType_A.enable();
        oreType_B.enable();
        ItemStack dirty = ore_dirty_gravel.makeStack(oreType_start);
        ItemStack clean = ore_clean_gravel.makeStack(oreType_start);
        
        ItemStack reduced_A = ore_reduced.makeStack(oreType_A);
        ItemStack crystal_A = ore_crystal.makeStack(oreType_A);
        ItemStack reduced_B = ore_reduced.makeStack(oreType_B);
        ItemStack crystal_B = ore_crystal.makeStack(oreType_B);
        
        for (ItemStack is : new ItemStack[] { dirty, clean, reduced_A, crystal_A }) {
            FurnaceRecipes.smelting().addSmelting(is.itemID, is.getItemDamage(), ingotA);
        }
        
        for (ItemStack is : new ItemStack[] { reduced_B, crystal_B }) {
            FurnaceRecipes.smelting().addSmelting(is.itemID, is.getItemDamage(), ingotB);
        }
        
        //NOTE: skip the ORE slag furnace recipe!
        //ORE
        TileEntityGrinder.addRecipe(ore, dirty, 1.4F);
        //DIRTY GRAVEL
        TileEntitySlagFurnace.SlagRecipes.register(dirty, 1.42857142857143F, ingotA, 0.2F, Block.dirt); //XXX TODO: This is lame.
        TileEntityMixer.addRecipe(
                new ItemStack[] { dirty, new ItemStack(Item.bucketWater) },
                new ItemStack[] { clean, new ItemStack(Item.bucketEmpty), new ItemStack(sludge) });
        //CLEAN CHUNKS
        TileEntitySlagFurnace.SlagRecipes.register(clean, 1.1F, reduced_A, 1.6F, reduced_B);
        //REDUCED CHUNKS
        //And now we split.
        TileEntityCrystallizer.addRecipe(reduced_A, crystal_A, 1.5F, new ItemStack(acid), 0);
        TileEntityCrystallizer.addRecipe(reduced_B, crystal_B, 1.5F, new ItemStack(acid), 0);
        
        
    }
    
    private HashMap<String, ItemStack> bestIngots = new HashMap();
    void addDictOres() {
        for (String oreClass : Arrays.asList("oreCopper", "oreTin", "oreSilver")) {
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
            if (oreClass.equals("oreSilver")) {
                ItemStack silver = new ItemStack(silver_ingot);
                ItemStack lead = new ItemStack(lead_ingot);
                for (ItemStack ore : oreList) {
                    createDualOreProcessingPath(ore, silver, lead, ItemOreProcessing.OreType.GALENA, ItemOreProcessing.OreType.SILVER, ItemOreProcessing.OreType.LEAD);
                }
            } else {
                ItemOreProcessing.OreType ot = null;
                if (oreClass.equals("oreCopper")) {
                    ot = ItemOreProcessing.OreType.COPPER;
                }
                if (oreClass.equals("oreTin")) {
                    ot = ItemOreProcessing.OreType.TIN;
                }
                if (ot == null) {
                    continue;
                }
                for (ItemStack ore : oreList) {
                    createOreProcessingPath(ore, bestIngot, ot);
                }
            }
            bestIngots.put(oreClass, bestIngot);
        }
    }

    @ForgeSubscribe
    public void registerOre(OreRegisterEvent evt) {
        String oreClass = evt.Name;
        ItemStack ore = evt.Ore;
        if (oreClass.equals("oreSilver")) {
            TileEntitySlagFurnace.SlagRecipes.register(ore, 0.9F, new ItemStack(silver_ingot), 1.4F, new ItemStack(lead_ingot));
            return;
        }
        if (bestIngots.containsKey(oreClass)) {
            //You're late.
            if (oreClass.equals("oreSilver")) {
                ItemStack silver = new ItemStack(silver_ingot);
                ItemStack lead = new ItemStack(lead_ingot);
                createDualOreProcessingPath(ore, silver, lead, ItemOreProcessing.OreType.GALENA, ItemOreProcessing.OreType.SILVER, ItemOreProcessing.OreType.LEAD);
            } else {
                OreType ot;
                if (oreClass.equals("oreTin")) {
                    ot = OreType.TIN;
                } else if (oreClass.equals("oreCopper")) {
                    ot = OreType.COPPER;
                } else {
                    return;
                }
                createOreProcessingPath(ore, bestIngots.get(oreClass), ot);
            }
        }
    }

    public void setToolEffectiveness() {
        for (String tool : new String[] { "pickaxe", "axe", "shovel" }) {
            MinecraftForge.removeBlockEffectiveness(factory_block, tool);
            MinecraftForge.removeBlockEffectiveness(resource_block, tool);
        }
        BlockClass.DarkIron.harvest("pickaxe", 2);
        BlockClass.Barrel.harvest("axe", 1);
        BlockClass.Machine.harvest("pickaxe", 0);
        BlockClass.Cage.harvest("pickaxe", 1);
        MinecraftForge.setBlockHarvestLevel(resource_block, "pickaxe", 2);
    }

    public void makeOther() {
        silverGen = new WorldGenMinable(resource_block.blockID, 35);
    }
    
    @Override
    public void generate(Random rand, int chunkX, int chunkZ, World world,
            IChunkProvider chunkGenerator, IChunkProvider chunkProvider) {
        if (!Core.gen_silver_ore) {
            return;
        }
        if ((chunkZ + 3*chunkX) % 5 != 0) {
            return;
        }
        int x = chunkX*16 + rand.nextInt(16);
        int z = chunkZ*16 + rand.nextInt(16);
        int y = 5 + rand.nextInt(48);
        silverGen.generate(world, rand, x, y, z);
    }

    public void onTickPlayer(EntityPlayer player) {
        // If in a GUI and holding a demon, BITE!
        // Otherwise, if not in a GUI and holding in hand... BITE!
        if (player.inventory.getItemStack() != null) {
            tiny_demon.playerHolding(player.inventory.getItemStack(), player);
        } else {
            tiny_demon.playerHolding(player.inventory.getCurrentItem(), player);
        }
    }

    public void onTickWorld(World world) {
        if (world.isRemote) {
            return;
        }
        //NOTE: This might bug out if worlds don't tick at the same rate or something! Or if they're in different threads!
        //(Like THAT would ever happen, ah ha ha ha ha ha ha ha ha ha ha ha ha ha.)
        TileEntityWrathLamp.handleAirUpdates();
        TileEntityWrathFire.updateCount = 0;
        TileEntityWatchDemon.worldTick(world);
    }
    
    public boolean extractEnergy(EntityPlayer player, int chargeCount) {
        IInventory inv = player.inventory;
        int totalCharge = 0;
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack is = inv.getStackInSlot(i);
            if (is == null || is.getItem() != battery) {
                continue;
            }
            totalCharge += battery.getStorage(is);
        }
        if (totalCharge < chargeCount) {
            return false;
        }
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack is = inv.getStackInSlot(i);
            if (is == null || is.getItem() != battery) {
                continue;
            }
            int storage = battery.getStorage(is);
            int delta = Math.min(chargeCount, storage);
            storage -= delta;
            chargeCount -= delta;
            if (delta > 0) {
                battery.setStorage(is, storage);
            }
            if (chargeCount <= 0) {
                return true;
            }
        }
        return false;
    }

    @ForgeSubscribe
    public boolean onItemPickup(EntityItemPickupEvent event) {
        EntityPlayer player = event.entityPlayer;
        EntityItem item = event.item;
        if (item == null || item.item == null || item.item.stackSize == 0) {
            return true;
        }
        if (player.isDead) {
            return true;
        }
        ItemStack is = item.item;
        InventoryPlayer inv = player.inventory;
        // If the item would take a new slot in our inventory, look for bags of
        // holding to put it into
        int remaining_size = is.stackSize;
        int free_slots = 0;
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack here = inv.getStackInSlot(i);
            if (here == null) {
                free_slots += 1;
                continue;
            }
            if (is.isItemEqual(here)) {
                int free = here.getMaxStackSize() - here.stackSize;
                remaining_size -= free;
                if (remaining_size <= 0) {
                    break;
                }
            }
        }
        if (remaining_size > 0) {
            // find the BOHs
            ArrayList<ItemStack> bags = new ArrayList<ItemStack>();
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                ItemStack here = inv.getStackInSlot(i);
                if (here != null && here.getItem() == bag_of_holding) {
                    bags.add(here);
                }
            }
            // For each row
            boolean success = false;
            for (ItemStack bag : bags) {
                if (is.stackSize < 0) {
                    break;
                }
                success = bag_of_holding.insertItem(bag, is);
            }
            if (success) {
                Sound.bagSlurp.playAt(player);
            }
        }
        Core.proxy.pokePocketCrafting();
        tiny_demon.bitePlayer(is, player, true);
        return true;
    }

    private final int demon_spawn_delay = 20*60*10;
    private int demon_spawn_tick = 20*60;

    @Override
    public void tickStart(EnumSet<TickType> type, Object... tickData) {
        if (type.contains(TickType.WORLD)) {
            World w = (World) tickData[0];
            if (w.isRemote) {
                return;
            }
            onTickWorld(w);
            if (DimensionManager.getWorld(-1) == w && Core.spawnDemons) {
                demon_spawn_tick--;
                if (demon_spawn_tick == 0) {
                    ItemDemon.spawnDemons(w);
                    demon_spawn_tick = demon_spawn_delay;
                }
            }
        }
        if (type.contains(TickType.PLAYER)) {
            EntityPlayer player = (EntityPlayer) tickData[0];
            onTickPlayer(player);
        }
    }

    @Override
    public void tickEnd(EnumSet<TickType> type, Object... tickData) {
    }

    @Override
    public EnumSet<TickType> ticks() {
        return EnumSet.of(TickType.WORLD, TickType.PLAYER);
    }

    @Override
    public String getLabel() {
        return "factorization-world";
    }

    @Override
    public void onCrafting(EntityPlayer player, ItemStack stack, IInventory craftMatrix) {
        //fixes for shitty MC achievements
        if (player != null) {
            Item item = stack.getItem();
            if (item == Item.hoeStone || item == Item.hoeSteel) {
                player.addStat(AchievementList.buildHoe, 1);
            }
            if (item == Item.swordStone || item == Item.swordSteel) {
                player.addStat(AchievementList.buildSword, 1);
            }
        }
        //our regular programming
        for (int i = 0; i < craftMatrix.getSizeInventory(); i++) {
            ItemStack here = craftMatrix.getStackInSlot(i);
            if (here == null) {
                continue;
            }
            Item item = here.getItem();
            if (item instanceof IActOnCraft) {
                ((IActOnCraft) item).onCraft(here, craftMatrix, i, stack, player);
            }
        }

        if (stack.getItem() == item_craft && stack.getItemDamage() == diamond_shard_packet.getItemDamage()) {
            stack.setTagCompound((NBTTagCompound) diamond_shard_packet.getTagCompound().copy());
            stack.setItemDamage(1);
        }
    }

    @Override
    public void onSmelting(EntityPlayer player, ItemStack item) {
    }

}
