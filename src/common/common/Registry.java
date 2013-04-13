package factorization.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.feature.WorldGenMinable;
import net.minecraftforge.common.ChestGenHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.liquids.LiquidContainerData;
import net.minecraftforge.liquids.LiquidContainerRegistry;
import net.minecraftforge.liquids.LiquidDictionary;
import net.minecraftforge.liquids.LiquidStack;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.ICraftingHandler;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.IWorldGenerator;
import cpw.mods.fml.common.TickType;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.LanguageRegistry;
import cpw.mods.fml.relauncher.Side;
import factorization.api.IActOnCraft;
import factorization.common.Core.TabType;
import factorization.common.TileEntityGreenware.ClayState;

public class Registry implements ICraftingHandler, IWorldGenerator, ITickHandler {
    static public final int ExoKeyCount = 3;

    public ItemFactorization item_factorization;
    public ItemBlockResource item_resource;
    public BlockFactorization factory_block, factory_rendering_block = null;
    public BlockRenderHelper blockRender = null, serverTraceHelper = null, clientTraceHelper = null;
    public BlockLightAir lightair_block;
    public BlockResource resource_block;

    public ItemStack router_item, maker_item, stamper_item, packager_item,
            barrel_item,
            lamp_item, air_item,
            slagfurnace_item, battery_item_hidden, heater_item, steamturbine_item, solarboiler_item,
            mirror_item_hidden,
            leadwire_item, grinder_item, mixer_item, crystallizer_item,
            greenware_item,
            rocket_engine_item_hidden;
    @Deprecated
    public ItemStack solar_turbine_item;
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
    public ItemAcidBottle acid;
    public ItemCraftingComponent magnet, insulated_coil, motor, fan, diamond_cutting_head;
    public ItemStack sulfuric_acid, aqua_regia;
    public ItemChargeMeter charge_meter;
    public ItemBlockProxy mirror;
    public ItemBattery battery;
    public ItemOreProcessing ore_dirty_gravel, ore_clean_gravel, ore_reduced, ore_crystal;
    public ItemCraftingComponent sludge;
    public ItemCraftingComponent inverium;
    public ItemSculptingTool sculpt_tool;
    public ItemGlazeBucket glaze_bucket;
    public ItemAngularSaw angular_saw;
    public ItemCraftingComponent heatHole, logicMatrix, logicMatrixIdentifier, logicMatrixProgrammer;
    public Item fz_steam;
    public ItemCraftingComponent nether_powder, rocket_fuel;
    public Item rocket_fuel_liquid_entry;
    public ItemBlockProxy rocket_engine;
    public LiquidStack liquidStackRocketFuel;
    public ItemCraftingComponent bucket_rocket_fuel;

    public Material materialMachine = new Material(MapColor.ironColor);

    WorldGenMinable silverGen;
    
    final int WILDCARD = Short.MAX_VALUE;

    void makeBlocks() {
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            //Theoretically, not necessary. I bet BUKKIT would flip its shit tho.
            blockRender = new BlockRenderHelper();
            factory_rendering_block = new BlockFactorization(Core.factory_block_id);
            Block.blocksList[factory_rendering_block.blockID] = null;
        }
        serverTraceHelper = new BlockRenderHelper();
        clientTraceHelper = new BlockRenderHelper();
        factory_block = new BlockFactorization(Core.factory_block_id);
        lightair_block = new BlockLightAir(Core.lightair_id);
        resource_block = new BlockResource(Core.resource_id);
        is_factory = new ItemStack(factory_block);
        is_lightair = new ItemStack(lightair_block);

        GameRegistry.registerBlock(factory_block, ItemFactorization.class, "FZ factory");
        GameRegistry.registerBlock(lightair_block, "FZ Lightair");
        GameRegistry.registerBlock(resource_block, ItemBlockResource.class, "FZ resource");
        GameRegistry.registerCraftingHandler(this);
        GameRegistry.registerWorldGenerator(this);

        Core.tab(factory_block, Core.TabType.MATERIALS);
    }

    void registerSimpleTileEntities() {
        FactoryType.registerTileEntities();
        GameRegistry.registerTileEntity(TileEntityFzNull.class, "fz.null");
        //TileEntity renderers are registered in the client proxy

        EntityRegistry.registerGlobalEntityID(TileEntityWrathLamp.RelightTask.class, "factory_relight_task", Core.entity_relight_task_id);
    }

    /*private void addName(Object what, String name) {
        Core.proxy.addName(what, name);
    }*/

    HashSet<Integer> added_ids = new HashSet<Integer>();

    public int itemID(String name, int default_id) {
        int id = Core.config.getItem("item", name, default_id).getInt();
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
        ore_dirty_gravel = new ItemOreProcessing(itemID("oreDirtyGravel", 9034), 2 * 16 + 4, "gravel");
        ore_clean_gravel = new ItemOreProcessing(itemID("oreCleanGravel", 9035), 2 * 16 + 5, "clean");
        ore_reduced = new ItemOreProcessing(itemID("oreReduced", 9036), 2 * 16 + 6, "reduced");
        ore_crystal = new ItemOreProcessing(itemID("oreCrystal", 9037), 2 * 16 + 7, "crystal");
        sludge = new ItemCraftingComponent(itemID("sludge", 9039), "sludge");
        OreDictionary.registerOre("FZ.sludge", sludge);
        //ItemBlocks
        item_factorization = (ItemFactorization) Item.itemsList[factory_block.blockID];
        item_resource = (ItemBlockResource) Item.itemsList[resource_block.blockID];
        Core.tab(resource_block, TabType.MATERIALS);

        //BlockFactorization stuff
        router_item = FactoryType.ROUTER.itemStack();
        barrel_item = FactoryType.BARREL.itemStack();
        maker_item = FactoryType.MAKER.itemStack();
        stamper_item = FactoryType.STAMPER.itemStack();
        lamp_item = FactoryType.LAMP.itemStack();
        packager_item = FactoryType.PACKAGER.itemStack();
        slagfurnace_item = FactoryType.SLAGFURNACE.itemStack();
        battery_item_hidden = FactoryType.BATTERY.itemStack();
        solar_turbine_item = FactoryType.SOLARTURBINE.itemStack();
        steamturbine_item = FactoryType.STEAMTURBINE.itemStack();
        solarboiler_item = FactoryType.SOLARBOILER.itemStack();
        heater_item = FactoryType.HEATER.itemStack();
        mirror_item_hidden = FactoryType.MIRROR.itemStack();
        leadwire_item = FactoryType.LEADWIRE.itemStack();
        grinder_item = FactoryType.GRINDER.itemStack();
        mixer_item = FactoryType.MIXER.itemStack();
        crystallizer_item = FactoryType.CRYSTALLIZER.itemStack();
        greenware_item = FactoryType.CERAMIC.itemStack();
        rocket_engine_item_hidden = FactoryType.ROCKETENGINE.itemStack();

        //BlockResource stuff
        silver_ore_item = ResourceType.SILVERORE.itemStack("Silver Ore");
        silver_block_item = ResourceType.SILVERBLOCK.itemStack("Block of Silver");
        lead_block_item = ResourceType.LEADBLOCK.itemStack("Block of Lead");
        dark_iron_block_item = ResourceType.DARKIRONBLOCK.itemStack("Block of Dark Iron");
        exoworkshop_item = ResourceType.EXOMODDER.itemStack("Exo-Modder");

        lead_ingot = new ItemCraftingComponent(itemID("leadIngot", 9014), "lead_ingot");
        silver_ingot = new ItemCraftingComponent(itemID("silverIngot", 9015), "silver_ingot");
        OreDictionary.registerOre("oreSilver", silver_ore_item);
        OreDictionary.registerOre("ingotSilver", new ItemStack(silver_ingot));
        OreDictionary.registerOre("ingotLead", new ItemStack(lead_ingot));
        OreDictionary.registerOre("blockSilver", silver_block_item);
        OreDictionary.registerOre("blockLead", lead_block_item);

        //Darkness & Evil
        diamond_shard = new ItemCraftingComponent(itemID("diamondShard", 9006), "diamond_shard");
        OreDictionary.registerOre("FZ.diamondShard", diamond_shard);
        wrath_igniter = new ItemWrathIgniter(itemID("wrathIgniter", 9007));
        dark_iron = new ItemCraftingComponent(itemID("darkIron", 9008), "dark_iron_ingot");
        OreDictionary.registerOre("FZ.darkIron", dark_iron);

        bag_of_holding = new ItemBagOfHolding(itemID("bagOfHolding", 9001));
        
        logicMatrixProgrammer = new ItemMatrixProgrammer(itemID("logicMatrixProgrammer", 9043), "tool.matrix_programmer");
        ChestGenHooks dungeon = ChestGenHooks.getInfo(ChestGenHooks.DUNGEON_CHEST);
        dungeon.addItem(new WeightedRandomChestContent(new ItemStack(logicMatrixProgrammer), 1, 1, 25)); //XXX TODO: Temporary, put these on asteroids.
        logicMatrix = new ItemCraftingComponent(itemID("logicMatrix", 9044), "logic_matrix");
        logicMatrixIdentifier = new ItemCraftingComponent(itemID("logicMatrixID", 9045), "logic_matrix_identifier");
        heatHole = new ItemCraftingComponent(itemID("heatHole", 9046), "heat_hole");

        wand_of_cooling = new ItemWandOfCooling(itemID("wandOfCooling", 9005));

        router_item_filter = new ItemMachineUpgrade(itemID("routerItemFilter", 9016), "router/item_filter", "Router Upgrade", FactoryType.ROUTER, 0);
        router_machine_filter = new ItemMachineUpgrade(itemID("routerMachineFilter", 9017), "router/machine_filter", "Router Upgrade", FactoryType.ROUTER, 1);
        router_speed = new ItemMachineUpgrade(itemID("routerSpeed", 9018), "router/speed", "Router Upgrade", FactoryType.ROUTER, 2);
        router_thorough = new ItemMachineUpgrade(itemID("routerThorough", 9019), "router/thorough", "Router Upgrade", FactoryType.ROUTER, 3);
        router_throughput = new ItemMachineUpgrade(itemID("routerThroughput", 9020), "router/bandwidth", "Router Upgrade", FactoryType.ROUTER, 4);
        router_eject = new ItemMachineUpgrade(itemID("routerEject", 9031), "router/eject", "Router Upgrade", FactoryType.ROUTER, 5);

        barrel_enlarge = new ItemMachineUpgrade(itemID("barrelEnlarge", 9032), "barrel_upgrade", "Barrel Upgrade", FactoryType.BARREL, 6);

        //Electricity
        acid = new ItemAcidBottle(itemID("acid", 9024));
        sulfuric_acid = new ItemStack(acid, 1);
        aqua_regia = new ItemStack(acid, 1, 1);
        OreDictionary.registerOre("sulfuricAcid", sulfuric_acid);
        OreDictionary.registerOre("aquaRegia", aqua_regia);
        magnet = new ItemCraftingComponent(itemID("magnet", 9025), "magnet");
        insulated_coil = new ItemCraftingComponent(itemID("coil", 9026), "insulated_coil");
        motor = new ItemCraftingComponent(itemID("motor", 9027), "motor");
        fan = new ItemCraftingComponent(itemID("fan", 9028), "fan");
        diamond_cutting_head = new ItemCraftingComponent(itemID("diamondCuttingHead", 9038), "diamond_cutting_head");
        charge_meter = new ItemChargeMeter(itemID("chargemeter", 9029));
        mirror = new ItemBlockProxy(itemID("mirror", 9030), mirror_item_hidden);
        mirror.setUnlocalizedName("factorization:mirror");
        battery = new ItemBattery(itemID("battery", 9033));

        //Industrial
        item_craft = new ItemCraft(itemID("itemCraftId", 9000));

        //Exo-items
        exo_head = new ExoArmor(itemID("mechaHead", 9010), 0).setSlotCount(5);
        exo_chest = new ExoArmor(itemID("mechaChest", 9011), 1).setSlotCount(8);
        exo_leg = new ExoArmor(itemID("mechaLeg", 9012), 2).setSlotCount(7);
        exo_foot = new ExoArmor(itemID("mechaFoot", 9013), 3).setSlotCount(4);

        exo_chasis = new ItemCraftingComponent(itemID("mechaChasis", 9009), "exo_chasis");
        //Exo-armor uses up to Item ID 9013.
        exo_buoyant_barrel = new ExoBuoyantBarrel(itemID("mechaBouyantBarrel", 9021));
        exo_cobble_drive = new ExoCobblestoneDrive(itemID("mechaCobbleDrive", 9022));
        exo_mounted_piston = new ExoMountedPiston(itemID("mechaMountedPiston", 9023));
        exo_wall_jump = new ExoWallJump(itemID("mechaSpring", 9047));
        angular_saw = new ItemAngularSaw(itemID("angularSaw", 9042));
        MinecraftForge.setToolClass(angular_saw, "pickaxe", 3);
        
        //ceramics
        sculpt_tool = new ItemSculptingTool(itemID("sculptTool", 9041));
        glaze_bucket = new ItemGlazeBucket(itemID("glazeBucket", 9055));
        
        //inverium = new ItemInverium(itemID("inverium", 9040), "item.inverium", 12*16 + 0, 11);
        inverium = new ItemInverium(itemID("inverium", 9040), "rocket/inverium_drop");
        OreDictionary.registerOre("FZ.inverium", inverium);

        //Misc
        pocket_table = new ItemPocketTable(itemID("pocketCraftingTable", 9002));
        fz_steam = new Item(itemID("steam", 9049));
        fz_steam.setUnlocalizedName("factorization:charge/steam");
        
        //Rocketry
        nether_powder = new ItemCraftingComponent(itemID("netherPowder", 9050), "nether_powder");
        if (Core.enable_dimension_slice) {
            rocket_fuel = new ItemCraftingComponent(itemID("heldRocketFuel", 9051), "rocket/powder_rocket_fuel");
            rocket_fuel_liquid_entry = new Item(itemID("liquidRocketFuel", 9052));
            rocket_fuel_liquid_entry.setUnlocalizedName("factorization:rocket/powder_rocket_fuel");
            rocket_engine = new ItemBlockProxy(itemID("rocketEngine", 9053), rocket_engine_item_hidden);
            rocket_engine.setUnlocalizedName("factorization:rocket/rocket_engine").setMaxStackSize(1);
            bucket_rocket_fuel = new ItemCraftingComponent(itemID("bucketRocketFuel", 9054), "rocket/rocket_fuel_bucket");
            bucket_rocket_fuel.setMaxStackSize(1);
            bucket_rocket_fuel.setContainerItem(Item.bucketEmpty);
        }
    }

    void recipe(ItemStack res, Object... params) {
        GameRegistry.addRecipe(res, params);
    }

    void shapelessRecipe(ItemStack res, Object... params) {
        if (res == null) {
            return;
        }
        GameRegistry.addShapelessRecipe(res, params);
    }

    void oreRecipe(ItemStack res, Object... params) {
        if (res == null) {
            return;
        }
        GameRegistry.addRecipe(new ShapedOreRecipe(res, params));
    }

    void shapelessOreRecipe(ItemStack res, Object... params) {
        if (res == null) {
            return;
        }
        GameRegistry.addRecipe(new ShapelessOreRecipe(res, params));
    }

    void makeRecipes() {
        recipe(new ItemStack(Block.stoneDoubleSlab),
                "-",
                "-",
                '-', new ItemStack(Block.stoneSingleSlab, 1));
        recipe(new ItemStack(Block.stoneDoubleSlab, 4, 8),
                "##",
                "##",
                '#', new ItemStack(Block.stoneDoubleSlab));
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
                'i', Item.field_94583_ca /* netherquartz */,
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
                'B', Item.field_94584_bZ /* netherbrick */);
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
                'W', "plankWood",
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
        oreRecipe(new ItemStack(glaze_bucket),
                "_ _",
                "# #",
                "#_#",
                '_', "slabWood",
                '#', "plankWood");
        
        ItemStack base_common = glaze_bucket.makeCraftingGlaze("base_common");
        ItemStack base_matte = glaze_bucket.makeCraftingGlaze("base_matte");
        ItemStack base_translucent = glaze_bucket.makeCraftingGlaze("base_translucent");
        ItemStack base_shiny = glaze_bucket.makeCraftingGlaze("base_shiny");
        ItemStack base_bright = glaze_bucket.makeCraftingGlaze("base_bright");
        ItemStack base_unreal = glaze_bucket.makeCraftingGlaze("base_unreal");
        final ItemStack base_mimicry = glaze_bucket.makeCraftingGlaze("base_mimicry");
        
        glaze_bucket.add(base_mimicry);
        
        ItemStack charcoal = new ItemStack(Item.coal, 1, 1);
        ItemStack bonemeal = new ItemStack(Item.dyePowder, 1, 15);
        ItemStack lapis = new ItemStack(Item.dyePowder, 1, 4);
        ItemStack lead_chunks = new ItemStack(ore_reduced, 1, ItemOreProcessing.OreType.LEAD.ID);
        ItemStack iron_chunks = new ItemStack(ore_reduced, 1, ItemOreProcessing.OreType.IRON.ID);
        Item netherquartz = Item.field_94583_ca; //= field_94583_ca = netherquartz
        Item netherbrick = Item.field_94584_bZ;
        
        shapelessOreRecipe(base_common, new ItemStack(glaze_bucket), Item.bucketWater, Block.sand, Item.clay);
        shapelessOreRecipe(base_matte, base_common, Item.clay, Block.sand, charcoal);
        shapelessOreRecipe(base_translucent, base_common, Block.sand, Block.sand, Block.sand);
        shapelessOreRecipe(base_shiny, base_common, netherquartz, Block.sand, charcoal);
        shapelessOreRecipe(base_bright, base_shiny, bonemeal, nether_powder, lead_chunks);
        shapelessOreRecipe(base_unreal, base_bright, diamond_shard, Item.eyeOfEnder, dark_iron);
        shapelessOreRecipe(base_mimicry, base_unreal, Item.redstone, Item.slimeBall, lapis);
        
        ItemStack blackWool = new ItemStack(Block.cloth, 1, 15);
        BasicGlazes.ST_VECHS_BLACK.recipe(base_matte, blackWool);
        BasicGlazes.TEMPLE_WHITE.recipe(base_common, bonemeal);
        BasicGlazes.SALLYS_WHITE.recipe(base_shiny, netherquartz);
        BasicGlazes.CLEAR.recipe(base_translucent, Block.sand);
        BasicGlazes.REDSTONE_OXIDE.recipe(base_common, Item.redstone);
        BasicGlazes.LAPIS_OXIDE.recipe(base_common, lapis);
        BasicGlazes.PURPLE_OXIDE.recipe(base_common, Item.redstone, lapis);
        BasicGlazes.LEAD_OXIDE.recipe(base_common, lead_chunks);
        BasicGlazes.FIRE_ENGINE_RED.recipe(base_bright, Item.redstone);
        BasicGlazes.CELEDON.recipe(base_translucent, Item.slimeBall);
        BasicGlazes.IRON_BLUE.recipe(base_shiny, lapis, iron_chunks);
        BasicGlazes.STONEWARE_SLIP.recipe(base_common, sludge);
        BasicGlazes.TENMOKU.recipe(base_common, netherbrick);
        BasicGlazes.PEKING_BLUE.recipe(base_bright, lapis);
        BasicGlazes.SHINO.recipe(base_matte, Item.redstone, netherquartz);
        Core.registry.glaze_bucket.doneMakingStandardGlazes();
        
        //Sculpture combiniation recipe
        GameRegistry.addRecipe(new IRecipe() {
            ArrayList<ItemStack> merge(InventoryCrafting inv) {
                if (inv.stackList.length < 2) {
                    return null;
                }
                ArrayList<ItemStack> match = new ArrayList<ItemStack>(2);
                int part_count = 0;
                for (int i = 0; i < inv.getSizeInventory(); i++) {
                    ItemStack is = inv.getStackInSlot(i);
                    if (is == null) {
                        continue;
                    }
                    if (!is.hasTagCompound()) {
                        return null;
                    }
                    Item item = is.getItem();
                    if (FactorizationUtil.similar(Core.registry.greenware_item, is)) {
                        match.add(is);
                    } else {
                        return null;
                    }
                }
                if (match.size() < 2) {
                    return null;
                }
                return match;
            }
            
            @Override
            public boolean matches(InventoryCrafting inventorycrafting, World world) {
                ArrayList<ItemStack> matching = merge(inventorycrafting);
                if (matching == null) {
                    return false;
                }
                int partCount = 0;
                TileEntityGreenware rep = (TileEntityGreenware) FactoryType.CERAMIC.getRepresentative();
                for (ItemStack is : matching) {
                    rep.loadParts(is.getTagCompound());
                    if (rep.getState() != ClayState.WET) {
                        return false;
                    }
                    partCount += rep.parts.size();
                    if (partCount >= TileEntityGreenware.MAX_PARTS) {
                        return false;
                    }
                }
                return true;
            }
            
            @Override
            public int getRecipeSize() {
                return 2;
            }
            
            @Override
            public ItemStack getRecipeOutput() {
                return greenware_item.copy();
            }
            
            @Override
            public ItemStack getCraftingResult(InventoryCrafting inventorycrafting) {
                ArrayList<ItemStack> matching = merge(inventorycrafting);
                TileEntityGreenware target = new TileEntityGreenware();
                for (ItemStack is : matching) {
                    TileEntityGreenware rep = (TileEntityGreenware) FactoryType.CERAMIC.getRepresentative();
                    rep.loadParts(is.getTagCompound());
                    target.parts.addAll(rep.parts);
                }
                return target.getItem();
            }
        });
        
        //Mimicry glaze recipe
        GameRegistry.addRecipe(new IRecipe() {			
            @Override
            public boolean matches(InventoryCrafting inventorycrafting, World world) {
                int mimic_items = 0;
                int other_items = 0;
                for (int i = 0; i < inventorycrafting.getSizeInventory(); i++) {
                    ItemStack is = inventorycrafting.getStackInSlot(i);
                    if (is == null) {
                        continue;
                    }
                    if (FactorizationUtil.couldMerge(base_mimicry, is)) {
                        mimic_items++;
                    } else {
                        if (is.itemID >= Block.blocksList.length) {
                            return false;
                        }
                        int d = is.getItemDamage();
                        if (d < 0 || d > 16) {
                            return false;
                        }
                        Block b = Block.blocksList[is.itemID];
                        if (b == null) {
                            return false;
                        }
                        other_items++;
                    }
                }
                return mimic_items == 1 && other_items == 1;
            }
            
            @Override
            public int getRecipeSize() {
                return 2;
            }
            
            @Override
            public ItemStack getRecipeOutput() {
                return base_mimicry;
            }
            
            @Override
            public ItemStack getCraftingResult(InventoryCrafting inventorycrafting) {
                int[] side_map = {
                        1, 2, 1,
                        4, 0, 5,
                        0, 3, 0
                };
                for (int i = 0; i < inventorycrafting.getSizeInventory(); i++) {
                    ItemStack is = inventorycrafting.getStackInSlot(i);
                    if (is == null) {
                        continue;
                    }
                    if (FactorizationUtil.couldMerge(base_mimicry, is)) {
                        continue;
                    }
                    if (is.itemID >= Block.blocksList.length) {
                        continue;
                    }
                    int d = is.getItemDamage();
                    if (d < 0 || d > 16) {
                        continue;
                    }
                    Block b = Block.blocksList[is.itemID];
                    if (b == null) {
                        continue;
                    }
                    int side = 0;
                    try {
                        side = side_map[i];
                    } catch (ArrayIndexOutOfBoundsException e) {}
                    return glaze_bucket.makeMimicingGlaze(is.itemID, is.getItemDamage(), side);
                }
                return null;
            }
        });
        
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
                'W', "plankWood",
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
                'W', new ItemStack(wrath_igniter, 1, WILDCARD));

        //Slag furnace
        recipe(slagfurnace_item,
                "CFC",
                "C C",
                "CFC",
                'C', Block.cobblestone,
                'F', Block.furnaceIdle);
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

        
        shapelessRecipe(sulfuric_acid, Item.gunpowder, Item.gunpowder, Item.coal, Item.potion);
        shapelessOreRecipe(sulfuric_acid, "dustSulfur", Item.coal, Item.potion);
        shapelessRecipe(aqua_regia, sulfuric_acid, nether_powder, Item.fireballCharge);
        recipe(new ItemStack(fan),
                "I I",
                " I ",
                "I I",
                'I', Item.ingotIron);
//		recipe(solar_turbine_item,
//				"###",
//				"#F#",
//				"#M#",
//				'#', Block.thinGlass,
//				'F', fan,
//				'M', motor);
        shapelessRecipe(new ItemStack(motor), solar_turbine_item);
        recipe(solarboiler_item,
                "I#I",
                "I I",
                "III",
                'I', Item.ingotIron,
                '#', Block.fenceIron
                );
        oreRecipe(steamturbine_item,
                "I#I",
                "GXG",
                "LML",
                'I', Item.ingotIron,
                '#', Block.fenceIron,
                'G', Block.thinGlass,
                'X', fan,
                'L', "ingotLead",
                'M', motor );
        oreRecipe(new ItemStack(charge_meter),
                "WSW",
                "W/W",
                "LIL",
                'W', "plankWood",
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
        oreRecipe(new ItemStack(insulated_coil, 4),
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
                "LIL",
                "I*I",
                "IMI",
                'L', "ingotLead",
                'I', Item.ingotIron,
                '*', diamond_cutting_head,
                'M', motor);
        TileEntityGrinder.addRecipe(new ItemStack(Block.stone), new ItemStack(Block.cobblestone), 1);
        TileEntityGrinder.addRecipe(new ItemStack(Block.cobblestone), new ItemStack(Block.gravel), 1);
        TileEntityGrinder.addRecipe(new ItemStack(Block.gravel), new ItemStack(Block.sand), 1);
        TileEntityGrinder.addRecipe(new ItemStack(Block.grass), new ItemStack(Block.dirt), 1);
        TileEntityGrinder.addRecipe(new ItemStack(Block.mycelium), new ItemStack(Block.dirt), 1);
        TileEntityGrinder.addRecipe(new ItemStack(Block.oreDiamond), new ItemStack(Item.diamond), 2.25F);
        TileEntityGrinder.addRecipe(new ItemStack(Block.oreEmerald), new ItemStack(Item.emerald), 2.25F);
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
        FurnaceRecipes.smelting().addSmelting(sludge.itemID, 0, new ItemStack(Item.clay), 0.1F);
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
        
        //Rocketry
        TileEntityGrinder.addRecipe(new ItemStack(Block.netherrack), new ItemStack(nether_powder, 1), 1);
        if (Core.enable_dimension_slice) {
            shapelessRecipe(new ItemStack(rocket_fuel, 3), nether_powder, nether_powder, nether_powder, Item.fireballCharge);
            liquidStackRocketFuel = new LiquidStack(rocket_fuel_liquid_entry, 0);
            LiquidDictionary.getOrCreateLiquid("powderRocketFuel", liquidStackRocketFuel);
            recipe(new ItemStack(rocket_engine),
                    "#F#",
                    "#I#",
                    "I I",
                    '#', Block.blockSteel,
                    'F', rocket_fuel,
                    'I', Item.ingotIron);
            shapelessRecipe(new ItemStack(bucket_rocket_fuel), Item.bucketEmpty, rocket_fuel, rocket_fuel);
            ItemStack air = new ItemStack(Item.bucketEmpty, 0);
            ItemStack emptyBucket = new ItemStack(Item.bucketEmpty, 1);
            LiquidContainerRegistry.registerLiquid(new LiquidContainerData(new LiquidStack(rocket_fuel_liquid_entry, LiquidContainerRegistry.BUCKET_VOLUME/2), new ItemStack(rocket_fuel, 1), air)); //TODO: Would be nice if this worked. Forge would need something for it tho.
            LiquidContainerRegistry.registerLiquid(new LiquidContainerData(new LiquidStack(rocket_fuel_liquid_entry, LiquidContainerRegistry.BUCKET_VOLUME), new ItemStack(bucket_rocket_fuel, 1), emptyBucket));
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
        BlockClass.MachineLightable.harvest("pickaxe", 0);
        MinecraftForge.setBlockHarvestLevel(resource_block, "pickaxe", 2);
    }

    public void makeOther() { 
        silverGen = new WorldGenMinable(resource_block.blockID, Core.silver_ore_node_size);
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

    public void onTickServer() {
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
        ItemStack is = item.getEntityItem();
        if (item == null || is == null || is.stackSize == 0) {
            return true;
        }
        if (player.isDead) {
            return true;
        }
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
            if (FactorizationUtil.couldMerge(is, here)) {
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

    @Override
    public void tickStart(EnumSet<TickType> type, Object... tickData) {
        this.onTickServer();
    }

    @Override
    public void tickEnd(EnumSet<TickType> type, Object... tickData) {
        
    }

    private EnumSet<TickType> serverTicks = EnumSet.of(TickType.SERVER);
    @Override
    public EnumSet<TickType> ticks() {
        return serverTicks;
    }

    @Override
    public String getLabel() {
        return "FZ_registry";
    }
    
    public void loadLanguages() {
        URL url = this.getClass().getResource(Core.language_file);
        if (url == null) {
            Core.logSevere("Language file %s was not found", url);
            return;
        }
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#") || !line.contains("=")) {
                    continue;
                }
                String parts[] = line.split("=");
                String key = parts[0], value = parts[1];
                LanguageRegistry.instance().addStringLocalization(key, value);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
