package factorization.common;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInterModComms;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;
import cpw.mods.fml.common.registry.ExistingSubstitutionException;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.GameRegistry.Type;
import cpw.mods.fml.relauncher.Side;
import factorization.api.IActOnCraft;
import factorization.beauty.ItemGrossFood;
import factorization.beauty.ItemLeafBomb;
import factorization.ceramics.ItemGlazeBucket;
import factorization.ceramics.ItemSculptingTool;
import factorization.ceramics.TileEntityGreenware;
import factorization.ceramics.TileEntityGreenware.ClayState;
import factorization.charge.ItemAcidBottle;
import factorization.charge.ItemBattery;
import factorization.charge.ItemChargeMeter;
import factorization.charge.TileEntityLeydenJar;
import factorization.colossi.*;
import factorization.darkiron.BlockDarkIronOre;
import factorization.truth.minecraft.ItemDocBook;
import factorization.fzds.DeltaChunk;
import factorization.mechanics.ItemDarkIronChain;
import factorization.oreprocessing.ItemOreProcessing;
import factorization.oreprocessing.ItemOreProcessing.OreType;
import factorization.oreprocessing.TileEntityCrystallizer;
import factorization.oreprocessing.TileEntityGrinder;
import factorization.oreprocessing.TileEntitySlagFurnace;
import factorization.redstone.BlockMatcher;
import factorization.servo.*;
import factorization.shared.*;
import factorization.shared.Core.TabType;
import factorization.sockets.ItemSocketPart;
import factorization.twistedblock.ItemTwistedBlock;
import factorization.util.CraftUtil;
import factorization.util.DataUtil;
import factorization.util.FzUtil;
import factorization.util.ItemUtil;
import factorization.utiligoo.ItemGoo;
import factorization.weird.*;
import factorization.weird.poster.ItemSpawnPoster;
import factorization.wrath.BlockLightAir;
import factorization.wrath.TileEntityWrathLamp;
import net.minecraft.block.Block;
import net.minecraft.block.BlockNetherrack;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
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
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.RecipeSorter;
import net.minecraftforge.oredict.RecipeSorter.Category;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;

public class Registry {
    public ItemFactorizationBlock item_factorization;
    public ItemBlockResource item_resource;
    public BlockFactorization factory_block, factory_block_barrel;
    public BlockFactorization factory_rendering_block;
    public BlockRenderHelper blockRender = null, serverTraceHelper = null, clientTraceHelper = null;
    public BlockLightAir lightair_block;
    public BlockResource resource_block;
    public Block dark_iron_ore;
    public Block fractured_bedrock_block;
    public Block blasted_bedrock_block;
    public Block colossal_block;
    public Block gargantuan_block;
    public Block mantlerock_block;
    public BlockMatcher matcher_block;

    public ItemStack servorail_item;
    public ItemStack empty_socket_item, socket_lacerator, socket_robot_hand, socket_shifter;
    public ItemStack hinge; //, anchor;
    
    public ItemStack stamper_item, packager_item,
            daybarrel_item_hidden,
            lamp_item, air_item,
            slagfurnace_item, battery_item_hidden, leydenjar_item, leydenjar_item_full, heater_item, steamturbine_item, solarboiler_item, caliometric_burner_item,
            mirror_item_hidden,
            leadwire_item, mixer_item, crystallizer_item,
            greenware_item,
            rocket_engine_item_hidden,
            parasieve_item,
            compression_crafter_item,
            sap_generator_item, anthro_generator_item,
            shaft_generator_item, steam_to_shaft, wooden_shaft, bibliogen, wind_mill, water_wheel;
    public ItemStack silver_ore_item, silver_block_item, lead_block_item,
            dark_iron_block_item;
    public ItemStack is_factory, is_lamp, is_lightair;
    public ItemPocketTable pocket_table;
    public ItemCraftingComponent diamond_shard;
    public ItemCraftingComponent silver_ingot, lead_ingot;
    public ItemCraftingComponent dark_iron;
    public ItemAcidBottle acid;
    public ItemCraftingComponent insulated_coil, motor, fan, diamond_cutting_head, corkscrew;
    public ItemStack sulfuric_acid, aqua_regia;
    public ItemChargeMeter charge_meter;
    public ItemBlockProxy mirror;
    public ItemBattery battery;
    public ItemOreProcessing ore_dirty_gravel, ore_clean_gravel, ore_reduced, ore_crystal;
    public ItemCraftingComponent sludge;
    public ItemSculptingTool sculpt_tool;
    public ItemGlazeBucket glaze_bucket;
    public ItemStack empty_glaze_bucket;
    public ItemStack base_common, glaze_base_mimicry;
    public ItemCraftingComponent logicMatrix, logicMatrixIdentifier, logicMatrixController;
    public ItemMatrixProgrammer logicMatrixProgrammer;
    public Fluid steamFluid;
    public ItemCraftingComponent nether_powder, rocket_fuel;
    public ItemBlockProxy rocket_engine;
    public ItemServoMotor servo_placer;
    public ItemServoRailWidget servo_widget_instruction, servo_widget_decor;
    public ItemStack dark_iron_sprocket, servo_motor;
    public ItemCraftingComponent giant_scissors;
    public ItemDayBarrel daybarrel;
    @Deprecated
    public ItemSocketPart socket_part;
    public ItemCraftingComponent instruction_plate;
    public ItemCommenter servo_rail_comment_editor;
    public ItemDocBook docbook;
    public ItemGoo utiligoo;
    public ItemColossusGuide colossusGuide;
    public ItemTwistedBlock twistedBlock;
    public ItemDarkIronChain darkIronChain;
    public ItemCraftingComponent chainLink, shortChain;
    public ItemSpawnPoster spawnPoster;
    public ItemMinecartDayBarrel barrelCart;
    public ItemGrossFood sap, entheas;
    public ItemLeafBomb leafBomb;
    public BlockBlast blastBlock;

    public Material materialMachine = new Material(MapColor.ironColor);
    public Material materialBarrel = new Material(MapColor.woodColor) {{
        setAdventureModeExempt();
    }};

    WorldgenManager worldgenManager;

    public static HashMap<String, Item> nameCleanup = new HashMap<String, Item>();
    
    static void registerItem(Item item) {
        String find = Matcher.quoteReplacement("item.factorization:");
        String unlocalizedName = item.getUnlocalizedName();
        String useName = unlocalizedName.replaceFirst(find, "");
        GameRegistry.registerItem(item, useName, Core.modId);
        nameCleanup.put("factorization:" + unlocalizedName, item);
    }
    
    public void makeBlocks() {
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            //Theoretically, not necessary. I bet BUKKIT would flip its shit tho.
            blockRender = new BlockRenderHelper();
            factory_rendering_block = new BlockFactorization(materialMachine);
        }
        serverTraceHelper = new BlockRenderHelper();
        clientTraceHelper = new BlockRenderHelper();
        factory_block = new BlockFactorization(materialMachine);
        factory_block_barrel = new BlockFactorization(materialBarrel);
        lightair_block = new BlockLightAir();
        resource_block = new BlockResource();
        dark_iron_ore = new BlockDarkIronOre().setBlockName("factorization:darkIronOre").setBlockTextureName("stone").setCreativeTab(Core.tabFactorization).setHardness(3.0F).setResistance(5.0F);
        fractured_bedrock_block = new FracturedBedrock();
        blasted_bedrock_block = new BlastedBedrock();
        if (DeltaChunk.enabled()) {
            colossal_block = new ColossalBlock();
        }
        blastBlock = new BlockBlast();
        gargantuan_block = new GargantuanBlock().setBlockName("factorization:gargantuanBrick").setCreativeTab(Core.tabFactorization);
        mantlerock_block = new BlockNetherrack().setBlockName("factorization:mantlerock").setBlockTextureName("factorization:mantlerock").setHardness(1.25F).setResistance(7.0F).setStepSound(Block.soundTypeStone);
        matcher_block = new BlockMatcher();
        
        GameRegistry.registerBlock(factory_block, ItemFactorizationBlock.class, "FzBlock");
        GameRegistry.registerBlock(factory_block_barrel, ItemFactorizationBlock.class, "FzBlockBarrel");
        GameRegistry.registerBlock(lightair_block, "Lightair");
        GameRegistry.registerBlock(resource_block, ItemBlockResource.class, "ResourceBlock");
        GameRegistry.registerBlock(dark_iron_ore, "DarkIronOre");
        GameRegistry.registerBlock(fractured_bedrock_block, "FracturedBedrock");
        GameRegistry.registerBlock(blasted_bedrock_block, "BlastedBedrock");
        GameRegistry.registerBlock(gargantuan_block, ItemGargantuanBlock.class, "GargantuanBlock");
        GameRegistry.registerBlock(mantlerock_block, "MantleRock");
        GameRegistry.registerBlock(matcher_block, "BlockMatcher");
        if (DeltaChunk.enabled()) {
            GameRegistry.registerBlock(colossal_block, ColossalBlockItem.class, "ColossalBlock");
            GameRegistry.registerTileEntity(TileEntityColossalHeart.class, "fz_colossal_heart");
            GameRegistry.registerBlock(blastBlock, "BlastBlock");
        }
        
        
        is_factory = new ItemStack(factory_block);
        is_lightair = new ItemStack(lightair_block);
        
        
        Core.tab(factory_block, Core.TabType.BLOCKS);
        Core.tab(resource_block, TabType.BLOCKS);
        
        worldgenManager = new WorldgenManager();
    }
    
    public void registerDerpyAliases() {
        String[][] aliases = new String[][] {
                {"factorization:tile.null", "factorization:FZ factory"},
                {"factorization:tile.factorization.ResourceBlock", "factorization:FZ resource"},
                {"factorization:tile.lightair", "factorization:tile.lightair"},
                {"factorization:tile.factorization:darkIronOre", "factorization:FZ dark iron ore"},
                {"factorization:tile.bedrock", "factorization:FZ fractured bedrock"}
        };
        for (String[] pair : aliases) {
            String proper = pair[0];
            String derpy = pair[1];
            try {
                GameRegistry.addSubstitutionAlias(derpy, Type.BLOCK, proper);
            } catch (ExistingSubstitutionException e) {
                e.printStackTrace();
            }
            try {
                GameRegistry.addSubstitutionAlias(derpy, Type.ITEM, proper);
            } catch (ExistingSubstitutionException e) {
                e.printStackTrace();
            }
            // Not totally awesome to ignore them. If someone else is replacing our own old names, then they ought to know what they're doing tho...
        }
    }

    void postMakeItems() {
        HashSet<Item> foundItems = new HashSet<Item>();
        for (Field field : this.getClass().getFields()) {
            Object obj;
            try {
                obj = field.get(this);
            } catch (Throwable e) {
                e.printStackTrace();
                continue;
            }
            if (obj instanceof ItemStack) {
                obj = ((ItemStack) obj).getItem();
            }
            if (obj instanceof Item) {
                foundItems.add((Item) obj);
            }
        }
        
        Block invalid = DataUtil.getBlock((Item) null);
        for (Item it : foundItems) {
            if (DataUtil.getBlock(it) == invalid) {
                it.setTextureName(it.getUnlocalizedName());
                registerItem(it);
            }
        }
    }

    public void makeItems() {
        ore_dirty_gravel = new ItemOreProcessing("gravel");
        ore_clean_gravel = new ItemOreProcessing("clean");
        ore_reduced = new ItemOreProcessing("reduced");
        ore_crystal = new ItemOreProcessing("crystal");
        sludge = new ItemCraftingComponent("sludge");
        OreDictionary.registerOre("sludge", sludge);
        //ItemBlocks
        item_factorization = (ItemFactorizationBlock) Item.getItemFromBlock(factory_block);
        item_resource = (ItemBlockResource) Item.getItemFromBlock(resource_block);

        //BlockFactorization stuff
        servorail_item = FactoryType.SERVORAIL.itemStack();
        empty_socket_item = FactoryType.SOCKET_EMPTY.itemStack();
        parasieve_item = FactoryType.PARASIEVE.itemStack();
        compression_crafter_item = FactoryType.COMPRESSIONCRAFTER.itemStack();
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
        sap_generator_item = FactoryType.SAP_TAP.itemStack();
        anthro_generator_item = FactoryType.ANTHRO_GEN.itemStack();
        shaft_generator_item = FactoryType.SHAFT_GEN.itemStack();
        steam_to_shaft = FactoryType.STEAM_SHAFT.itemStack();
        wooden_shaft = FactoryType.SHAFT.itemStack();
        bibliogen = FactoryType.BIBLIO_GEN.itemStack();
        heater_item = FactoryType.HEATER.itemStack();
        mirror_item_hidden = FactoryType.MIRROR.itemStack();
        leadwire_item = FactoryType.LEADWIRE.itemStack();
        mixer_item = FactoryType.MIXER.itemStack();
        crystallizer_item = FactoryType.CRYSTALLIZER.itemStack();
        greenware_item = FactoryType.CERAMIC.itemStack();
        if (FzConfig.enable_rocketry) {
            rocket_engine_item_hidden = FactoryType.ROCKETENGINE.itemStack();
        }
        if (DeltaChunk.enabled()) {
            hinge = FactoryType.HINGE.itemStack();
            wind_mill = FactoryType.WIND_MILL_GEN.itemStack();
            water_wheel = FactoryType.WATER_WHEEL_GEN.itemStack();
            //anchor = FactoryType.ANCHOR.itemStack();
        }

        //BlockResource stuff
        silver_ore_item = ResourceType.SILVERORE.itemStack("Silver Ore");
        silver_block_item = ResourceType.SILVERBLOCK.itemStack("Block of Silver");
        lead_block_item = ResourceType.LEADBLOCK.itemStack("Block of Lead");
        dark_iron_block_item = ResourceType.DARKIRONBLOCK.itemStack("Block of Dark Iron");


        diamond_shard = new ItemCraftingComponent("diamond_shard");
        dark_iron = new ItemCraftingComponent("dark_iron_ingot");
        
        lead_ingot = new ItemCraftingComponent("lead_ingot");
        silver_ingot = new ItemCraftingComponent("silver_ingot");
        OreDictionary.registerOre("oreSilver", silver_ore_item);
        OreDictionary.registerOre("ingotSilver", new ItemStack(silver_ingot));
        OreDictionary.registerOre("ingotLead", new ItemStack(lead_ingot));
        OreDictionary.registerOre("blockSilver", silver_block_item);
        OreDictionary.registerOre("blockLead", lead_block_item);
        OreDictionary.registerOre("oreFzDarkIron", dark_iron_ore);
        OreDictionary.registerOre("ingotFzDarkIron", dark_iron);
        OreDictionary.registerOre("blockFzDarkIron", dark_iron_block_item);

        
        logicMatrixProgrammer = new ItemMatrixProgrammer();
        logicMatrix = new ItemCraftingComponent("logic_matrix");
        logicMatrixIdentifier = new ItemCraftingComponent("logic_matrix_identifier");
        logicMatrixController = new ItemCraftingComponent("logic_matrix_controller");

        //Electricity
        acid = new ItemAcidBottle();
        sulfuric_acid = new ItemStack(acid, 1);
        aqua_regia = new ItemStack(acid, 1, 1);
        OreDictionary.registerOre("sulfuricAcid", sulfuric_acid);
        OreDictionary.registerOre("bottleSulfuricAcid", sulfuric_acid);
        OreDictionary.registerOre("aquaRegia", aqua_regia);
        insulated_coil = new ItemCraftingComponent("insulated_coil");
        motor = new ItemCraftingComponent("motor");
        giant_scissors = new ItemCraftingComponent("socket/scissors");
        fan = new ItemCraftingComponent("fan");
        corkscrew = new ItemCraftingComponent("corkscrew");
        diamond_cutting_head = new ItemCraftingComponent("diamond_cutting_head");
        charge_meter = new ItemChargeMeter();
        mirror = new ItemBlockProxy(mirror_item_hidden, "mirror", TabType.CHARGE);
        battery = new ItemBattery();
        leydenjar_item_full = ItemStack.copyItemStack(leydenjar_item);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("storage", TileEntityLeydenJar.max_storage);
        leydenjar_item_full.setTagCompound(tag);
        
        //ceramics
        sculpt_tool = new ItemSculptingTool();
        glaze_bucket = new ItemGlazeBucket();
        empty_glaze_bucket = new ItemStack(glaze_bucket, 1, 100);
        spawnPoster = new ItemSpawnPoster();

        //Misc
        pocket_table = new ItemPocketTable();
        steamFluid = new Fluid("steam").setDensity(-500).setGaseous(true).setViscosity(100).setUnlocalizedName("factorization:fluid/steam").setTemperature(273 + 110);
        FluidRegistry.registerFluid(steamFluid);
        
        //Rocketry
        nether_powder = new ItemCraftingComponent("nether_powder");
        if (FzConfig.enable_rocketry && DeltaChunk.enabled()) {
            rocket_fuel = new ItemCraftingComponent("rocket/rocket_fuel");
            rocket_engine = new ItemBlockProxy(rocket_engine_item_hidden, "rocket/rocket_engine", TabType.ROCKETRY);
            rocket_engine.setMaxStackSize(1);
        }
        
        //Servos
        servo_placer = new ItemServoMotor();
        servo_widget_decor = new ItemServoRailWidget("servo/decorator");
        servo_widget_instruction = new ItemServoRailWidget("servo/component");
        servo_widget_decor.setMaxStackSize(16);
        servo_widget_instruction.setMaxStackSize(1);
        dark_iron_sprocket = new ItemStack(new ItemCraftingComponent("servo/sprocket"));
        servo_motor = new ItemStack(new ItemCraftingComponent("servo/servo_motor"));
        socket_part = new ItemSocketPart("socket/", TabType.SERVOS);
        instruction_plate = new ItemCraftingComponent("servo/instruction_plate", TabType.SERVOS);
        instruction_plate.setSpriteNumber(0);
        instruction_plate.setMaxStackSize(16);
        servo_rail_comment_editor = new ItemCommenter("servo/commenter");
        
        socket_lacerator = FactoryType.SOCKET_LACERATOR.asSocketItem();
        socket_robot_hand = FactoryType.SOCKET_ROBOTHAND.asSocketItem();
        socket_shifter = FactoryType.SOCKET_SHIFTER.asSocketItem();
        
        //Barrels
        daybarrel = new ItemDayBarrel("daybarrel");
        barrelCart = new ItemMinecartDayBarrel();

        docbook = new ItemDocBook("docbook", TabType.TOOLS);
        utiligoo = new ItemGoo("utiligoo", TabType.TOOLS);
        if (DeltaChunk.enabled()) {
            colossusGuide = new ItemColossusGuide("colossusGuide", TabType.TOOLS);
            twistedBlock = new ItemTwistedBlock();
            darkIronChain = new ItemDarkIronChain("darkIronChain", TabType.TOOLS);
            chainLink = new ItemCraftingComponent("chainLink", TabType.MATERIALS);
            shortChain = new ItemCraftingComponent("shortChain", TabType.MATERIALS);
        }

        // Beautiful generators
        sap = new ItemGrossFood("sap", Core.TabType.MATERIALS, false);
        entheas = new ItemGrossFood("entheas", Core.TabType.MATERIALS, true);
        leafBomb = new ItemLeafBomb();

        postMakeItems();
    }

    private boolean checkInput(ItemStack res, Object... params) {
        if (res == null) {
            return true;
        }
        for (Object obj : params) {
            if (obj == null) {
                return true;
            }
        }
        return false;
    }

    public void vanillaRecipe(ItemStack res, Object... params) {
        if (checkInput(res, params)) return;
        GameRegistry.addRecipe(res, params);
    }

    public void vanillaShapelessRecipe(ItemStack res, Object... params) {
        if (checkInput(res, params)) return;
        GameRegistry.addShapelessRecipe(res, params);
    }

    public void oreRecipe(ItemStack res, Object... params) {
        if (checkInput(res, params)) return;
        convertOreItems(params);
        GameRegistry.addRecipe(new ShapedOreRecipe(res, params));
    }

    public void shapelessOreRecipe(ItemStack res, Object... params) {
        if (checkInput(res, params)) return;
        convertOreItems(params);
        GameRegistry.addRecipe(new ShapelessOreRecipe(res, params));
    }

    void batteryRecipe(ItemStack res, Object... params) {
        for (int damage : new int[] { 1, 2 }) {
            ArrayList<Object> items = new ArrayList<Object>(params.length);
            for (Object p : params) {
                if (p == battery) {
                    p = new ItemStack(battery, 1, damage);
                }
                items.add(p);
            }
            oreRecipe(res, items.toArray());
        }
    }
    
    private void convertOreItems(Object[] params) {
        for (int i = 0; i < params.length; i++) {
            if (params[i] == Blocks.cobblestone) {
                params[i] = "cobblestone";
            } else if (params[i] == Blocks.stone) {
                params[i] = "stone";
            } else if (params[i] == Items.stick) {
                params[i] = "stickWood";
            } else if (params[i] == dark_iron) {
                params[i] = "ingotFzDarkIron";
            }
        }
    }

    public void makeRecipes() {
        vanillaRecipe(ItemUtil.nameItemStack(new ItemStack(Blocks.double_stone_slab), "Double Half Slab"),
                "-",
                "-",
                '-', new ItemStack(Blocks.stone_slab));
        vanillaRecipe(ItemUtil.nameItemStack(new ItemStack(Blocks.double_stone_slab, 2, 8), "Flat Stone"),
                "##",
                "##",
                '#', new ItemStack(Blocks.stone_slab));
        vanillaRecipe(ItemUtil.nameItemStack(new ItemStack(Blocks.double_stone_slab, 2, 9), "Flat Sandstone"),
                "#",
                "#",
                '#', new ItemStack(Blocks.sandstone, 1, 2));
        vanillaShapelessRecipe(ItemUtil.nameItemStack(new ItemStack(Blocks.dirt, 4, 1), "Dry Dirt"),
                Blocks.dirt,
                Blocks.dirt,
                Blocks.dirt,
                Blocks.dirt);
        
        shapelessOreRecipe(new ItemStack(dark_iron, 9), dark_iron_block_item);
        oreRecipe(dark_iron_block_item,
                "III",
                "III",
                "III",
                'I', dark_iron);
        
        // Pocket Crafting Table (pocket table)
        oreRecipe(new ItemStack(pocket_table),
                " #",
                "| ",
                '#', Blocks.crafting_table,
                '|', Items.stick);

        oreRecipe(new ItemStack(logicMatrixIdentifier),
                "MiX",
                'M', logicMatrix,
                'i', Items.quartz,
                'X', logicMatrixProgrammer);
        GameRegistry.addSmelting(logicMatrixIdentifier, new ItemStack(logicMatrix), 0);
        oreRecipe(new ItemStack(logicMatrixController),
                "MiX",
                'M', logicMatrix,
                'i', "ingotSilver",
                'X', logicMatrixProgrammer);
        GameRegistry.addSmelting(logicMatrixController, new ItemStack(logicMatrix), 0);
        oreRecipe(new ItemStack(logicMatrixProgrammer),
                "MiX",
                'M', logicMatrix,
                'i', dark_iron,
                'X', logicMatrixProgrammer);
        oreRecipe(new ItemStack(logicMatrixProgrammer),
                "DSI",
                " #>",
                "BSI",
                'D', Items.record_13,
                'B', Items.record_11,
                'S', diamond_shard,
                'I', dark_iron,
                '#', logicMatrix,
                '>', Items.comparator);
        TileEntitySlagFurnace.SlagRecipes.register(new ItemStack(logicMatrixProgrammer), 2F/3F, new ItemStack(dark_iron), 0.85F, new ItemStack(logicMatrix));
        
        TileEntityCrystallizer.addRecipe(new ItemStack(Blocks.redstone_block), new ItemStack(logicMatrix), 1, Core.registry.aqua_regia);

        //Resources
        oreRecipe(new ItemStack(lead_ingot, 9), "#", '#', lead_block_item);
        oreRecipe(new ItemStack(silver_ingot, 9), "#", '#', silver_block_item);
        oreRecipe(lead_block_item, "###", "###", "###", '#', "ingotLead");
        oreRecipe(silver_block_item, "###", "###", "###", '#', "ingotSilver");
        FurnaceRecipes.smelting().func_151394_a(new ItemStack(resource_block, 1, ResourceType.SILVERORE.md), new ItemStack(silver_ingot), 0.3F);
        FurnaceRecipes.smelting().func_151394_a(new ItemStack(dark_iron_ore), new ItemStack(dark_iron), 0.5F);

        //ceramics
        oreRecipe(new ItemStack(sculpt_tool),
                " c",
                "/ ",
                'c', Items.clay_ball,
                '/', Items.stick);
        ItemSculptingTool.addModeChangeRecipes();
        oreRecipe(empty_glaze_bucket.copy(),
                "_ _",
                "# #",
                "#_#",
                '_', "slabWood",
                '#', "plankWood");
        
        base_common = glaze_bucket.makeCraftingGlaze("base_common");
        glaze_base_mimicry = glaze_bucket.makeCraftingGlaze("base_mimicry");
        
        glaze_bucket.addGlaze(glaze_base_mimicry);
        
        ItemStack lapis = new ItemStack(Items.dye, 1, 4);
        
        shapelessOreRecipe(base_common, empty_glaze_bucket.copy(), Items.water_bucket, new ItemStack(Blocks.sand, 1, OreDictionary.WILDCARD_VALUE), Items.clay_ball);
        shapelessOreRecipe(glaze_base_mimicry, base_common, Items.redstone, Items.slime_ball, lapis);
        
        ItemStack waterFeature = glaze_bucket.makeMimicingGlaze(Blocks.water, 0, -1);
        ItemStack lavaFeature = glaze_bucket.makeMimicingGlaze(Blocks.lava, 0, -1);
        shapelessOreRecipe(waterFeature, base_common, Items.water_bucket);
        shapelessOreRecipe(lavaFeature, base_common, Items.lava_bucket);
        
        Core.registry.glaze_bucket.doneMakingStandardGlazes();
        
        //Sculpture combiniation recipe
        IRecipe sculptureMergeRecipe = new IRecipe() {
            ArrayList<ItemStack> merge(InventoryCrafting inv) {
                ArrayList<ItemStack> match = null;
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
                    if (ItemUtil.similar(Core.registry.greenware_item, is)) {
                        if (match == null) match = new ArrayList<ItemStack>(2);
                        match.add(is);
                    } else {
                        return null;
                    }
                }
                if (match == null || match.size() != 2) {
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
                    rep.loadFromStack(is);
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
                    rep.loadFromStack(is);
                    target.parts.addAll(rep.parts);
                }
                return target.getItem();
            }
        };
        GameRegistry.addRecipe(sculptureMergeRecipe);
        
        IRecipe mimicryGlazeRecipe = new IRecipe() {
            @Override
            public boolean matches(InventoryCrafting inventorycrafting, World world) {
                int mimic_items = 0;
                int other_items = 0;
                for (int i = 0; i < inventorycrafting.getSizeInventory(); i++) {
                    ItemStack is = inventorycrafting.getStackInSlot(i);
                    if (is == null) {
                        continue;
                    }
                    if (ItemUtil.couldMerge(glaze_base_mimicry, is)) {
                        mimic_items++;
                    } else {
                        if (!(is.getItem() instanceof ItemBlock)) continue;
                        int d = is.getItemDamage();
                        if (d < 0 || d > 16) {
                            return false;
                        }
                        Block b = Block.getBlockFromItem(is.getItem());
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
                    if (ItemUtil.couldMerge(glaze_base_mimicry, is)) {
                        bucket_slot = i;
                        continue;
                    }
                    int d = is.getItemDamage();
                    if (d < 0 || d > 16) {
                        continue;
                    }
                    if (!(is.getItem() instanceof ItemBlock)) continue;
                    Block b = Block.getBlockFromItem(is.getItem());
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
                return glaze_bucket.makeMimicingGlaze(Block.getBlockFromItem(is.getItem()), is.getItemDamage(), side);
            }
        };
        GameRegistry.addRecipe(mimicryGlazeRecipe);
        RecipeSorter.register("factorization:sculptureMerge", sculptureMergeRecipe.getClass(), Category.SHAPELESS, "");
        RecipeSorter.register("factorization:mimicryGlaze", mimicryGlazeRecipe.getClass(), Category.SHAPELESS, "");
        oreRecipe(new ItemStack(spawnPoster),
                "0",
                "-",
                "0",
                '-', Items.paper,
                '0', "slimeball");

        // Barrel
        // Add the recipes for vanilla woods.

        final ItemStack oakLog = new ItemStack(Blocks.log);
        final ItemStack oakPlank = new ItemStack(Blocks.wooden_slab);
        final ItemStack oakBarrel = TileEntityDayBarrel.makeBarrel(TileEntityDayBarrel.Type.NORMAL, oakLog, oakPlank);
        for (int i = 0; i < 4; i++) {
            ItemStack log = new ItemStack(Blocks.log, 1, i);
            ItemStack slab = new ItemStack(Blocks.wooden_slab, 1, i);
            TileEntityDayBarrel.makeRecipe(log, slab);
        }
        for (int i = 0; i < 2; i++) {
            ItemStack log = new ItemStack(Blocks.log2, 1, i);
            ItemStack slab = new ItemStack(Blocks.wooden_slab, 1, 4 + i);
            TileEntityDayBarrel.makeRecipe(log, slab);
        }

        IRecipe barrel_cart_recipe = new IRecipe() {
            @Override
            public boolean matches(InventoryCrafting inv, World world) {
                boolean found_barrel = false, found_cart = false;
                for (int i = 0; i < inv.getSizeInventory(); i++) {
                    ItemStack is = inv.getStackInSlot(i);
                    if (is == null) continue;
                    if (is.getItem() == Core.registry.daybarrel) {
                        if (TileEntityDayBarrel.getUpgrade(is) == TileEntityDayBarrel.Type.SILKY) {
                            return false;
                        }
                        found_barrel = true;
                    } else if (is.getItem() == Items.minecart) {
                        found_cart = true;
                    } else {
                        return false;
                    }
                }
                return found_barrel && found_cart;
            }

            @Override
            public ItemStack getCraftingResult(InventoryCrafting inv) {
                for (int i = 0; i < inv.getSizeInventory(); i++) {
                    ItemStack is = inv.getStackInSlot(i);
                    if (is == null) continue;
                    if (is.getItem() != Core.registry.daybarrel) continue;
                    if (TileEntityDayBarrel.getUpgrade(is) == TileEntityDayBarrel.Type.SILKY) {
                        return null;
                    }
                    return Core.registry.barrelCart.makeBarrel(is);
                }
                return new ItemStack(barrelCart);
            }

            @Override
            public int getRecipeSize() {
                return 2;
            }

            @Override
            public ItemStack getRecipeOutput() {
                return new ItemStack(barrelCart);
            }
        };
        GameRegistry.addRecipe(barrel_cart_recipe);
        RecipeSorter.register("factorization:barrel_cart", barrel_cart_recipe.getClass(), Category.SHAPELESS, "");

        BarrelUpgradeRecipes.addUpgradeRecipes();
        
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
        oreRecipe(lamp_item,
                "ISI",
                "GWG",
                "ISI",
                'I', dark_iron,
                'S', "ingotSilver",
                'G', Blocks.glass_pane,
                'W', diamond_shard);

        //Slag furnace
        oreRecipe(slagfurnace_item,
                "CFC",
                "C C",
                "CFC",
                'C', Blocks.cobblestone,
                'F', Blocks.furnace);
        
        //most ores give 0.4F stone, but redstone is dense.
        //mining redstone normally gives 4 to 6 ore. 5.8F should get you a slightly better yield.
        TileEntitySlagFurnace.SlagRecipes.register(Blocks.redstone_ore, 5.8F, Items.redstone, 0.2F, Blocks.stone);
        
        
        oreRecipe(greenware_item,
                "c",
                "-",
                'c', Items.clay_ball,
                '-', "slabWood");

        //Electricity

        
        shapelessOreRecipe(sulfuric_acid, Items.gunpowder, Items.gunpowder, Items.coal, Items.potionitem);
        shapelessOreRecipe(sulfuric_acid, "dustSulfur", Items.coal, Items.potionitem);
        shapelessOreRecipe(aqua_regia, sulfuric_acid, nether_powder, Items.fire_charge);
        shapelessOreRecipe(aqua_regia, sulfuric_acid, Items.blaze_powder, Items.fire_charge); //I'd kind of like this to be a recipe for a different — but compatible — aqua regia. 
        oreRecipe(new ItemStack(fan),
                "I I",
                " - ",
                "I I",
                'I', Items.iron_ingot,
                '-', Blocks.heavy_weighted_pressure_plate);
        oreRecipe(new ItemStack(corkscrew),
                " |-",
                "-| ",
                " |-",
                '|', Items.iron_ingot,
                '-', Blocks.heavy_weighted_pressure_plate);
        oreRecipe(new ItemStack(giant_scissors),
                "I I",
                " S ",
                "P P",
                'P', Blocks.sticky_piston,
                'S', Items.shears,
                'I', Items.iron_sword);
        oreRecipe(solarboiler_item,
                "I#I",
                "I I",
                "III",
                'I', Items.iron_ingot,
                '#', Blocks.iron_bars);
        oreRecipe(caliometric_burner_item,
                "BPB",
                "BAB",
                "BLB",
                'B', Items.bone,
                'P', Blocks.sticky_piston,
                'A', sulfuric_acid,
                'L', Items.leather);
        if (DeltaChunk.enabled()) {
            oreRecipe(water_wheel,
                    "#I#",
                    "===",
                    "#I#",
                    '#', "plankWood",
                    'I', dark_iron_block_item,
                    '=', wooden_shaft);
            oreRecipe(wind_mill,
                    "#=#",
                    "I=I",
                    "#=#",
                    '#', "plankWood",
                    'I', dark_iron_block_item,
                    '=', wooden_shaft);
        }
        oreRecipe(new ItemStack(charge_meter),
                "WSW",
                "W|W",
                "LIL",
                'W', "plankWood",
                'S', Items.sign,
                '|', Items.stick,
                'L', "ingotLead",
                'I', Items.iron_ingot);
        oreRecipe(new ItemStack(battery, 1, 2),
                "ILI",
                "LAL",
                "ILI",
                'I', Items.iron_ingot,
                'L', "ingotLead",
                'A', acid);
        oreRecipe(leydenjar_item,
                "#G#",
                "#L#",
                "LLL",
                '#', Blocks.glass_pane,
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
                'I', Items.iron_ingot);
        if (FzConfig.enable_solar_steam) { //NOTE: This'll probably cause a bug should we use mirrors for other things
            oreRecipe(new ItemStack(mirror),
                    "SSS",
                    "S#S",
                    "SSS",
                    'S', "ingotSilver",
                    '#', Blocks.glass_pane);
        }
        ItemStack with_8 = leadwire_item.copy();
        with_8.stackSize = 8;
        oreRecipe(with_8,
                "LLL",
                'L', "ingotLead");
        oreRecipe(new ItemStack(diamond_cutting_head),
                "SSS",
                "S-S",
                "SSS",
                'S', diamond_shard,
                '-', Blocks.heavy_weighted_pressure_plate);
        
        //Values based on Fortune I
        TileEntityGrinder.addRecipe(new ItemStack(Blocks.coal_ore), new ItemStack(Items.coal), 1.5F);
        TileEntityGrinder.addRecipe("oreRedstone", new ItemStack(Items.redstone), 5F);
        TileEntityGrinder.addRecipe("oreDiamond", new ItemStack(Items.diamond), 1.25F);
        TileEntityGrinder.addRecipe("oreEmerald", new ItemStack(Items.emerald), 1.25F);
        TileEntityGrinder.addRecipe(new ItemStack(Blocks.quartz_ore), new ItemStack(Items.quartz), 2.5F /* It should actually be 1.25, but I feel like being EXTRA generous here. */);
        TileEntityGrinder.addRecipe("oreLapis", new ItemStack(Items.dye, 1, 4), 8.5F);
        
        //VANILLA RECIPES
        //These are based on going through the Search tab in the creative menu
        //When we turn the Grinder into a Lacerator, anything not specified here will be broken in the usual manner.
        TileEntityGrinder.addRecipe(Blocks.stone, new ItemStack(Blocks.cobblestone), 1);
        TileEntityGrinder.addRecipe(Blocks.cobblestone, new ItemStack(Blocks.gravel), 1);
        TileEntityGrinder.addRecipe("treeSapling", new ItemStack(Items.stick), 1.25F);
        TileEntityGrinder.addRecipe(Blocks.gravel, new ItemStack(Blocks.sand), 1);
        TileEntityGrinder.addRecipe("treeLeaves", new ItemStack(Items.stick), 0.5F);
        TileEntityGrinder.addRecipe(Blocks.glass, new ItemStack(Blocks.sand), 0.1F);
        TileEntityGrinder.addRecipe(Blocks.web, new ItemStack(Items.string), 0.25F);
        TileEntityGrinder.addRecipe(Blocks.brick_block, new ItemStack(Items.brick), 3.5F);
        TileEntityGrinder.addRecipe(Blocks.mossy_cobblestone, new ItemStack(Blocks.gravel), 1);
        //Now's a fine time to add the mob spawner
        TileEntityGrinder.addRecipe(Blocks.mob_spawner, new ItemStack(Blocks.iron_bars), 2.5F);
        //No stairs, no slabs.
        //Chest, but we don't want to support wood transmutes.
        TileEntityGrinder.addRecipe(Blocks.furnace, new ItemStack(Blocks.cobblestone), 7F);
        TileEntityGrinder.addRecipe(Blocks.lit_furnace, new ItemStack(Blocks.stone), 7F);
        TileEntityGrinder.addRecipe(Blocks.ladder, new ItemStack(Items.stick), 1.5F);
        TileEntityGrinder.addRecipe(Blocks.snow_layer, new ItemStack(Items.snowball), 0.5F);
        TileEntityGrinder.addRecipe(Blocks.snow, new ItemStack(Items.snowball), 4F);
        TileEntityGrinder.addRecipe(Blocks.clay, new ItemStack(Items.clay_ball), 4F);
        TileEntityGrinder.addRecipe(Blocks.fence, new ItemStack(Items.stick), 2.5F);
        //Netherrack dust is handled elsewhere!
        TileEntityGrinder.addRecipe(Blocks.glowstone, new ItemStack(Items.glowstone_dust), 4F);
        TileEntityGrinder.addRecipe(Blocks.trapdoor, new ItemStack(Items.stick), 3.5F);
        TileEntityGrinder.addRecipe(Blocks.stonebrick, new ItemStack(Blocks.cobblestone), 0.75F);
        TileEntityGrinder.addRecipe(Blocks.glass_pane, new ItemStack(Blocks.sand), 0.1F/16F);
        TileEntityGrinder.addRecipe(Blocks.melon_block, new ItemStack(Items.melon), 7.75F);
        TileEntityGrinder.addRecipe(Blocks.fence_gate, new ItemStack(Items.stick), 2.5F);
        TileEntityGrinder.addRecipe(Blocks.nether_brick, new ItemStack(Items.netherbrick), 3.5F);
        TileEntityGrinder.addRecipe(Blocks.nether_brick_fence, new ItemStack(Items.netherbrick), 2.5F);
        //TODO: Asbestos from endstone
        TileEntityGrinder.addRecipe(Blocks.redstone_lamp, new ItemStack(Items.glowstone_dust), 4F);
        //Don't want to be responsible for some netherstar exploit involving a beacon, so no beacon.
        //Walls have weird geometry
        TileEntityGrinder.addRecipe(Blocks.hay_block, new ItemStack(Items.wheat), 8.25F);
        
        //So, that's blocks. How about items?
        TileEntityGrinder.addRecipe(Items.book, new ItemStack(Items.leather), 0.75F); //Naughty.
        TileEntityGrinder.addRecipe(Items.enchanted_book, new ItemStack(Items.leather), 0.9F);
        //NOTE: We're going to have to do something tricksy for the lacerator...
        //These go to Blocks.skull, but the item damagevalue != block metadata.
        TileEntityGrinder.addRecipe(new ItemStack(Items.skull, 1, 0 /* skele */), new ItemStack(Items.dye, 1, 15 /* bonemeal */), 6.5F);
        TileEntityGrinder.addRecipe(new ItemStack(Items.skull, 1, 2 /* zombie */), new ItemStack(Items.rotten_flesh), 2.5F);
        TileEntityGrinder.addRecipe(new ItemStack(Items.skull, 1, 3 /* player */), new ItemStack(Items.rotten_flesh), 3.5F);
        TileEntityGrinder.addRecipe(new ItemStack(Items.skull, 1, 4 /* creeper */), new ItemStack(Items.gunpowder), 1.5F);
        
        
        
        oreRecipe(mixer_item,
                " X ",
                " M ",
                "LUL",
                'X', fan,
                'M', motor,
                'L', "ingotLead",
                'U', Items.cauldron);
        FurnaceRecipes.smelting().func_151394_a(new ItemStack(sludge), new ItemStack(Items.clay_ball), 0.1F);
        oreRecipe(crystallizer_item,
                "-",
                "S",
                "U",
                '-', Items.stick,
                'S', Items.string,
                'U', Items.cauldron);
        ItemStack lime = new ItemStack(Items.dye, 1, 10);
        TileEntityCrystallizer.addRecipe(lime, new ItemStack(Items.slime_ball), 1, new ItemStack(Items.milk_bucket));
        
        //Rocketry
        TileEntityGrinder.addRecipe(new ItemStack(Blocks.netherrack), new ItemStack(nether_powder, 1), 1);
        if (FzConfig.enable_rocketry) {
            shapelessOreRecipe(new ItemStack(rocket_fuel, 9),
                    nether_powder, nether_powder, nether_powder,
                    nether_powder, Items.fire_charge, nether_powder,
                    nether_powder, nether_powder, nether_powder);
            oreRecipe(new ItemStack(rocket_engine),
                    "#F#",
                    "#I#",
                    "I I",
                    '#', Blocks.iron_block,
                    'F', rocket_fuel,
                    'I', Items.iron_ingot);
        }
        
        //Servos
        makeServoRecipes();
        oreRecipe(empty_socket_item,
                "#",
                "-",
                "#",
                '#', Blocks.iron_bars,
                '-', "slabWood");
        oreRecipe(FactoryType.SOCKET_SHIFTER.asSocketItem(),
                "V",
                "@",
                "D",
                'V', Blocks.hopper,
                '@', logicMatrixController,
                'D', Blocks.dropper);
        oreRecipe(socket_robot_hand,
                "+*P",
                "+@+",
                "P*+",
                '+', servorail_item,
                '*', dark_iron_sprocket,
                '@', logicMatrixController,
                'P', Blocks.piston);
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
        GameRegistry.addSmelting(servo_widget_instruction, new ItemStack(instruction_plate), 0);
        oreRecipe(new ItemStack(docbook),
                "B~>",
                'B', Items.book,
                '~', new ItemStack(Items.dye, 1, 0), // The book says "ink sac", so you'll have to use an actual ink sac.
                '>', logicMatrixProgrammer);
        ItemStack tons_of_bonemeal = new ItemStack(Items.dye, 12 /* stacksize */, 15 /* damage value for bonemeal */);
        oreRecipe(tons_of_bonemeal,
                "MSH",
                "nXn",
                'M', Blocks.melon_block,
                'S', Blocks.sand,
                'H', Blocks.hay_block,
                'n', Items.nether_wart,
                'X', Items.bone);
        shapelessOreRecipe(new ItemStack(utiligoo, 32), // NORELEASE: Temporary recipe!
                Blocks.red_mushroom,
                Items.diamond,
                Items.diamond,
                logicMatrixProgrammer);
        vanillaRecipe(new ItemStack(gargantuan_block),
                "#",
                "#",
                "F",
                '#', Blocks.stone,
                'F', Blocks.fire);
        vanillaRecipe(new ItemStack(mantlerock_block, 3),
                "S#",
                "#S",
                'S', Blocks.stone,
                '#', Blocks.netherrack);
        if (DeltaChunk.enabled()) {
            oreRecipe(new ItemStack(twistedBlock),
                    "*",
                    "#",
                    '*', dark_iron_sprocket,
                    '#', this.dark_iron_block_item);
            oreRecipe(hinge.copy(),
                    "|##",
                    "|  ",
                    "|##",
                    '|', dark_iron,
                    '#', Blocks.iron_block);
            oreRecipe(new ItemStack(chainLink, 15),
                    "DD ",
                    "D L",
                    "DD ",
                    'D', dark_iron,
                    'L', "ingotLead");
            oreRecipe(new ItemStack(shortChain),
                    "LLL",
                    "LLL",
                    "LLL",
                    'L', chainLink);
            oreRecipe(new ItemStack(darkIronChain),
                    "L  ",
                    "LLL",
                    "  L",
                    'L', shortChain);
        }

        // Beautiful generators
        oreRecipe(sap_generator_item,
                "LYL",
                "W+W",
                "WUW",
                'L', "treeLeaves",
                'Y', Blocks.hopper,
                '+', Blocks.fence,
                'W', "logWood",
                'U', Items.bucket);
        int red = 14;
        oreRecipe(anthro_generator_item,
                "s#s",
                "#i#",
                "s-s",
                '#', Items.paper,
                's', "stickWood",
                'C', new ItemStack(Blocks.carpet, 1, red),
                'W', new ItemStack(Blocks.wool, 1, red),
                '-', Blocks.wooden_pressure_plate,
                'i', Items.glowstone_dust);
        oreRecipe(steam_to_shaft,
                " I ",
                "-B-",
                " I ",
                '-', Blocks.heavy_weighted_pressure_plate,
                'I', dark_iron,
                'B', "blockIron");
        ItemStack shaft8 = wooden_shaft.copy();
        shaft8.stackSize = 8;
        oreRecipe(shaft8,
                "LIL",
                "LIL",
                "LIL",
                'L', "logWood",
                'I', dark_iron);
        oreRecipe(shaft_generator_item,
                "IDI",
                "CMC",
                "LIL",
                'I', "ingotIron",
                'D', dark_iron,
                'C', insulated_coil,
                'M', motor,
                'L', lead_ingot);
        oreRecipe(bibliogen,
                "I",
                "O",
                "Y",
                'I', "crystallineDark Iron",
                'O', "slimeball",
                'Y', Blocks.enchanting_table);

        oreRecipe(new ItemStack(matcher_block),
                "#-#",
                "#@#",
                "#-#",
                '#', "cobblestone",
                '-', "paneGlass",
                '@', logicMatrixIdentifier);
        oreRecipe(new ItemStack(blastBlock),
                "###",
                "###",
                "###",
                '#', Items.gunpowder);
        shapelessOreRecipe(new ItemStack(Items.gunpowder, 9), blastBlock);

        if (Core.enable_test_content) {
            TestContent.add();
        }
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
                'q', Items.quartz,
                'r', Items.redstone,
                'S', dark_iron_sprocket,
                'C', insulated_coil,
                'I', Items.iron_ingot,
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
                '#', Blocks.iron_bars,
                'I', Items.iron_ingot,
                'm', logicMatrixIdentifier,
                'v', Blocks.dropper);
    }

    public void setToolEffectiveness() {
        BlockClass.DarkIron.harvest("pickaxe", 2);
        BlockClass.Barrel.harvest("axe", 1);
        BlockClass.Machine.harvest("pickaxe", 1);
        BlockClass.MachineLightable.harvest("pickaxe", 1);
        BlockClass.MachineDynamicLightable.harvest("pickaxe", 1);
        BlockClass.Socket.harvest("axe", 1);
        BlockClass.Socket.harvest("pickaxe", 1);
        resource_block.setHarvestLevel("pickaxe", 2);
        dark_iron_ore.setHarvestLevel("pickaxe", 2);
    }
    
    @SubscribeEvent
    public void tick(ServerTickEvent event) {
        if (event.phase == Phase.START) {
            TileEntityWrathLamp.handleAirUpdates();
        } else {
            worldgenManager.tickRetrogenQueue();
        }
    }

    @SubscribeEvent
    public boolean onItemPickup(EntityItemPickupEvent event) {
        //TODO: Extractify?
        Core.proxy.pokePocketCrafting();
        return true;
    }

    @SubscribeEvent
    public void onCrafting(PlayerEvent.ItemCraftedEvent event) {
        //TODO: Extractify
        EntityPlayer player = event.player;
        ItemStack stack = event.crafting;
        IInventory craftMatrix = event.craftMatrix;
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

    public void sendIMC() {
        //Registers our recipe handlers to a list in NEIPlugins.
        //Format: "Factorization@<Recipe Name>@<outputId that used to view all recipes>"
        for (String msg : new String[] {
                "factorization crystallizer recipes@fz.crystallizing",
                //"factorization grinder recipes@fz.grinding",
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
        leafBomb.addRecipes();

        barrelCart.setMaxStackSize(Items.chest_minecart.getItemStackLimit(new ItemStack(Items.chest_minecart))); // Duplicate changes Railcraft might make
        ArrayList<ItemStack> theLogs = new ArrayList<ItemStack>();
        for (ItemStack is : OreDictionary.getOres("logWood")) {
            Block log = Block.getBlockFromItem(is.getItem());
            if (log == null || log == Blocks.log || log == Blocks.log2) {
                continue;
            }
            if (!ItemUtil.isWildcard(is, false)) {
                theLogs.add(is);
                continue;
            }
            List<ItemStack> discovered_plank_types = new ArrayList<ItemStack>();
            for (int md = 0; md < 16; md++) {
                ItemStack ilog = new ItemStack(log, 1, md);
                List<ItemStack> planks = FzUtil.copyWithoutNull(CraftUtil.craft1x1(null, true, ilog.copy()));
                if (planks.size() == 1) {
                    ItemStack plank = planks.get(0);
                    if (discovered_plank_types.contains(plank)) continue;
                    theLogs.add(ilog);
                    discovered_plank_types.add(plank);
                }
            }
        }
        for (ItemStack log : theLogs) {
            log = log.copy();
            List<ItemStack> planks = FzUtil.copyWithoutNull(CraftUtil.craft1x1(null, true, log.copy()));
            if (planks.size() != 1 || !CraftUtil.craft_succeeded) {
                continue;
            }
            ItemStack plank = planks.get(0).copy();
            plank.stackSize = 1;
            List<ItemStack> slabs = FzUtil.copyWithoutNull(CraftUtil.craft3x3(null, true, true,
                    plank.copy(), plank.copy(), plank.copy(),
                    null, null, null,
                    null, null, null));
            ItemStack slab;
            String odType;
            if (slabs.size() != 1 || !CraftUtil.craft_succeeded) {
                slab = plank; // can't convert to slabs; strange wood
                odType = "plankWood";
            } else {
                slab = slabs.get(0);
                odType = "slabWood";
            }
            // Some modwoods have planks, but no slabs, and their planks convert to vanilla slabs.
            // In this case we're going to want to use the plank.
            // But if the plank is also vanilla, then keep the vanilla slab!
            if (Block.getBlockFromItem(slab.getItem()) == Blocks.wooden_slab) {
                if (Block.getBlockFromItem(plank.getItem()) != Blocks.planks /* the new-in-1.7 planks are packed in the same ID */) {
                    slab = plank;
                }
            }
            TileEntityDayBarrel.makeRecipe(log, slab.copy());
        }
    }
    
}
