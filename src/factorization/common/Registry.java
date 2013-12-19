package factorization.common;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Tuple;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraft.village.MerchantRecipeList;
import net.minecraft.world.World;
import net.minecraftforge.common.ChestGenHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.ICraftingHandler;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;
import cpw.mods.fml.common.event.FMLInterModComms;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.VillagerRegistry;
import cpw.mods.fml.common.registry.VillagerRegistry.IVillageTradeHandler;
import cpw.mods.fml.relauncher.Side;
import factorization.api.IActOnCraft;
import factorization.ceramics.BasicGlazes;
import factorization.ceramics.ItemGlazeBucket;
import factorization.ceramics.ItemSculptingTool;
import factorization.ceramics.TileEntityGreenware;
import factorization.ceramics.TileEntityGreenware.ClayState;
import factorization.charge.ItemAcidBottle;
import factorization.charge.ItemBattery;
import factorization.charge.ItemChargeMeter;
import factorization.charge.TileEntityLeydenJar;
import factorization.oreprocessing.BlockOreStorageShatterable;
import factorization.oreprocessing.ItemOreProcessing;
import factorization.oreprocessing.ItemOreProcessing.OreType;
import factorization.oreprocessing.TileEntityCrystallizer;
import factorization.oreprocessing.TileEntityGrinder;
import factorization.oreprocessing.TileEntitySlagFurnace;
import factorization.servo.ItemMatrixProgrammer;
import factorization.servo.ItemServoMotor;
import factorization.servo.ItemServoRailWidget;
import factorization.servo.ServoComponent;
import factorization.shared.BlockClass;
import factorization.shared.BlockFactorization;
import factorization.shared.Core;
import factorization.shared.Core.TabType;
import factorization.shared.FactoryType;
import factorization.shared.FzConfig;
import factorization.shared.FzUtil;
import factorization.shared.ItemBlockProxy;
import factorization.shared.ItemCraftingComponent;
import factorization.shared.ItemFactorizationBlock;
import factorization.shared.Sound;
import factorization.weird.ItemDayBarrel;
import factorization.weird.ItemPocketTable;
import factorization.weird.TileEntityDayBarrel;
import factorization.wrath.BlockLightAir;
import factorization.wrath.ItemBagOfHolding;
import factorization.wrath.ItemMachineUpgrade;
import factorization.wrath.ItemWrathIgniter;
import factorization.wrath.TileEntityWrathFire;
import factorization.wrath.TileEntityWrathLamp;

public class Registry implements ICraftingHandler, ITickHandler {
    public ItemFactorizationBlock item_factorization;
    public ItemBlockResource item_resource;
    public BlockFactorization factory_block, factory_rendering_block = null;
    public BlockRenderHelper blockRender = null, serverTraceHelper = null, clientTraceHelper = null;
    public BlockLightAir lightair_block;
    public BlockResource resource_block;
    public Block dark_iron_ore;
    public Block fractured_bedrock_block;

    public ItemStack router_item, servorail_item;
    public ItemStack empty_socket_item, socket_lacerator, socket_robot_hand, socket_shifter;
    
    public ItemStack stamper_item, packager_item,
            barrel_item, daybarrel_item_hidden,
            lamp_item, air_item,
            slagfurnace_item, battery_item_hidden, leydenjar_item, leydenjar_item_full, heater_item, steamturbine_item, solarboiler_item, caliometric_burner_item,
            mirror_item_hidden,
            leadwire_item, grinder_item, mixer_item, crystallizer_item,
            greenware_item,
            rocket_engine_item_hidden,
            parasieve_item,
            compression_crafter_item;
    public ItemStack silver_ore_item, silver_block_item, lead_block_item,
            dark_iron_block_item;
    public ItemStack is_factory, is_lamp, is_lightair;
    public ItemBagOfHolding bag_of_holding;
    public ItemPocketTable pocket_table;
    public ItemCraftingComponent diamond_shard;
    public ItemStack diamond_shard_packet;
    public IRecipe boh_upgrade_recipe;
    public ItemWrathIgniter wrath_igniter;
    public ItemCraftingComponent silver_ingot, lead_ingot;
    public ItemCraftingComponent dark_iron;
    public ItemMachineUpgrade router_item_filter, router_machine_filter, router_speed,
            router_thorough, router_throughput, router_eject;
    public ItemMachineUpgrade barrel_enlarge;
    public ItemAcidBottle acid;
    public ItemCraftingComponent magnet, insulated_coil, motor, fan, diamond_cutting_head;
    public ItemStack sulfuric_acid, aqua_regia;
    public ItemChargeMeter charge_meter;
    public ItemBlockProxy mirror;
    public ItemBattery battery;
    public ItemOreProcessing ore_dirty_gravel, ore_clean_gravel, ore_reduced, ore_crystal;
    public ItemCraftingComponent sludge;
    public ItemSculptingTool sculpt_tool;
    public ItemGlazeBucket glaze_bucket;
    public ItemStack base_common, glaze_base_mimicry;
    public ItemCraftingComponent logicMatrix, logicMatrixIdentifier, logicMatrixController;
    public ItemMatrixProgrammer logicMatrixProgrammer;
    public Fluid steamFluid;
    public ItemCraftingComponent nether_powder, rocket_fuel;
    public ItemBlockProxy rocket_engine;
    public ItemServoMotor servo_motor_placer;
    public ItemServoRailWidget servo_widget_instruction, servo_widget_decor;
    public ItemStack dark_iron_sprocket, sprocket_motor;
    public ItemDayBarrel daybarrel;
    public ItemSocketPart socket_part;
    public ItemCraftingComponent instruction_plate;

    public Material materialMachine = new Material(MapColor.ironColor);
    
    WorldgenManager worldgenManager;

    static void registerItem(Item item) {
        GameRegistry.registerItem(item, item.getUnlocalizedName(), Core.modId);
    }
    
    static void registerItem(Block block) {
        registerItem(new ItemStack(block).getItem());
    }
    
    public void makeBlocks() {
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            //Theoretically, not necessary. I bet BUKKIT would flip its shit tho.
            blockRender = new BlockRenderHelper();
            factory_rendering_block = new BlockFactorization(FzConfig.factory_block_id);
            Block.blocksList[factory_rendering_block.blockID] = null;
        }
        serverTraceHelper = new BlockRenderHelper();
        clientTraceHelper = new BlockRenderHelper();
        factory_block = new BlockFactorization(FzConfig.factory_block_id);
        lightair_block = new BlockLightAir(FzConfig.lightair_id);
        resource_block = new BlockResource(FzConfig.resource_id);
        is_factory = new ItemStack(factory_block);
        is_lightair = new ItemStack(lightair_block);
        dark_iron_ore = new BlockDarkIronOre(FzConfig.dark_iron_ore_id).setUnlocalizedName("factorization:darkIronOre").setTextureName("stone").setCreativeTab(Core.tabFactorization).setHardness(3.0F).setResistance(5.0F);
        fractured_bedrock_block = new Block(FzConfig.fractured_bedrock_id, Material.rock).setBlockUnbreakable().setResistance(6000000).setUnlocalizedName("bedrock").setTextureName("bedrock").setCreativeTab(Core.tabFactorization);
        ItemBlock itemDarkIronOre = new ItemBlock(FzConfig.dark_iron_ore_id - 256); //&lookofdisapproval;
        ItemBlock itemFracturedBedrock = new ItemBlock(FzConfig.fractured_bedrock_id - 256); //&lookofdisapproval;

        GameRegistry.registerBlock(factory_block, ItemFactorizationBlock.class, "FZ factory");
        GameRegistry.registerBlock(lightair_block, "FZ Lightair");
        GameRegistry.registerBlock(resource_block, ItemBlockResource.class, "FZ resource");
        GameRegistry.registerCraftingHandler(this);
        
        registerItem(factory_block);
        registerItem(lightair_block);
        registerItem(resource_block);
        registerItem(dark_iron_ore);
        registerItem(fractured_bedrock_block);

        Core.tab(factory_block, Core.TabType.BLOCKS);
        Core.tab(resource_block, TabType.BLOCKS);
        
        worldgenManager = new WorldgenManager();
        
        final Block vanillaDiamond = Block.blockDiamond;
        final int diamondId = vanillaDiamond.blockID;
        Block.blocksList[diamondId] = null;
        BlockOreStorageShatterable newDiamond = new BlockOreStorageShatterable(diamondId, vanillaDiamond);
        newDiamond.setHardness(5.0F).setResistance(10.0F).setStepSound(Block.soundMetalFootstep).setUnlocalizedName("blockDiamond");
        //Block.blockDiamond /* blockDiamond */ = newDiamond;
//		ReflectionHelper.setPrivateValue(Block.class, null, newDiamond, "blockDiamond", "blockDiamond"); TODO: Reflection-set blockDiamond.
    }

    /*private void addName(Object what, String name) {
        Core.proxy.addName(what, name);
    }*/

    HashSet<Integer> added_ids = new HashSet<Integer>();

    public int itemID(String name, int default_id) {
        int id = FzConfig.config.getItem("item", name, default_id).getInt();
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
    
    void postMakeItems() {
        for (int id : added_ids) {
            Item it = Item.itemsList[id + 256];
            if (it == null) {
                continue; //This is weird.
            }
            it.setTextureName(it.getUnlocalizedName());
            registerItem(it);
        }
    }

    public void makeItems() {
        ore_dirty_gravel = new ItemOreProcessing(itemID("oreDirtyGravel", 9034), 2 * 16 + 4, "gravel");
        ore_clean_gravel = new ItemOreProcessing(itemID("oreCleanGravel", 9035), 2 * 16 + 5, "clean");
        ore_reduced = new ItemOreProcessing(itemID("oreReduced", 9036), 2 * 16 + 6, "reduced");
        ore_crystal = new ItemOreProcessing(itemID("oreCrystal", 9037), 2 * 16 + 7, "crystal");
        sludge = new ItemCraftingComponent(itemID("sludge", 9039), "sludge");
        OreDictionary.registerOre("sludge", sludge);
        //ItemBlocks
        item_factorization = (ItemFactorizationBlock) Item.itemsList[factory_block.blockID];
        item_resource = (ItemBlockResource) Item.itemsList[resource_block.blockID];

        //BlockFactorization stuff
        router_item = FactoryType.ROUTER.itemStack();
        servorail_item = FactoryType.SERVORAIL.itemStack();
        empty_socket_item = FactoryType.SOCKET_EMPTY.itemStack();
        parasieve_item = FactoryType.PARASIEVE.itemStack();
        compression_crafter_item = FactoryType.COMPRESSIONCRAFTER.itemStack();
        barrel_item = FactoryType.BARREL.itemStack();
        daybarrel_item_hidden = FactoryType.DAYBARREL.itemStack();
        stamper_item = FactoryType.STAMPER.itemStack();
        lamp_item = FactoryType.LAMP.itemStack();
        packager_item = FactoryType.PACKAGER.itemStack();
        slagfurnace_item = FactoryType.SLAGFURNACE.itemStack();
        battery_item_hidden = FactoryType.BATTERY.itemStack();
        leydenjar_item = FactoryType.LEYDENJAR.itemStack();
        steamturbine_item = FactoryType.STEAMTURBINE.itemStack();
        solarboiler_item = FactoryType.SOLARBOILER.itemStack();
        caliometric_burner_item = FactoryType.CALIOMETRIC_BURNER.itemStack();
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


        diamond_shard = new ItemCraftingComponent(itemID("diamondShard", 9006), "diamond_shard");
        wrath_igniter = new ItemWrathIgniter(itemID("wrathIgniter", 9007));
        dark_iron = new ItemCraftingComponent(itemID("darkIron", 9008), "dark_iron_ingot");
        
        lead_ingot = new ItemCraftingComponent(itemID("leadIngot", 9014), "lead_ingot");
        silver_ingot = new ItemCraftingComponent(itemID("silverIngot", 9015), "silver_ingot");
        OreDictionary.registerOre("oreSilver", silver_ore_item);
        OreDictionary.registerOre("ingotSilver", new ItemStack(silver_ingot));
        OreDictionary.registerOre("ingotLead", new ItemStack(lead_ingot));
        OreDictionary.registerOre("blockSilver", silver_block_item);
        OreDictionary.registerOre("blockLead", lead_block_item);
        OreDictionary.registerOre("oreFzDarkIron", dark_iron_ore);
        OreDictionary.registerOre("ingotFzDarkIron", dark_iron);
        OreDictionary.registerOre("blockFzDarkIron", dark_iron_block_item);


        bag_of_holding = new ItemBagOfHolding(itemID("bagOfHolding", 9001));
        
        logicMatrixProgrammer = new ItemMatrixProgrammer(itemID("logicMatrixProgrammer", 9043));
        for (String chestName : new String[] {
                ChestGenHooks.STRONGHOLD_LIBRARY,
                ChestGenHooks.DUNGEON_CHEST,
                //TODO: Nether fortresses? Needs a forge thing tho.
                }) {
            ChestGenHooks dungeon = ChestGenHooks.getInfo(chestName);
            dungeon.addItem(new WeightedRandomChestContent(new ItemStack(logicMatrixProgrammer), 1, 1, 35)); //XXX TODO: Temporary, put these on asteroids.
        }
        logicMatrix = new ItemCraftingComponent(itemID("logicMatrix", 9044), "logic_matrix");
        logicMatrixIdentifier = new ItemCraftingComponent(itemID("logicMatrixID", 9045), "logic_matrix_identifier");
        logicMatrixController = new ItemCraftingComponent(itemID("logicMatrixCtrl", 9063), "logic_matrix_controller");

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
        mirror = new ItemBlockProxy(itemID("mirror", 9030), mirror_item_hidden, "mirror", TabType.CHARGE);
        battery = new ItemBattery(itemID("battery", 9033));
        leydenjar_item_full = ItemStack.copyItemStack(leydenjar_item);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("storage", TileEntityLeydenJar.max_storage);
        leydenjar_item_full.setTagCompound(tag);
        
        //ceramics
        sculpt_tool = new ItemSculptingTool(itemID("sculptTool", 9041));
        glaze_bucket = new ItemGlazeBucket(itemID("glazeBucket", 9055));

        //Misc
        pocket_table = new ItemPocketTable(itemID("pocketCraftingTable", 9002));
        steamFluid = new Fluid("steam").setDensity(-500).setGaseous(true).setViscosity(100).setUnlocalizedName("factorization:fluid/steam").setTemperature(273 + 110);
        FluidRegistry.registerFluid(steamFluid);
        
        //Rocketry
        nether_powder = new ItemCraftingComponent(itemID("netherPowder", 9050), "nether_powder");
        if (FzConfig.enable_dimension_slice) {
            rocket_fuel = new ItemCraftingComponent(itemID("heldRocketFuel", 9051), "rocket/rocket_fuel");
            rocket_engine = new ItemBlockProxy(itemID("rocketEngine", 9053), rocket_engine_item_hidden, "rocket/rocket_engine", TabType.ROCKETRY);
            rocket_engine.setMaxStackSize(1);
        }
        
        //Servos
        servo_motor_placer = new ItemServoMotor(itemID("servoMotorPlacer", 9056));
        servo_widget_decor = new ItemServoRailWidget(itemID("servoWidgetDecor", 9057), "servo/decorator");
        servo_widget_instruction = new ItemServoRailWidget(itemID("servoWidgetInstruction", 9061), "servo/component");
        servo_widget_decor.setMaxStackSize(16);
        servo_widget_instruction.setMaxStackSize(1);
        dark_iron_sprocket = new ItemStack(new ItemCraftingComponent(itemID("darkIronSprocket", 9059), "servo/sprocket"));
        sprocket_motor = new ItemStack(new ItemCraftingComponent(itemID("servoMotor", 9060), "servo/servo_motor"));
        socket_part = new ItemSocketPart(itemID("socketPart", 9064), "socket/", TabType.SERVOS);
        instruction_plate = new ItemCraftingComponent(itemID("instructionPlate", 9065), "servo/instruction_plate", TabType.SERVOS);
        instruction_plate.setSpriteNumber(0);
        
        socket_lacerator = FactoryType.SOCKET_LACERATOR.asSocketItem();
        socket_robot_hand = FactoryType.SOCKET_ROBOTHAND.asSocketItem();
        socket_shifter = FactoryType.SOCKET_SHIFTER.asSocketItem();
        
        //Barrels
        daybarrel = new ItemDayBarrel(itemID("daybarrelItem", 9062), "daybarrel");
        postMakeItems();
    }

    public void recipe(ItemStack res, Object... params) {
        GameRegistry.addRecipe(res, params);
    }

    public void shapelessRecipe(ItemStack res, Object... params) {
        if (res == null) {
            return;
        }
        GameRegistry.addShapelessRecipe(res, params);
    }

    public void oreRecipe(ItemStack res, Object... params) {
        if (res == null) {
            return;
        }
        convertOreItems(params);
        GameRegistry.addRecipe(new ShapedOreRecipe(res, params));
    }

    public void shapelessOreRecipe(ItemStack res, Object... params) {
        if (res == null) {
            return;
        }
        convertOreItems(params);
        GameRegistry.addRecipe(new ShapelessOreRecipe(res, params));
    }
    
    private void convertOreItems(Object[] params) {
        for (int i = 0; i < params.length; i++) {
            if (params[i] == Block.cobblestone) {
                params[i] = "cobblestone";
            } else if (params[i] == Block.stone) {
                params[i] = "stone";
            } else if (params[i] == Item.stick) {
                params[i] = "stickWood";
            }
        }
    }

    public void makeRecipes() {
        recipe(new ItemStack(Block.stoneDoubleSlab),
                "-",
                "-",
                '-', new ItemStack(Block.stoneSingleSlab));
        recipe(new ItemStack(Block.stoneDoubleSlab, 4, 8),
                "##",
                "##",
                '#', new ItemStack(Block.stoneDoubleSlab));
        recipe(new ItemStack(Block.stoneDoubleSlab, 2, 9),
                "#",
                "#",
                '#', new ItemStack(Block.sandStone, 1, 2));
        
        shapelessRecipe(new ItemStack(dark_iron, 9), dark_iron_block_item);
        recipe(dark_iron_block_item,
                "III",
                "III",
                "III",
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
        boh_upgrade_recipe = FzUtil.createShapelessRecipe(BOH, BOH, dark_iron, Item.enderPearl, Item.leather); // I !
        // Pocket Crafting Table (pocket table)
        oreRecipe(new ItemStack(pocket_table),
                " #",
                "| ",
                '#', Block.workbench,
                '|', Item.stick);

        recipe(new ItemStack(logicMatrixIdentifier),
                "MiX",
                'M', logicMatrix,
                'i', Item.netherQuartz,
                'X', logicMatrixProgrammer);
        oreRecipe(new ItemStack(logicMatrixController),
                "MiX",
                'M', logicMatrix,
                'i', "ingotSilver",
                'X', logicMatrixProgrammer);
        recipe(new ItemStack(logicMatrixProgrammer),
                "MiX",
                'M', logicMatrix,
                'i', dark_iron,
                'X', logicMatrixProgrammer);
        recipe(new ItemStack(logicMatrixProgrammer),
                "DSI",
                " #>",
                "BSI",
                'D', Item.record13,
                'B', Item.record11,
                'S', diamond_shard,
                'I', dark_iron,
                '#', logicMatrix,
                '>', Item.comparator);
        int librarianVillager = 1;
        VillagerRegistry.instance().registerVillageTradeHandler(librarianVillager, new IVillageTradeHandler() {
            @Override
            public void manipulateTradesForVillager(EntityVillager villager, MerchantRecipeList recipeList, Random random) {
                int min = 2, max = 3;
                Item item = Core.registry.logicMatrixProgrammer;
                float chance = 1;
                
                if (min > 0 && max > 0) {
                    EntityVillager.blacksmithSellingList.put(item.itemID, new Tuple(min, max));
                }
                EntityVillager.addBlacksmithItem(recipeList, item.itemID, random, chance);
            }
        });
        
        TileEntityCrystallizer.addRecipe(new ItemStack(Block.blockRedstone), new ItemStack(logicMatrix), 1, Core.registry.aqua_regia);

        //wrathfire igniter
        recipe(new ItemStack(wrath_igniter),
                "D ",
                " B",
                'D', diamond_shard,
                'B', Item.netherrackBrick /* netherbrick */);

        //Resources
        recipe(new ItemStack(lead_ingot, 9), "#", '#', lead_block_item);
        recipe(new ItemStack(silver_ingot, 9), "#", '#', silver_block_item);
        oreRecipe(lead_block_item, "###", "###", "###", '#', "ingotLead");
        oreRecipe(silver_block_item, "###", "###", "###", '#', "ingotSilver");
        FurnaceRecipes.smelting().addSmelting(resource_block.blockID, ResourceType.SILVERORE.md, new ItemStack(silver_ingot), 0.3F);
        FurnaceRecipes.smelting().addSmelting(dark_iron_ore.blockID, 0, new ItemStack(dark_iron), 0.5F);

        //ceramics
        oreRecipe(new ItemStack(sculpt_tool),
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
        
        base_common = glaze_bucket.makeCraftingGlaze("base_common");
        glaze_base_mimicry = glaze_bucket.makeCraftingGlaze("base_mimicry");
        
        glaze_bucket.addGlaze(glaze_base_mimicry);
        
        ItemStack charcoal = new ItemStack(Item.coal, 1, 1);
        ItemStack bonemeal = new ItemStack(Item.dyePowder, 1, 15);
        ItemStack lapis = new ItemStack(Item.dyePowder, 1, 4);
        ItemStack lead_chunks = new ItemStack(ore_reduced, 1, ItemOreProcessing.OreType.LEAD.ID);
        ItemStack iron_chunks = new ItemStack(ore_reduced, 1, ItemOreProcessing.OreType.IRON.ID);
        Item netherquartz = Item.netherQuartz;
        Item netherbrick = Item.netherrackBrick;
        Block sand = Block.sand;
        Item redstone=  Item.redstone;
        Item slimeBall = Item.slimeBall;
        ItemStack blackWool = new ItemStack(Block.cloth, 1, 15);
        
        shapelessOreRecipe(base_common, new ItemStack(glaze_bucket), Item.bucketWater, Block.sand, Item.clay);
        shapelessOreRecipe(glaze_base_mimicry, base_common, Item.redstone, Item.slimeBall, lapis);
        
        BasicGlazes.ST_VECHS_BLACK.recipe(base_common, blackWool, charcoal);
        BasicGlazes.TEMPLE_WHITE.recipe(base_common, bonemeal, bonemeal);
        BasicGlazes.SALLYS_WHITE.recipe(base_common, netherquartz, netherquartz);
        BasicGlazes.CLEAR.recipe(base_common, sand, sand);
        BasicGlazes.REDSTONE_OXIDE.recipe(base_common, redstone);
        BasicGlazes.LAPIS_OXIDE.recipe(base_common, lapis);
        BasicGlazes.PURPLE_OXIDE.recipe(base_common, redstone, lapis);
        BasicGlazes.LEAD_OXIDE.recipe(base_common, lead_chunks);
        BasicGlazes.FIRE_ENGINE_RED.recipe(base_common, redstone, redstone);
        BasicGlazes.CELEDON.recipe(base_common, sand, slimeBall);
        BasicGlazes.IRON_BLUE.recipe(base_common, lapis, iron_chunks);
        BasicGlazes.STONEWARE_SLIP.recipe(base_common, sludge, sludge);
        BasicGlazes.TENMOKU.recipe(base_common, netherbrick, netherbrick);
        BasicGlazes.PEKING_BLUE.recipe(base_common, lapis, netherquartz);
        BasicGlazes.SHINO.recipe(base_common, redstone, netherquartz);
        
        ItemStack waterFeature = glaze_bucket.makeMimicingGlaze(Block.waterMoving.blockID, 0, -1);
        ItemStack lavaFeature = glaze_bucket.makeMimicingGlaze(Block.lavaMoving.blockID, 0, -1);
        shapelessOreRecipe(waterFeature, base_common, Item.bucketWater);
        shapelessOreRecipe(lavaFeature, base_common, Item.bucketLava);
        
        Core.registry.glaze_bucket.doneMakingStandardGlazes();
        
        //Sculpture combiniation recipe
        GameRegistry.addRecipe(new IRecipe() {
            ArrayList<ItemStack> merge(InventoryCrafting inv) {
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
                    if (FzUtil.similar(Core.registry.greenware_item, is)) {
                        match.add(is);
                    } else {
                        return null;
                    }
                }
                if (match.size() != 2) {
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
                    if (FzUtil.couldMerge(glaze_base_mimicry, is)) {
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
                        if (b == null || b.getUnlocalizedName().equals("tile.ForgeFiller")) {
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
                return glaze_base_mimicry;
            }
            
            final int[] side_map = new int[] {
                    1, 2, 1,
                    4, 0, 5,
                    0, 3, 0
            };
            @Override
            public ItemStack getCraftingResult(InventoryCrafting inventorycrafting) {
                int bucket_slot = -1, block_slot = -1;

                for (int i = 0; i < inventorycrafting.getSizeInventory(); i++) {
                    ItemStack is = inventorycrafting.getStackInSlot(i);
                    if (is == null) {
                        continue;
                    }
                    if (FzUtil.couldMerge(glaze_base_mimicry, is)) {
                        bucket_slot = i;
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
                    if (b == null || b.getUnlocalizedName().equals("tile.ForgeFiller")) {
                        continue;
                    }
                    block_slot = i;
                }
                if (bucket_slot == -1 || block_slot == -1) {
                    return null;
                }
                int side = 0;
                try {
                    if (block_slot == 4) {
                        side = side_map[block_slot];
                    } else {
                        side = -1;
                    }
                } catch (ArrayIndexOutOfBoundsException e) {}
                ItemStack is = inventorycrafting.getStackInSlot(block_slot);
                return glaze_bucket.makeMimicingGlaze(is.itemID, is.getItemDamage(), side);
            }
        });

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

        // Barrel
        // Add the recipes for vanilla woods.
        for (int i = 0; i < 4; i++) {
            ItemStack log = new ItemStack(Block.wood, 1, i);
            ItemStack slab = new ItemStack(Block.woodSingleSlab, 1, i);
            TileEntityDayBarrel.makeRecipe(log, slab);
        }
        
        // Craft stamper
        oreRecipe(stamper_item,
                "#p#",
                "#S#",
                "#C#",
                '#', Block.cobblestone,
                'p', Block.pistonBase,
                'S', Item.stick,
                'C', Block.workbench);

        //Packager
        oreRecipe(packager_item,
                "#p#",
                "I I",
                "#C#",
                '#', Block.cobblestone,
                'p', Block.pistonBase,
                'I', Item.ingotIron,
                'C', Block.workbench);
        
        //Compression Crafter
        oreRecipe(compression_crafter_item,
                "D",
                "C",
                "P",
                'D', dark_iron,
                'C', Block.workbench,
                'P', Block.pistonBase);

        // Wrath lamp
        oreRecipe(lamp_item,
                "ISI",
                "GWG",
                "ISI",
                'I', dark_iron,
                'S', "ingotSilver",
                'G', Block.thinGlass,
                'W', new ItemStack(wrath_igniter, 1, FzUtil.WILDCARD_DAMAGE));

        //Slag furnace
        recipe(slagfurnace_item,
                "CFC",
                "C C",
                "CFC",
                'C', Block.cobblestone,
                'F', Block.furnaceIdle);
        
        //most ores give 0.4F stone, but redstone is dense.
        //mining redstone normally gives 4 to 6 ore. 5.8F should get you a slightly better yield.
        TileEntitySlagFurnace.SlagRecipes.register(Block.oreRedstone, 5.8F, Item.redstone, 0.2F, Block.stone);
        
        
        oreRecipe(greenware_item,
                "c",
                "-",
                'c', Item.clay,
                '-', "slabWood");

        //Electricity

        
        shapelessRecipe(sulfuric_acid, Item.gunpowder, Item.gunpowder, Item.coal, Item.potion);
        shapelessOreRecipe(sulfuric_acid, "dustSulfur", Item.coal, Item.potion);
        shapelessRecipe(aqua_regia, sulfuric_acid, nether_powder, Item.fireballCharge);
        shapelessRecipe(aqua_regia, sulfuric_acid, Item.blazePowder, Item.fireballCharge); //I'd kind of like this to be a recipe for a different — but compatible — aqua regia. 
        recipe(new ItemStack(fan),
                "I I",
                " I ",
                "I I",
                'I', Item.ingotIron);
        if (FzConfig.enable_solar_steam) {
            recipe(solarboiler_item,
                    "I#I",
                    "I I",
                    "III",
                    'I', Item.ingotIron,
                    '#', Block.fenceIron
                    );
        }
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
        oreRecipe(caliometric_burner_item,
                "BPB",
                "BAB",
                "BLB",
                'B', Item.bone,
                'P', Block.pistonStickyBase,
                'A', sulfuric_acid,
                'L', Item.leather);
        oreRecipe(new ItemStack(charge_meter),
                "WSW",
                "W|W",
                "LIL",
                'W', "plankWood",
                'S', Item.sign,
                '|', Item.stick,
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
        }
        oreRecipe(leydenjar_item,
                "#G#",
                "#L#",
                "L#L",
                '#', Block.thinGlass,
                'G', Block.glass,
                'L', "ingotLead");

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
        if (FzConfig.enable_solar_steam) { //NOTE: This'll probably cause a bug when we use mirrors for other things
            oreRecipe(new ItemStack(mirror),
                    "SSS",
                    "S#S",
                    "SSS",
                    'S', "ingotSilver",
                    '#', Block.thinGlass);
        }
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
        /* oreRecipe(grinder_item,
                "LIL",
                "I*I",
                "IMI",
                'L', "ingotLead",
                'I', Item.ingotIron,
                '*', diamond_cutting_head,
                'M', motor);*/
        shapelessRecipe(socket_lacerator, grinder_item);
        
        //Values based on Fortune I
        TileEntityGrinder.addRecipe(new ItemStack(Block.oreCoal), new ItemStack(Item.coal), 1.5F);
        TileEntityGrinder.addRecipe("oreRedstone", new ItemStack(Item.redstone), 5F);
        TileEntityGrinder.addRecipe("oreDiamond", new ItemStack(Item.diamond), 1.25F);
        TileEntityGrinder.addRecipe("oreEmerald", new ItemStack(Item.emerald), 1.25F);
        TileEntityGrinder.addRecipe(new ItemStack(Block.oreNetherQuartz), new ItemStack(Item.netherQuartz), 2.5F /* It should actually be 1.25, but I feel like being EXTRA generous here. */);
        TileEntityGrinder.addRecipe("oreLapis", new ItemStack(Item.dyePowder, 1, 4), 8.5F);
        
        //VANILLA RECIPES
        //These are based on going through the Search tab in the creative menu
        //When we turn the Grinder into a Lacerator, anything not specified here will be broken in the usual manner.
        TileEntityGrinder.addRecipe(Block.stone, new ItemStack(Block.cobblestone), 1);
        TileEntityGrinder.addRecipe(Block.cobblestone, new ItemStack(Block.gravel), 1);
        TileEntityGrinder.addRecipe("treeSapling", new ItemStack(Item.stick), 1.25F);
        TileEntityGrinder.addRecipe(Block.gravel, new ItemStack(Block.sand), 1);
        TileEntityGrinder.addRecipe("treeLeaves", new ItemStack(Item.stick), 0.5F);
        TileEntityGrinder.addRecipe(Block.glass, new ItemStack(Block.sand), 0.1F);
        TileEntityGrinder.addRecipe(Block.web, new ItemStack(Item.silk), 0.25F);
        TileEntityGrinder.addRecipe(Block.brick, new ItemStack(Item.brick), 3.5F);
        TileEntityGrinder.addRecipe(Block.cobblestoneMossy, new ItemStack(Block.gravel), 1);
        //Now's a fine time to add the mob spawner
        TileEntityGrinder.addRecipe(Block.mobSpawner, new ItemStack(Block.fenceIron), 2.5F);
        //No stairs, no slabs.
        //Chest, but we don't want to support wood transmutes.
        TileEntityGrinder.addRecipe(Block.furnaceIdle, new ItemStack(Block.cobblestone), 7F);
        TileEntityGrinder.addRecipe(Block.furnaceBurning, new ItemStack(Block.stone), 7F);
        TileEntityGrinder.addRecipe(Block.ladder, new ItemStack(Item.stick), 1.5F);
        TileEntityGrinder.addRecipe(Block.snow, new ItemStack(Item.snowball), 0.25F);
        TileEntityGrinder.addRecipe(Block.blockSnow, new ItemStack(Item.snowball), 4F);
        TileEntityGrinder.addRecipe(Block.blockClay, new ItemStack(Item.clay), 4F);
        TileEntityGrinder.addRecipe(Block.fence, new ItemStack(Item.stick), 2.5F);
        //Netherrack dust is handled elsewhere!
        TileEntityGrinder.addRecipe(Block.glowStone, new ItemStack(Item.glowstone), 4F);
        TileEntityGrinder.addRecipe(Block.trapdoor, new ItemStack(Item.stick), 3.5F);
        TileEntityGrinder.addRecipe(Block.stoneBrick, new ItemStack(Block.cobblestone), 0.75F);
        TileEntityGrinder.addRecipe(Block.thinGlass, new ItemStack(Block.sand), 0.1F/16F);
        TileEntityGrinder.addRecipe(Block.melon, new ItemStack(Item.melon), 7.75F);
        TileEntityGrinder.addRecipe(Block.fenceGate, new ItemStack(Item.stick), 2.5F);
        TileEntityGrinder.addRecipe(Block.netherBrick, new ItemStack(Item.netherrackBrick), 3.5F);
        TileEntityGrinder.addRecipe(Block.netherFence, new ItemStack(Item.netherrackBrick), 2.5F);
        //TODO: Asbestos from endstone
        TileEntityGrinder.addRecipe(Block.redstoneLampActive, new ItemStack(Item.glowstone), 4F);
        TileEntityGrinder.addRecipe(Block.redstoneLampIdle, new ItemStack(Item.glowstone), 4F);
        //Don't want to be responsible for some netherstar exploit involving a beacon, so no beacon.
        //Walls have weird geometry
        TileEntityGrinder.addRecipe(Block.blockNetherQuartz, new ItemStack(Item.netherQuartz), 3.5F);
        TileEntityGrinder.addRecipe(Block.hay /* blockHay */, new ItemStack(Item.wheat), 8.25F);
        
        //So, that's blocks. How about items?
        TileEntityGrinder.addRecipe(Item.book, new ItemStack(Item.leather), 0.75F); //Naughty.
        TileEntityGrinder.addRecipe(Item.enchantedBook, new ItemStack(Item.leather), 0.9F);
        //NOTE: We're going to have to do something tricksy for the lacerator...
        //These go to Block.skull, but the item damagevalue != block metadata.
        TileEntityGrinder.addRecipe(new ItemStack(Item.skull, 1, 0 /* skele */), new ItemStack(Item.dyePowder, 1, 15 /* bonemeal */), 6.5F);
        TileEntityGrinder.addRecipe(new ItemStack(Item.skull, 1, 2 /* zombie */), new ItemStack(Item.rottenFlesh), 2.5F);
        TileEntityGrinder.addRecipe(new ItemStack(Item.skull, 1, 3 /* player */), new ItemStack(Item.rottenFlesh), 3.5F);
        TileEntityGrinder.addRecipe(new ItemStack(Item.skull, 1, 4 /* creeper */), new ItemStack(Item.gunpowder), 1.5F);
        
        
        
        oreRecipe(mixer_item,
                " X ",
                " M ",
                "LUL",
                'X', fan,
                'M', motor,
                'L', "ingotLead",
                'U', Item.cauldron);
        FurnaceRecipes.smelting().addSmelting(sludge.itemID, 0, new ItemStack(Item.clay), 0.1F);
        oreRecipe(crystallizer_item,
                "-",
                "S",
                "U",
                '-', Item.stick,
                'S', Item.silk,
                'U', Item.cauldron);
        ItemStack lime = new ItemStack(Item.dyePowder, 1, 10);
        TileEntityCrystallizer.addRecipe(lime, new ItemStack(Item.slimeBall), 1, new ItemStack(Item.bucketMilk));
        
        //Rocketry
        TileEntityGrinder.addRecipe(new ItemStack(Block.netherrack), new ItemStack(nether_powder, 1), 1);
        if (FzConfig.enable_dimension_slice) {
            shapelessRecipe(new ItemStack(rocket_fuel, 9),
                    nether_powder, nether_powder, nether_powder,
                    nether_powder, Item.fireballCharge, nether_powder,
                    nether_powder, nether_powder, nether_powder);
            recipe(new ItemStack(rocket_engine),
                    "#F#",
                    "#I#",
                    "I I",
                    '#', Block.blockIron,
                    'F', rocket_fuel,
                    'I', Item.ingotIron);
        }
        
        //Servos
        makeServoRecipes();
        oreRecipe(empty_socket_item,
                "#",
                "-",
                "#",
                '#', Block.fenceIron,
                '-', "slabWood");
        oreRecipe(FactoryType.SOCKET_LACERATOR.asSocketItem(),
                "*",
                "M",
                '*', diamond_cutting_head,
                'M', motor);
        oreRecipe(FactoryType.SOCKET_SHIFTER.asSocketItem(),
                "V",
                "@",
                "D",
                'V', Block.hopperBlock,
                '@', logicMatrixController,
                'D', Block.dropper);
        oreRecipe(socket_robot_hand,
                "+*P",
                "+@+",
                "P*+",
                '+', servorail_item,
                '*', dark_iron_sprocket,
                '@', logicMatrixController,
                'P', Block.pistonBase);
        oreRecipe(new ItemStack(instruction_plate, 5),
                "I ",
                "I>",
                "I ",
                'I', dark_iron,
                '>', logicMatrixProgrammer);
    }
    
    private void makeServoRecipes() {
        ItemStack rails = servorail_item.copy();
        rails.stackSize = 8;
        oreRecipe(rails, "LDL",
                'D', dark_iron,
                'L', "ingotLead");
        ItemStack two_sprockets = dark_iron_sprocket.copy();
        two_sprockets.stackSize = 2;
        oreRecipe(two_sprockets,
                " D ",
                "DSD",
                " D ",
                'D', dark_iron,
                'S', "ingotSilver");
        recipe(sprocket_motor,
                " S ",
                "rMc",
                'S', dark_iron_sprocket,
                'r', Item.redstoneRepeater,
                'M', motor,
                'c', Item.comparator);
        oreRecipe(new ItemStack(servo_motor_placer),
                "M#P",
                " SC",
                "M#P",
                'M', sprocket_motor,
                '#', logicMatrix,
                'P', logicMatrixProgrammer,
                'S', empty_socket_item,
                'C', Block.chest);
        ServoComponent.setupRecipes();
        oreRecipe(parasieve_item,
                "C#C",
                "ImI",
                "CvC",
                'C', Block.cobblestone,
                '#', Block.fenceIron,
                'I', Item.ingotIron,
                'm', logicMatrixIdentifier,
                'v', Block.dropper);
    }

    public void setToolEffectiveness() {
        for (String tool : new String[] { "pickaxe", "axe", "shovel" }) {
            MinecraftForge.removeBlockEffectiveness(factory_block, tool);
            MinecraftForge.removeBlockEffectiveness(resource_block, tool);
        }
        BlockClass.DarkIron.harvest("pickaxe", 2);
        BlockClass.Barrel.harvest("axe", 1);
        BlockClass.Machine.harvest("pickaxe", 1);
        BlockClass.MachineLightable.harvest("pickaxe", 1);
        BlockClass.MachineDynamicLightable.harvest("pickaxe", 1);
        BlockClass.Socket.harvest("axe", 1);
        BlockClass.Socket.harvest("pickaxe", 1);
        MinecraftForge.setBlockHarvestLevel(resource_block, "pickaxe", 2);
        MinecraftForge.setBlockHarvestLevel(dark_iron_ore, "pickaxe", 2);
    }
    
    
    @Override
    public void tickStart(EnumSet<TickType> type, Object... tickData) {
        TileEntityWrathLamp.handleAirUpdates();
        TileEntityWrathFire.updateCount = 0;
    }
    
    @Override
    public void tickEnd(EnumSet<TickType> type, Object... tickData) {
        worldgenManager.tickRetrogenQueue();
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

    @ForgeSubscribe
    public boolean onItemPickup(EntityItemPickupEvent event) {
        EntityPlayer player = event.entityPlayer;
        EntityItem item = event.item;
        if (item == null) {
            return true;
        }
        ItemStack is = item.getEntityItem();
        if (is == null || is.stackSize == 0) {
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
            if (FzUtil.couldMerge(is, here)) {
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
    }

    @Override
    public void onSmelting(EntityPlayer player, ItemStack item) {
    }

    public void sendIMC() {
        //Registers our recipe handlers to a list in NEIPlugins.
        //Format: "Factorization@<Recipe Name>@<outputId that used to view all recipes>"
        for (String msg : new String[] {
                "factorization crystallizer recipes@fz.crystallizing",
                "factorization grinder recipes@fz.grinding",
                "factorization mixer recipes@fz.mixing",
                "factorization slag furnace recipes@fz.slagging"
        }) {
            FMLInterModComms.sendRuntimeMessage(Core.instance, "NEIPlugins", "register-crafting-handler", Core.name + "@" + msg);
        }
        //Disables the Thaumcraft infernal furnace nugget bonus for crystalline metal
        for (OreType ot : ItemOreProcessing.OreType.values()) {
            if (!ot.enabled) {
                continue;
            }
            FMLInterModComms.sendMessage("Thaumcraft", "smeltBonusExclude", new ItemStack(ore_crystal, 1, ot.ID));
        }
    }
    
    public void addOtherRecipes() {
        ArrayList<ItemStack> theLogs = new ArrayList();
        for (ItemStack log : OreDictionary.getOres("logWood")) {
            if (log.itemID == Block.wood.blockID) {
                //Skip vanilla
                continue;
            }
            if (log.getItemDamage() == FzUtil.WILDCARD_DAMAGE && log.itemID < Block.blocksList.length) {
                Block b = Block.blocksList[log.itemID];
                if (b == null) {
                    continue;
                }
                for (int md = 0; md < 16; md++) {
                    ItemStack is = log.copy();
                    is.setItemDamage(md);
                    is.stackSize = 1;
                    theLogs.add(is);
                }
            }
            theLogs.add(log);
        }
        for (ItemStack log : theLogs) {
            log = log.copy();
            List<ItemStack> planks = FzUtil.craft1x1(null, true, log.copy());
            if (planks == null || planks.size() != 1) {
                continue;
            }
            ItemStack plank = planks.get(0).copy();
            plank.stackSize = 1;
            List<ItemStack> slabs = FzUtil.craft3x3(null, true, true, new ItemStack[] {
                    plank.copy(), plank.copy(), plank.copy(),
                    null, null, null,
                    null, null, null
            });
            ItemStack slab;
            String odType;
            if (slabs.size() != 1 || !FzUtil.craft_succeeded) {
                slab = plank;
                odType = "plankWood";
            } else {
                slab = slabs.get(0);
                odType = "slabWood";
            }
            TileEntityDayBarrel.makeRecipe(log, slab);
        }
    }
    
}
