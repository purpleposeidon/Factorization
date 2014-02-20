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
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
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
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import cpw.mods.fml.common.FMLCommonHandler;
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
import factorization.darkiron.BlockDarkIronOre;
import factorization.oreprocessing.BlockOreStorageShatterable;
import factorization.oreprocessing.ItemOreProcessing;
import factorization.oreprocessing.ItemOreProcessing.OreType;
import factorization.oreprocessing.TileEntityCrystallizer;
import factorization.oreprocessing.TileEntityGrinder;
import factorization.oreprocessing.TileEntitySlagFurnace;
import factorization.servo.ItemCommenter;
import factorization.servo.ItemMatrixProgrammer;
import factorization.servo.ItemServoMotor;
import factorization.servo.ItemServoRailWidget;
import factorization.servo.ServoComponent;
import factorization.shared.BlockClass;
import factorization.shared.BlockFactorization;
import factorization.shared.BlockRenderHelper;
import factorization.shared.Core;
import factorization.shared.Core.TabType;
import factorization.shared.FzUtil;
import factorization.shared.ItemBlockProxy;
import factorization.shared.ItemCraftingComponent;
import factorization.shared.ItemFactorizationBlock;
import factorization.shared.Sound;
import factorization.sockets.ItemSocketPart;
import factorization.weird.ItemDayBarrel;
import factorization.weird.ItemPocketTable;
import factorization.weird.TileEntityDayBarrel;
import factorization.wrath.BlockLightAir;
import factorization.wrath.ItemBagOfHolding;
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
    public ItemCraftingComponent silver_ingot, lead_ingot;
    public ItemCraftingComponent dark_iron;
    public ItemAcidBottle acid;
    public ItemCraftingComponent insulated_coil, motor, fan, diamond_cutting_head;
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
    public ItemServoMotor servo_placer;
    public ItemServoRailWidget servo_widget_instruction, servo_widget_decor;
    public ItemStack dark_iron_sprocket, servo_motor;
    public ItemDayBarrel daybarrel;
    @Deprecated
    public ItemSocketPart socket_part;
    public ItemCraftingComponent instruction_plate;
    public ItemCommenter servo_rail_comment_editor;

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
            factory_rendering_block = null;
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

        GameRegistry.registerBlock(factory_block, ItemFactorizationBlocks.class, "FZ factory");
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
        
        final Block vanillaDiamond = Blocks.diamond_block;
        final int diamondId = vanillaDiamond;
        diamondId = null;
        BlockOreStorageShatterable newDiamond = new BlockOreStorageShatterable(diamondId, vanillaDiamond);
        newDiamond.setHardness(5.0F).setResistance(10.0F).setStepSound(Blocks.soundMetalFootstep).setUnlocalizedName("blockDiamond");
        //Blocks.diamond_block /* blockDiamond */ = newDiamond;
//		ReflectionHelper.setPrivateValue(Blocks.class, null, newDiamond, "blockDiamond", "blockDiamond"); TODO: Reflection-set blockDiamond.
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
        if (Items.itemsList[id] != null) {
            throw new RuntimeException("Item ID conflict: " + id + " is already taken by "
                    + Items.itemsList[id] + "; tried to use it for Factorization " + name);
        }
        added_ids.add(default_id);
        return id;
    }
    
    void postMakeItems() {
        for (int id : added_ids) {
            Item it = Items.itemsList[id + 256];
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
        item_factorization = (ItemFactorizationBlock) Items.itemsList[factory_block];
        item_resource = (ItemBlockResource) Items.itemsList[resource_block];

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

        //Electricity
        acid = new ItemAcidBottle(itemID("acid", 9024));
        sulfuric_acid = new ItemStack(acid, 1);
        aqua_regia = new ItemStack(acid, 1, 1);
        OreDictionary.registerOre("sulfuricAcid", sulfuric_acid);
        OreDictionary.registerOre("bottleSulfuricAcid", sulfuric_acid);
        OreDictionary.registerOre("aquaRegia", aqua_regia);
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
        servo_placer = new ItemServoMotor(itemID("servoMotorPlacer", 9056));
        servo_widget_decor = new ItemServoRailWidget(itemID("servoWidgetDecor", 9057), "servo/decorator");
        servo_widget_instruction = new ItemServoRailWidget(itemID("servoWidgetInstruction", 9061), "servo/component");
        servo_widget_decor.setMaxStackSize(16);
        servo_widget_instruction.setMaxStackSize(1);
        dark_iron_sprocket = new ItemStack(new ItemCraftingComponent(itemID("darkIronSprocket", 9059), "servo/sprocket"));
        servo_motor = new ItemStack(new ItemCraftingComponent(itemID("servoMotor", 9060), "servo/servo_motor"));
        socket_part = new ItemSocketPart(itemID("socketPart", 9064), "socket/", TabType.SERVOS);
        instruction_plate = new ItemCraftingComponent(itemID("instructionPlate", 9065), "servo/instruction_plate", TabType.SERVOS);
        instruction_plate.setSpriteNumber(0);
        servo_rail_comment_editor = new ItemCommenter(itemID("servoCommenter", 9066), "servo/commenter");
        
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
    
    void batteryRecipe(ItemStack res, Object... params) {
        for (int damage : new int[] { 1, 2 }) {
            ArrayList items = new ArrayList(params.length);
            for (Object p : params) {
                if (p == battery) {
                    p = new ItemStack(battery, 1, damage);
                }
                items.add(p);
            }
            oreRecipe(res, items.toArray());
        }
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
            if (params[i] == Blocks.cobblestone) {
                params[i] = "cobblestone";
            } else if (params[i] == Blocks.stone) {
                params[i] = "stone";
            } else if (params[i] == Items.stick) {
                params[i] = "stickWood";
            }
        }
    }

    public void makeRecipes() {
        recipe(new ItemStack(Blocks.stoneDoubleSlab),
                "-",
                "-",
                '-', new ItemStack(Blocks.stoneSingleSlab));
        recipe(new ItemStack(Blocks.stoneDoubleSlab, 4, 8),
                "##",
                "##",
                '#', new ItemStack(Blocks.stoneDoubleSlab));
        recipe(new ItemStack(Blocks.stoneDoubleSlab, 2, 9),
                "#",
                "#",
                '#', new ItemStack(Blocks.sandStone, 1, 2));
        
        shapelessRecipe(new ItemStack(dark_iron, 9), dark_iron_block_item);
        recipe(dark_iron_block_item,
                "III",
                "III",
                "III",
                'I', dark_iron);

        // Bag of holding

        ItemStack BOH = new ItemStack(bag_of_holding, 1); //we don't call bag_of_holding.init(BOH) because that would show incorrect info
        recipe(BOH, //NORELEASE: Are we removing the BOH or no?
                "LOL",
                "ILI",
                " I ",
                'I', dark_iron,
                'O', Items.ender_pearl,
                'L', Items.leather); // LOL!
        shapelessRecipe(BOH, BOH, dark_iron, Items.ender_pearl, Items.leather); //ILI!
        boh_upgrade_recipe = FzUtil.createShapelessRecipe(BOH, BOH, dark_iron, Items.ender_pearl, Items.leather); // I !
        // Pocket Crafting Table (pocket table)
        oreRecipe(new ItemStack(pocket_table),
                " #",
                "| ",
                '#', Blocks.crafting_table,
                '|', Items.stick);

        recipe(new ItemStack(logicMatrixIdentifier),
                "MiX",
                'M', logicMatrix,
                'i', Items.quartz,
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
                'D', Items.record_13,
                'B', Items.record_11,
                'S', diamond_shard,
                'I', dark_iron,
                '#', logicMatrix,
                '>', Items.comparator);
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
        
        TileEntityCrystallizer.addRecipe(new ItemStack(Blocks.redstone_block), new ItemStack(logicMatrix), 1, Core.registry.aqua_regia);

        //Resources
        recipe(new ItemStack(lead_ingot, 9), "#", '#', lead_block_item);
        recipe(new ItemStack(silver_ingot, 9), "#", '#', silver_block_item);
        oreRecipe(lead_block_item, "###", "###", "###", '#', "ingotLead");
        oreRecipe(silver_block_item, "###", "###", "###", '#', "ingotSilver");
        FurnaceRecipes.smelting().addSmelting(resource_block, ResourceType.SILVERORE.md, new ItemStack(silver_ingot), 0.3F);
        FurnaceRecipes.smelting().addSmelting(dark_iron_ore, 0, new ItemStack(dark_iron), 0.5F);

        //ceramics
        oreRecipe(new ItemStack(sculpt_tool),
                " c",
                "/ ",
                'c', Items.clay,
                '/', Items.stick);
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
        
        ItemStack charcoal = new ItemStack(Items.coal, 1, 1);
        ItemStack bonemeal = new ItemStack(Items.dye, 1, 15);
        ItemStack lapis = new ItemStack(Items.dye, 1, 4);
        ItemStack lead_chunks = new ItemStack(ore_reduced, 1, ItemOreProcessing.OreType.LEAD.ID);
        ItemStack iron_chunks = new ItemStack(ore_reduced, 1, ItemOreProcessing.OreType.IRON.ID);
        Item netherquartz = Items.quartz;
        Item netherbrick = Items.netherbrick;
        Block sand = Blocks.sand;
        Item redstone=  Items.redstone;
        Item slimeBall = Items.slime_ball;
        ItemStack blackWool = new ItemStack(Blocks.wool, 1, 15);
        
        shapelessOreRecipe(base_common, new ItemStack(glaze_bucket), Items.water_bucket, Blocks.sand, Items.clay_ball);
        shapelessOreRecipe(glaze_base_mimicry, base_common, Items.redstone, Items.slime_ball, lapis);
        
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
        
        ItemStack waterFeature = glaze_bucket.makeMimicingGlaze(Blocks.water, 0, -1);
        ItemStack lavaFeature = glaze_bucket.makeMimicingGlaze(Blocks.lava, 0, -1);
        shapelessOreRecipe(waterFeature, base_common, Items.water_bucket);
        shapelessOreRecipe(lavaFeature, base_common, Items.lava_bucket);
        
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
                        if (is.itemID >= Blocks.blocksList.length) {
                            return false;
                        }
                        int d = is.getItemDamage();
                        if (d < 0 || d > 16) {
                            return false;
                        }
                        Block b = is.itemID;
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
                    if (is.itemID >= Blocks.blocksList.length) {
                        continue;
                    }
                    int d = is.getItemDamage();
                    if (d < 0 || d > 16) {
                        continue;
                    }
                    Block b = is.itemID;
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

        // Barrel
        // Add the recipes for vanilla woods.
        for (int i = 0; i < 4; i++) {
            ItemStack log = new ItemStack(Blocks.wood, 1, i);
            ItemStack slab = new ItemStack(Blocks.woodSingleSlab, 1, i);
            TileEntityDayBarrel.makeRecipe(log, slab);
        }
        
        // Craft stamper
        oreRecipe(stamper_item,
                "#p#",
                "#S#",
                "#C#",
                '#', Blocks.cobblestone,
                'p', Blocks.piston,
                'S', Items.stick,
                'C', Blocks.crafting_table);

        //Packager
        oreRecipe(packager_item,
                "#p#",
                "I I",
                "#C#",
                '#', Blocks.cobblestone,
                'p', Blocks.piston,
                'I', Items.iron_ingot,
                'C', Blocks.crafting_table);
        
        //Compression Crafter
        oreRecipe(compression_crafter_item,
                "D",
                "C",
                "P",
                'D', dark_iron,
                'C', Blocks.crafting_table,
                'P', Blocks.piston);

        // Wrath lamp
//		oreRecipe(lamp_item,
//				"ISI",
//				"GWG",
//				"ISI",
//				'I', dark_iron,
//				'S', "ingotSilver",
//				'G', Blocks.glass_pane,
//				'W', new ItemStack(wrath_igniter, 1, FzUtil.WILDCARD_DAMAGE));

        //Slag furnace
        recipe(slagfurnace_item,
                "CFC",
                "C C",
                "CFC",
                'C', Blocks.cobblestone,
                'F', Blocks.furnaceIdle);
        
        //most ores give 0.4F stone, but redstone is dense.
        //mining redstone normally gives 4 to 6 ore. 5.8F should get you a slightly better yield.
        TileEntitySlagFurnace.SlagRecipes.register(Blocks.oreRedstone, 5.8F, Items.redstone, 0.2F, Blocks.stone);
        
        
        oreRecipe(greenware_item,
                "c",
                "-",
                'c', Items.clay,
                '-', "slabWood");

        //Electricity

        
        shapelessRecipe(sulfuric_acid, Items.gunpowder, Items.gunpowder, Items.coal, Items.potion);
        shapelessOreRecipe(sulfuric_acid, "dustSulfur", Items.coal, Items.potion);
        shapelessRecipe(aqua_regia, sulfuric_acid, nether_powder, Items.fireballCharge);
        shapelessRecipe(aqua_regia, sulfuric_acid, Items.blazePowder, Items.fireballCharge); //I'd kind of like this to be a recipe for a different — but compatible — aqua regia. 
        recipe(new ItemStack(fan),
                "I I",
                " - ",
                "I I",
                'I', Items.ingotIron,
                '-', Blocks.pressurePlateIron);
        if (FzConfig.enable_solar_steam) {
            recipe(solarboiler_item,
                    "I#I",
                    "I I",
                    "III",
                    'I', Items.ingotIron,
                    '#', Blocks.fenceIron
                    );
        }
        oreRecipe(steamturbine_item,
                "I#I",
                "GXG",
                "LML",
                'I', Items.ingotIron,
                '#', Blocks.fenceIron,
                'G', Blocks.thinGlass,
                'X', fan,
                'L', "ingotLead",
                'M', motor );
        oreRecipe(caliometric_burner_item,
                "BPB",
                "BAB",
                "BLB",
                'B', Items.bone,
                'P', Blocks.pistonStickyBase,
                'A', sulfuric_acid,
                'L', Items.leather);
        oreRecipe(new ItemStack(charge_meter),
                "WSW",
                "W|W",
                "LIL",
                'W', "plankWood",
                'S', Items.sign,
                '|', Items.stick,
                'L', "ingotLead",
                'I', Items.ingotIron);
        oreRecipe(new ItemStack(battery, 1, 2),
                "ILI",
                "LAL",
                "ILI",
                'I', Items.ingotIron,
                'L', "ingotLead",
                'A', acid);
        oreRecipe(leydenjar_item,
                "#G#",
                "#L#",
                "L#L",
                '#', Blocks.thinGlass,
                'G', Blocks.glass,
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
                'C', Blocks.clay);
        batteryRecipe(new ItemStack(motor),
                "CIC",
                "CIC",
                "LBL",
                'C', insulated_coil,
                'B', battery,
                'L', "ingotLead",
                'I', Items.ingotIron);
        if (FzConfig.enable_solar_steam) { //NOTE: This'll probably cause a bug when we use mirrors for other things
            oreRecipe(new ItemStack(mirror),
                    "SSS",
                    "S#S",
                    "SSS",
                    'S', "ingotSilver",
                    '#', Blocks.thinGlass);
        }
        ItemStack with_8 = leadwire_item.copy();
        with_8.stackSize = 8;
        oreRecipe(with_8,
                "LLL",
                'L', "ingotLead");
        recipe(new ItemStack(diamond_cutting_head),
                "SSS",
                "S-S",
                "SSS",
                'S', diamond_shard,
                '-', Blocks.pressurePlateIron);
        /* oreRecipe(grinder_item,
                "LIL",
                "I*I",
                "IMI",
                'L', "ingotLead",
                'I', Items.ingotIron,
                '*', diamond_cutting_head,
                'M', motor);*/
        shapelessRecipe(socket_lacerator, grinder_item);
        
        //Values based on Fortune I
        TileEntityGrinder.addRecipe(new ItemStack(Blocks.oreCoal), new ItemStack(Items.coal), 1.5F);
        TileEntityGrinder.addRecipe("oreRedstone", new ItemStack(Items.redstone), 5F);
        TileEntityGrinder.addRecipe("oreDiamond", new ItemStack(Items.diamond), 1.25F);
        TileEntityGrinder.addRecipe("oreEmerald", new ItemStack(Items.emerald), 1.25F);
        TileEntityGrinder.addRecipe(new ItemStack(Blocks.oreNetherQuartz), new ItemStack(Items.netherQuartz), 2.5F /* It should actually be 1.25, but I feel like being EXTRA generous here. */);
        TileEntityGrinder.addRecipe("oreLapis", new ItemStack(Items.dyePowder, 1, 4), 8.5F);
        
        //VANILLA RECIPES
        //These are based on going through the Search tab in the creative menu
        //When we turn the Grinder into a Lacerator, anything not specified here will be broken in the usual manner.
        TileEntityGrinder.addRecipe(Blocks.stone, new ItemStack(Blocks.cobblestone), 1);
        TileEntityGrinder.addRecipe(Blocks.cobblestone, new ItemStack(Blocks.gravel), 1);
        TileEntityGrinder.addRecipe("treeSapling", new ItemStack(Items.stick), 1.25F);
        TileEntityGrinder.addRecipe(Blocks.gravel, new ItemStack(Blocks.sand), 1);
        TileEntityGrinder.addRecipe("treeLeaves", new ItemStack(Items.stick), 0.5F);
        TileEntityGrinder.addRecipe(Blocks.glass, new ItemStack(Blocks.sand), 0.1F);
        TileEntityGrinder.addRecipe(Blocks.web, new ItemStack(Items.silk), 0.25F);
        TileEntityGrinder.addRecipe(Blocks.brick, new ItemStack(Items.brick), 3.5F);
        TileEntityGrinder.addRecipe(Blocks.cobblestoneMossy, new ItemStack(Blocks.gravel), 1);
        //Now's a fine time to add the mob spawner
        TileEntityGrinder.addRecipe(Blocks.mobSpawner, new ItemStack(Blocks.fenceIron), 2.5F);
        //No stairs, no slabs.
        //Chest, but we don't want to support wood transmutes.
        TileEntityGrinder.addRecipe(Blocks.furnaceIdle, new ItemStack(Blocks.cobblestone), 7F);
        TileEntityGrinder.addRecipe(Blocks.furnaceBurning, new ItemStack(Blocks.stone), 7F);
        TileEntityGrinder.addRecipe(Blocks.ladder, new ItemStack(Items.stick), 1.5F);
        TileEntityGrinder.addRecipe(Blocks.snow, new ItemStack(Items.snowball), 0.25F);
        TileEntityGrinder.addRecipe(Blocks.blockSnow, new ItemStack(Items.snowball), 4F);
        TileEntityGrinder.addRecipe(Blocks.clay, new ItemStack(Items.clay), 4F);
        TileEntityGrinder.addRecipe(Blocks.fence, new ItemStack(Items.stick), 2.5F);
        //Netherrack dust is handled elsewhere!
        TileEntityGrinder.addRecipe(Blocks.glowStone, new ItemStack(Items.glowstone), 4F);
        TileEntityGrinder.addRecipe(Blocks.trapdoor, new ItemStack(Items.stick), 3.5F);
        TileEntityGrinder.addRecipe(Blocks.stoneBrick, new ItemStack(Blocks.cobblestone), 0.75F);
        TileEntityGrinder.addRecipe(Blocks.thinGlass, new ItemStack(Blocks.sand), 0.1F/16F);
        TileEntityGrinder.addRecipe(Blocks.melon, new ItemStack(Items.melon), 7.75F);
        TileEntityGrinder.addRecipe(Blocks.fenceGate, new ItemStack(Items.stick), 2.5F);
        TileEntityGrinder.addRecipe(Blocks.netherBrick, new ItemStack(Items.netherrackBrick), 3.5F);
        TileEntityGrinder.addRecipe(Blocks.netherFence, new ItemStack(Items.netherrackBrick), 2.5F);
        //TODO: Asbestos from endstone
        TileEntityGrinder.addRecipe(Blocks.redstoneLampActive, new ItemStack(Items.glowstone), 4F);
        TileEntityGrinder.addRecipe(Blocks.redstoneLampIdle, new ItemStack(Items.glowstone), 4F);
        //Don't want to be responsible for some netherstar exploit involving a beacon, so no beacon.
        //Walls have weird geometry
        TileEntityGrinder.addRecipe(Blocks.blockNetherQuartz, new ItemStack(Items.netherQuartz), 3.5F);
        TileEntityGrinder.addRecipe(Blocks.hay /* blockHay */, new ItemStack(Items.wheat), 8.25F);
        
        //So, that's blocks. How about items?
        TileEntityGrinder.addRecipe(Items.book, new ItemStack(Items.leather), 0.75F); //Naughty.
        TileEntityGrinder.addRecipe(Items.enchantedBook, new ItemStack(Items.leather), 0.9F);
        //NOTE: We're going to have to do something tricksy for the lacerator...
        //These go to Blocks.skull, but the item damagevalue != block metadata.
        TileEntityGrinder.addRecipe(new ItemStack(Items.skull, 1, 0 /* skele */), new ItemStack(Items.dyePowder, 1, 15 /* bonemeal */), 6.5F);
        TileEntityGrinder.addRecipe(new ItemStack(Items.skull, 1, 2 /* zombie */), new ItemStack(Items.rottenFlesh), 2.5F);
        TileEntityGrinder.addRecipe(new ItemStack(Items.skull, 1, 3 /* player */), new ItemStack(Items.rottenFlesh), 3.5F);
        TileEntityGrinder.addRecipe(new ItemStack(Items.skull, 1, 4 /* creeper */), new ItemStack(Items.gunpowder), 1.5F);
        
        
        
        oreRecipe(mixer_item,
                " X ",
                " M ",
                "LUL",
                'X', fan,
                'M', motor,
                'L', "ingotLead",
                'U', Items.cauldron);
        FurnaceRecipes.smelting().addSmelting(sludge.itemID, 0, new ItemStack(Items.clay), 0.1F);
        oreRecipe(crystallizer_item,
                "-",
                "S",
                "U",
                '-', Items.stick,
                'S', Items.silk,
                'U', Items.cauldron);
        ItemStack lime = new ItemStack(Items.dyePowder, 1, 10);
        TileEntityCrystallizer.addRecipe(lime, new ItemStack(Items.slimeBall), 1, new ItemStack(Items.bucketMilk));
        
        //Rocketry
        TileEntityGrinder.addRecipe(new ItemStack(Blocks.netherrack), new ItemStack(nether_powder, 1), 1);
        if (FzConfig.enable_dimension_slice) {
            shapelessRecipe(new ItemStack(rocket_fuel, 9),
                    nether_powder, nether_powder, nether_powder,
                    nether_powder, Items.fireballCharge, nether_powder,
                    nether_powder, nether_powder, nether_powder);
            recipe(new ItemStack(rocket_engine),
                    "#F#",
                    "#I#",
                    "I I",
                    '#', Blocks.blockIron,
                    'F', rocket_fuel,
                    'I', Items.ingotIron);
        }
        
        //Servos
        makeServoRecipes();
        oreRecipe(empty_socket_item,
                "#",
                "-",
                "#",
                '#', Blocks.fenceIron,
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
                'V', Blocks.hopperBlock,
                '@', logicMatrixController,
                'D', Blocks.dropper);
        oreRecipe(socket_robot_hand,
                "+*P",
                "+@+",
                "P*+",
                '+', servorail_item,
                '*', dark_iron_sprocket,
                '@', logicMatrixController,
                'P', Blocks.pistonBase);
        oreRecipe(new ItemStack(instruction_plate, 5),
                "I ",
                "I>",
                "I ",
                'I', dark_iron,
                '>', logicMatrixProgrammer);
        oreRecipe(new ItemStack(servo_rail_comment_editor),
                "#",
                "T",
                '#', instruction_plate,
                'T', Items.sign);
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
        batteryRecipe(servo_motor,
                "qCL",
                "SIB",
                "rCL",
                'q', Items.netherQuartz,
                'r', Items.redstone,
                'S', dark_iron_sprocket,
                'C', insulated_coil,
                'I', Items.ingotIron,
                'B', battery,
                'L', "ingotLead");
        oreRecipe(new ItemStack(servo_placer),
                "M#P",
                " S ",
                "M#P",
                'M', servo_motor,
                '#', logicMatrix,
                'P', logicMatrixProgrammer,
                'S', empty_socket_item);
        ServoComponent.setupRecipes();
        oreRecipe(parasieve_item,
                "C#C",
                "ImI",
                "CvC",
                'C', Blocks.cobblestone,
                '#', Blocks.fenceIron,
                'I', Items.ingotIron,
                'm', logicMatrixIdentifier,
                'v', Blocks.dropper);
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

    @SubscribeEvent
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
            if (log.itemID == Blocks.wood) {
                //Skip vanilla; NORELEASE: 1.7, check the new woods
                continue;
            }
            if (log.getItemDamage() == FzUtil.WILDCARD_DAMAGE && log.itemID < Blocks.blocksList.length) {
                Block b = log.itemID;
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
                slab = plank; // can't convert to slabs; strange wood
                odType = "plankWood";
            } else {
                slab = slabs.get(0);
                odType = "slabWood";
            }
            // Some modwoods have planks, but no slabs, and their planks convert to vanilla slabs.
            // In this case we're going to want to use the plank.
            // But if the plank is also vanilla, then keep the vanilla slab!
            if (slab.itemID == Blocks.woodSingleSlab) {
                if (plank.itemID != Blocks.planks /* NORELEASE: 1.7, new planks */) {
                    slab = plank;
                }
            }
            TileEntityDayBarrel.makeRecipe(log, slab);
        }
    }
    
}
