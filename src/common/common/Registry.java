package factorization.common;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

import net.minecraft.src.AchievementList;
import net.minecraft.src.Block;
import net.minecraft.src.CraftingManager;
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
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.World;
import net.minecraft.src.WorldGenMinable;
import net.minecraftforge.common.DungeonHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.ICraftingHandler;
import cpw.mods.fml.common.IWorldGenerator;
import cpw.mods.fml.common.Side;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import factorization.api.IActOnCraft;
import factorization.common.Core.TabType;
import factorization.common.astro.DimensionSliceEntity;

public class Registry implements ICraftingHandler, IWorldGenerator {
    static public final int ExoKeyCount = 3;

    public ItemFactorization item_factorization;
    public ItemBlockResource item_resource;
    public BlockFactorization factory_block, factory_rendering_block = null;
    public BlockLightAir lightair_block;
    public BlockResource resource_block;

    public ItemStack router_item, maker_item, stamper_item, packager_item,
            barrel_item,
            lamp_item, air_item,
            slagfurnace_item, battery_item_hidden, solar_turbine_item, heater_item,
            mirror_item_hidden,
            leadwire_item, grinder_item, mixer_item, crystallizer_item,
            greenware_item;
    public ItemStack silver_ore_item, silver_block_item, lead_block_item,
            dark_iron_block_item, exoworkshop_item;
    public ItemStack is_factory, is_lamp, is_lightair;
    public ItemCraft item_craft;
    public ItemBagOfHolding bag_of_holding;
    public ItemPocketTable pocket_table;
    public ItemWandOfCooling wand_of_cooling;
    public ItemCraftingComponent diamond_shard;
    public IRecipe diamond_shard_recipe;
    public ItemStack diamond_shard_packet;
    public IRecipe boh_upgrade_recipe;
    public ItemWrathIgniter wrath_igniter;
    public ItemCraftingComponent silver_ingot, lead_ingot;
    public ItemCraftingComponent dark_iron;
    public ItemCraftingComponent exo_chasis;
    public ExoArmor exo_head, exo_chest, exo_leg, exo_foot;
    public ExoBuoyantBarrel exo_buoyant_barrel;
    public ExoCobblestoneDrive exo_cobble_drive;
    public ExoMountedPiston exo_mounted_piston;
    public ExoWallJump exo_wall_jump;
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
            Block.blocksList[factory_rendering_block.blockID] = null;
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

        Core.tab(factory_block, Core.TabType.MATERIALS);
    }

    void registerSimpleTileEntities() {
        FactoryType.registerTileEntities();
        //TileEntity renderers are registered in the client proxy

        EntityRegistry.registerGlobalEntityID(TileEntityWrathLamp.RelightTask.class, "factory_relight_task", Core.entity_relight_task_id);
        EntityRegistry.registerModEntity(DimensionSliceEntity.class, "fzwe", 1, Core.instance, 64, 20, true);
    }

    private void addName(Object what, String name) {
        Core.proxy.addName(what, name);
    }

    HashSet<Integer> added_ids = new HashSet();

    public int itemID(String name, int default_id) {
        int id = Integer.parseInt(Core.config.getItem("item", name, default_id).value);
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
        OreDictionary.registerOre("FZ.sludge", sludge);
        //ItemBlocks
        item_factorization = (ItemFactorization) Item.itemsList[factory_block.blockID];
        item_resource = (ItemBlockResource) Item.itemsList[resource_block.blockID];
        Core.instance.tab(resource_block, TabType.MATERIALS);

        //BlockFactorization stuff
        router_item = FactoryType.ROUTER.itemStack("Router");
        barrel_item = FactoryType.BARREL.itemStack("Barrel");
        maker_item = FactoryType.MAKER.itemStack("Craftpacket Maker");
        stamper_item = FactoryType.STAMPER.itemStack("Craftpacket Stamper");
        lamp_item = FactoryType.LAMP.itemStack("Wrathlamp");
        packager_item = FactoryType.PACKAGER.itemStack("Packager");
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
        exoworkshop_item = ResourceType.EXOMODDER.itemStack("Exo-Modder");

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
        OreDictionary.registerOre("FZ.diamondShard", diamond_shard);
        wrath_igniter = new ItemWrathIgniter(itemID("wrathIgniter", 9007));
        addName(wrath_igniter, "Wrath Igniter");
        dark_iron = new ItemCraftingComponent(itemID("darkIron", 9008), "item.darkiron", 3 * 16 + 2);
        addName(dark_iron, "Dark Iron Ingot");
        OreDictionary.registerOre("FZ.darkIron", dark_iron);

        bag_of_holding = new ItemBagOfHolding(itemID("bagOfHolding", 9001));
        addName(bag_of_holding, "Bag of Holding");
        
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
        OreDictionary.registerOre("sulfuricAcid", acid);
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

        //Exo-items
        exo_head.setSlotCount(5);
        exo_chest.setSlotCount(8);
        exo_leg.setSlotCount(6);
        exo_foot.setSlotCount(4);

        exo_chasis = new ItemCraftingComponent(itemID("mechaChasis", 9009), "item.exochasis", 5);
        addName(exo_chasis, "Exo-Chassis");
        //exo_ITEMS created in make_recipes_side()
        //Exo-armor uses up to Item ID 9013.
        addName(exo_head, "Exo-Helmet");
        addName(exo_chest, "Exo-Chestplate");
        addName(exo_leg, "Exo-Leggings");
        addName(exo_foot, "Exo-Boots");
        exo_buoyant_barrel = new ExoBuoyantBarrel(itemID("mechaBouyantBarrel", 9021));
        exo_cobble_drive = new ExoCobblestoneDrive(itemID("mechaCobbleDrive", 9022));
        exo_mounted_piston = new ExoMountedPiston(itemID("mechaMountedPiston", 9023));
        exo_wall_jump = new ExoWallJump(itemID("mechaSpring", 9047));
        addName(exo_buoyant_barrel, "Buoyant Barrel");
        addName(exo_cobble_drive, "Cobblestone Drive");
        addName(exo_mounted_piston, "Shoulder-Mounted Piston");
        addName(exo_wall_jump, "Wall Jumping Boots");
        angular_saw = new ItemAngularSaw(itemID("angularSaw", 9042));
        addName(angular_saw, "Angular Saw");
        MinecraftForge.setToolClass(angular_saw, "pickaxe", 3);
        
        //ceramics
        sculpt_tool = new ItemSculptingTool(itemID("sculptTool", 9041));
        addName(sculpt_tool, "Sculpting Tool");
        
        //inverium = new ItemInverium(itemID("inverium", 9040), "item.inverium", 12*16 + 0, 11);
        inverium = new ItemInverium(itemID("inverium", 9040), "item.inverium", 12*16 + 0, 1);
        addName(inverium, "Inverium Drop");
        OreDictionary.registerOre("FZ.inverium", inverium);

        //Misc
        pocket_table = new ItemPocketTable(itemID("pocketCraftingTable", 9002));
        addName(pocket_table, "Pocket Crafting Table");
    }

    void recipe(ItemStack res, Object... params) {
        GameRegistry.addRecipe(res, params);
    }

    void shapelessRecipe(ItemStack res, Object... params) {
        GameRegistry.addShapelessRecipe(res, params);
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
        shapelessRecipe(BOH, BOH, dark_iron, Item.enderPearl, Item.leather); //ILI!
        boh_upgrade_recipe = FactorizationUtil.createShapelessRecipe(BOH, BOH, dark_iron, Item.enderPearl, Item.leather); // I !
        // Pocket Crafting Table (pocket table)
        recipe(new ItemStack(pocket_table),
                " #",
                "| ",
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
            item_craft.addItem(diamond_shard_packet, i, new ItemStack(Block.obsidian), null);
        }
        for (int i : new int[] { 1, 3, 5, 7 }) {
            item_craft.addItem(diamond_shard_packet, i, new ItemStack(Block.tnt), null);
        }
        item_craft.addItem(diamond_shard_packet, 4, new ItemStack(Block.blockDiamond), null);
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
        FurnaceRecipes.smelting().addSmelting(resource_block.blockID, 0 /* MD for silver */, new ItemStack(silver_ingot), 0.3F);
        
        FurnaceRecipes.smelting().addSmelting(Item.bucketWater.shiftedIndex, new ItemStack(Item.bucketEmpty), 0);

        //exo armor
        recipe(new ItemStack(exo_chasis),
                "III",
                "InI",
                "III",
                'I', Item.ingotIron,
                'n', Item.goldNugget);
        recipe(new ItemStack(exo_head),
                "###",
                "# #",
                '#', exo_chasis);
        recipe(new ItemStack(exo_chest),
                "# #",
                "###",
                "###",
                '#', exo_chasis);
        recipe(new ItemStack(exo_leg),
                "###",
                "# #",
                "# #",
                '#', exo_chasis);
        recipe(new ItemStack(exo_foot),
                "# #",
                "# #",
                '#', exo_chasis);
        //exo armor upgrades

        oreRecipe(new ItemStack(exo_buoyant_barrel),
                "W_W",
                "PBP",
                "WVW",
                'W', "woodPlank",
                '_', Block.pressurePlatePlanks,
                'P', Block.pistonBase,
                'B', barrel_item,
                'V', Item.boat);

        ItemStack is_cobble_drive = new ItemStack(exo_cobble_drive);
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
        recipe(new ItemStack(exo_mounted_piston),
                "CNC",
                "LSL",
                "CCC",
                'C', Block.cobblestone,
                'S', Block.pistonStickyBase,
                'N', Block.pistonBase,
                'L', Block.lever);
        oreRecipe(new ItemStack(angular_saw),
                "O ",
                "MY",
                "! ",
                'O', new ItemStack(diamond_cutting_head),
                'M', new ItemStack(motor),
                'Y', new ItemStack(Item.ingotIron),
                '!', "ingotLead");
        recipe(new ItemStack(exo_wall_jump),
                "ILI",
                "IBI",
                "OOO",
                'I', Item.ingotIron,
                'L', Item.leather,
                'B', Item.bootsLeather,
                'O', Item.slimeBall);
        
        //ceramics
        recipe(new ItemStack(sculpt_tool),
                " c",
                "/ ",
                'c', Item.clay,
                '/', Item.stick);
        ItemSculptingTool.addModeChangeRecipes();
        
        //inverium
        oreRecipe(new ItemStack(inverium, 1, 1),
                "LGL",
                "GDG",
                "LGL",
                'L', "ingotLead",
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
        oreRecipe(new ItemStack(router_eject),
                "IWI",
                "C_C",
                "IPI",
                'I', dark_iron,
                'W', "woodPlank",
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
        oreRecipe(barrel_item,
                "W-W",
                "W W",
                "WWW",
                'W', "logWood",
                '-', "slabWood");

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

        //Slag furnace
        recipe(slagfurnace_item,
                "CFC",
                "C C",
                "CFC",
                'C', Block.cobblestone,
                'F', Block.stoneOvenIdle);
        OreDictionary.registerOre("oreIron", new ItemStack(Block.oreIron));
        OreDictionary.registerOre("oreGold", new ItemStack(Block.oreGold));
        OreDictionary.registerOre("ingotIron", new ItemStack(Item.ingotIron));
        OreDictionary.registerOre("ingotGold", new ItemStack(Item.ingotGold));
        
        //most ores give 0.4F stone, but redstone is dense.
        //mining redstone normally gives 4 to 6 ore. 5.8F should get you a slightly better yield.
        TileEntitySlagFurnace.SlagRecipes.register(Block.oreRedstone, 5.8F, Item.redstone, 0.2F, Block.stone);

        //exo-workshop
        recipe(exoworkshop_item,
                "MCM",
                "i i",
                "i i",
                'C', Block.workbench,
                'M', exo_chasis,
                'i', Item.ingotIron);
        oreRecipe(greenware_item,
                "c",
                "-",
                'c', Item.clay,
                '-', "slabWood");

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
                'L', "ingotLead");
        recipe(new ItemStack(diamond_cutting_head),
                "SSS",
                "SIS",
                "SSS",
                'S', diamond_shard,
                'I', Item.ingotIron);
        oreRecipe(grinder_item,
                "I*I",
                "IMI",
                "LIL",
                'I', Item.ingotIron,
                '*', diamond_cutting_head,
                'M', motor,
                'L', "ingotLead");
        oreRecipe(grinder_item,
                "IMI",
                "I*I",
                "LIL",
                'I', Item.ingotIron,
                '*', diamond_cutting_head,
                'M', motor,
                'L', "ingotLead");
        TileEntityGrinder.addRecipe(new ItemStack(Block.stone), new ItemStack(Block.cobblestone), 1);
        TileEntityGrinder.addRecipe(new ItemStack(Block.cobblestone), new ItemStack(Block.gravel), 1);
        TileEntityGrinder.addRecipe(new ItemStack(Block.gravel), new ItemStack(Block.sand), 1);
        TileEntityGrinder.addRecipe(new ItemStack(Block.grass), new ItemStack(Block.dirt), 1);
        TileEntityGrinder.addRecipe(new ItemStack(Block.mycelium), new ItemStack(Block.dirt), 1);
        TileEntityGrinder.addRecipe(new ItemStack(Block.oreDiamond), new ItemStack(Item.diamond), 2.25F);
        TileEntityGrinder.addRecipe(new ItemStack(Block.oreRedstone), new ItemStack(Item.redstone), 6.5F);
        TileEntityGrinder.addRecipe(new ItemStack(Block.oreLapis), new ItemStack(Item.dyePowder, 1, 4), 8);
        TileEntityGrinder.addRecipe(new ItemStack(Block.oreCoal), new ItemStack(Item.coal), 3.5F);
        oreRecipe(mixer_item,
                " X ",
                "WMW",
                "LUL",
                'X', fan,
                'W', Item.bucketWater,
                'M', motor,
                'L', "ingotLead",
                'U', Item.cauldron);
        FurnaceRecipes.smelting().addSmelting(sludge.shiftedIndex, 0, new ItemStack(Item.clay), 0.1F);
//		TileEntityMixer.addRecipe(
//				new ItemStack[] { new ItemStack(sludge, 1), new ItemStack(Block.dirt), new ItemStack(Item.bucketWater) },
//				new ItemStack[] { new ItemStack(Item.clay), new ItemStack(Item.bucketEmpty) });
        //		TileEntityMixer.addRecipe(
        //				new ItemStack[] { new ItemStack(Item.slimeBall), new ItemStack(Item.bucketMilk), new ItemStack(Block.leaves) },
        //				new ItemStack[] { new ItemStack(Item.slimeBall, 2), new ItemStack(Item.bucketEmpty) });
        recipe(crystallizer_item,
                "-",
                "S",
                "U",
                '-', Item.stick,
                'S', Item.silk,
                'U', Item.cauldron);
        ItemStack lime = new ItemStack(Item.dyePowder, 1, 10);
        TileEntityCrystallizer.addRecipe(lime, new ItemStack(Item.slimeBall), 1, new ItemStack(Item.bucketMilk), 0);
    }

    public void setToolEffectiveness() {
        for (String tool : new String[] { "pickaxe", "axe", "shovel" }) {
            MinecraftForge.removeBlockEffectiveness(factory_block, tool);
            MinecraftForge.removeBlockEffectiveness(resource_block, tool);
        }
        BlockClass.DarkIron.harvest("pickaxe", 2);
        BlockClass.Barrel.harvest("axe", 1);
        BlockClass.Machine.harvest("pickaxe", 0);
        BlockClass.MachineLightable.harvest("pickaxe", 0);
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

    public void onTickWorld(World world) {
        if (world.isRemote) {
            return;
        }
        //NOTE: This might bug out if worlds don't tick at the same rate or something! Or if they're in different threads!
        //(Like THAT would ever happen, ah ha ha ha ha ha ha ha ha ha ha ha ha ha.)
        TileEntityWrathLamp.handleAirUpdates();
        TileEntityWrathFire.updateCount = 0;
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
        return true;
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
