package factorization.common;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;

import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.entity.passive.EntityVillager;
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
import net.minecraft.util.Tuple;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraft.village.MerchantRecipeList;
import net.minecraft.world.World;
import net.minecraftforge.common.ChestGenHooks;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.RecipeSorter;
import net.minecraftforge.oredict.RecipeSorter.Category;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInterModComms;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;
import cpw.mods.fml.common.registry.FMLControlledNamespacedRegistry;
import cpw.mods.fml.common.registry.GameData;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.GameRegistry.Type;
import cpw.mods.fml.common.registry.VillagerRegistry;
import cpw.mods.fml.common.registry.VillagerRegistry.IVillageTradeHandler;
import cpw.mods.fml.relauncher.Side;
import factorization.api.IActOnCraft;
import factorization.ceramics.ItemGlazeBucket;
import factorization.ceramics.ItemSculptingTool;
import factorization.ceramics.TileEntityGreenware;
import factorization.ceramics.TileEntityGreenware.ClayState;
import factorization.charge.ItemAcidBottle;
import factorization.charge.ItemBattery;
import factorization.charge.ItemChargeMeter;
import factorization.charge.TileEntityLeydenJar;
import factorization.darkiron.BlockDarkIronOre;
import factorization.docs.ItemDocBook;
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
import factorization.sockets.ItemSocketPart;
import factorization.utiligoo.ItemGoo;
import factorization.weird.ItemDayBarrel;
import factorization.weird.ItemPocketTable;
import factorization.weird.TileEntityDayBarrel;
import factorization.wrath.BlockLightAir;
import factorization.wrath.TileEntityWrathLamp;

public class Registry {
    public ItemFactorizationBlock item_factorization;
    public ItemBlockResource item_resource;
    public BlockFactorization factory_block, factory_rendering_block;
    public BlockRenderHelper blockRender = null, serverTraceHelper = null, clientTraceHelper = null;
    public BlockLightAir lightair_block;
    public BlockResource resource_block;
    public Block dark_iron_ore;
    public Block fractured_bedrock_block;

    public ItemStack servorail_item;
    public ItemStack empty_socket_item, socket_lacerator, socket_robot_hand, socket_shifter;
    
    public ItemStack stamper_item, packager_item,
            daybarrel_item_hidden,
            lamp_item, air_item,
            slagfurnace_item, battery_item_hidden, leydenjar_item, leydenjar_item_full, heater_item, steamturbine_item, solarboiler_item, caliometric_burner_item,
            mirror_item_hidden,
            leadwire_item, mixer_item, crystallizer_item,
            greenware_item,
            rocket_engine_item_hidden,
            parasieve_item,
            compression_crafter_item;
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

    public Material materialMachine = new Material(MapColor.ironColor);
    
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
            factory_rendering_block = new BlockFactorization();
        }
        serverTraceHelper = new BlockRenderHelper();
        clientTraceHelper = new BlockRenderHelper();
        factory_block = new BlockFactorization();
        lightair_block = new BlockLightAir();
        resource_block = new BlockResource();
        dark_iron_ore = new BlockDarkIronOre().setBlockName("factorization:darkIronOre").setBlockTextureName("stone").setCreativeTab(Core.tabFactorization).setHardness(3.0F).setResistance(5.0F);
        class NotchBlock extends Block { public NotchBlock(Material honestly) { super(honestly); } }
        fractured_bedrock_block = new NotchBlock(Material.rock).setBlockUnbreakable().setResistance(6000000).setBlockName("bedrock").setBlockTextureName("bedrock").setCreativeTab(Core.tabFactorization);
        
        GameRegistry.registerBlock(factory_block, ItemFactorizationBlock.class, "FzBlock");
        GameRegistry.registerBlock(lightair_block, "Lightair");
        GameRegistry.registerBlock(resource_block, ItemBlockResource.class, "ResourceBlock");
        GameRegistry.registerBlock(dark_iron_ore, "DarkIronOre");
        GameRegistry.registerBlock(fractured_bedrock_block, "FracturedBedrock");
        
        
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
        FMLControlledNamespacedRegistry<Block> blockRegistry = GameData.getBlockRegistry();
        for (String[] pair : aliases) {
            String proper = pair[0];
            String derpy = pair[1];
            GameRegistry.addAlias(derpy, proper, Type.BLOCK);
            GameRegistry.addAlias(derpy, proper, Type.ITEM);
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
        
        Block invalid = FzUtil.getBlock((Item) null);
        for (Item it : foundItems) {
            if (FzUtil.getBlock(it) == invalid) {
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
        heater_item = FactoryType.HEATER.itemStack();
        mirror_item_hidden = FactoryType.MIRROR.itemStack();
        leadwire_item = FactoryType.LEADWIRE.itemStack();
        mixer_item = FactoryType.MIXER.itemStack();
        crystallizer_item = FactoryType.CRYSTALLIZER.itemStack();
        greenware_item = FactoryType.CERAMIC.itemStack();
        if (FzConfig.enable_rocketry) {
            rocket_engine_item_hidden = FactoryType.ROCKETENGINE.itemStack();
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
        for (String chestName : new String[] {
                ChestGenHooks.STRONGHOLD_LIBRARY,
                ChestGenHooks.DUNGEON_CHEST,
                //TODO: Nether fortresses? Needs a forge thing tho.
                }) {
            ChestGenHooks dungeon = ChestGenHooks.getInfo(chestName);
            dungeon.addItem(new WeightedRandomChestContent(new ItemStack(logicMatrixProgrammer), 1, 1, 35)); //XXX TODO: Temporary, put these on asteroids.
        }
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

        //Misc
        pocket_table = new ItemPocketTable();
        steamFluid = new Fluid("steam").setDensity(-500).setGaseous(true).setViscosity(100).setUnlocalizedName("factorization:fluid/steam").setTemperature(273 + 110);
        FluidRegistry.registerFluid(steamFluid);
        
        //Rocketry
        nether_powder = new ItemCraftingComponent("nether_powder");
        if (FzConfig.enable_rocketry) {
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
        docbook = new ItemDocBook("docbook", TabType.TOOLS);
        utiligoo = new ItemGoo("utiligoo", TabType.TOOLS);
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
        recipe(FzUtil.nameItemStack(new ItemStack(Blocks.double_stone_slab), "Double Half Slab"),
                "-",
                "-",
                '-', new ItemStack(Blocks.stone_slab));
        recipe(FzUtil.nameItemStack(new ItemStack(Blocks.double_stone_slab, 2, 8), "Flat Stone"),
                "##",
                "##",
                '#', new ItemStack(Blocks.stone_slab));
        recipe(FzUtil.nameItemStack(new ItemStack(Blocks.double_stone_slab, 2, 9), "Flat Sandstone"),
                "#",
                "#",
                '#', new ItemStack(Blocks.sandstone, 1, 2));
        shapelessRecipe(FzUtil.nameItemStack(new ItemStack(Blocks.dirt, 4, 1), "Dry Dirt"),
                Blocks.dirt,
                Blocks.dirt,
                Blocks.dirt,
                Blocks.dirt);
        
        shapelessRecipe(new ItemStack(dark_iron, 9), dark_iron_block_item);
        recipe(dark_iron_block_item,
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

        recipe(new ItemStack(logicMatrixIdentifier),
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
                int min = 2, max = 4;
                Item item = Core.registry.logicMatrixProgrammer;
                float chance = 1;
                
                if (min > 0 && max > 0) {
                    EntityVillager.blacksmithSellingList.put(item, new Tuple(min, max));
                }
                EntityVillager.func_146089_b(recipeList, item, random, chance);
            }
        });
        TileEntitySlagFurnace.SlagRecipes.register(new ItemStack(logicMatrixProgrammer), 2F/3F, new ItemStack(dark_iron), 0.85F, new ItemStack(logicMatrix));
        
        TileEntityCrystallizer.addRecipe(new ItemStack(Blocks.redstone_block), new ItemStack(logicMatrix), 1, Core.registry.aqua_regia);

        //Resources
        recipe(new ItemStack(lead_ingot, 9), "#", '#', lead_block_item);
        recipe(new ItemStack(silver_ingot, 9), "#", '#', silver_block_item);
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
        
        shapelessOreRecipe(base_common, empty_glaze_bucket.copy(), Items.water_bucket, Blocks.sand, Items.clay_ball);
        shapelessOreRecipe(glaze_base_mimicry, base_common, Items.redstone, Items.slime_ball, lapis);
        
        ItemStack waterFeature = glaze_bucket.makeMimicingGlaze(Blocks.water, 0, -1);
        ItemStack lavaFeature = glaze_bucket.makeMimicingGlaze(Blocks.lava, 0, -1);
        shapelessOreRecipe(waterFeature, base_common, Items.water_bucket);
        shapelessOreRecipe(lavaFeature, base_common, Items.lava_bucket);
        
        Core.registry.glaze_bucket.doneMakingStandardGlazes();
        
        //Sculpture combiniation recipe
        IRecipe sculptureMergeRecipe = new IRecipe() {
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
                    if (FzUtil.couldMerge(glaze_base_mimicry, is)) {
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
                    if (FzUtil.couldMerge(glaze_base_mimicry, is)) {
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

        // Barrel
        // Add the recipes for vanilla woods.
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
        recipe(slagfurnace_item,
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

        
        shapelessRecipe(sulfuric_acid, Items.gunpowder, Items.gunpowder, Items.coal, Items.potionitem);
        shapelessOreRecipe(sulfuric_acid, "dustSulfur", Items.coal, Items.potionitem);
        shapelessRecipe(aqua_regia, sulfuric_acid, nether_powder, Items.fire_charge);
        shapelessRecipe(aqua_regia, sulfuric_acid, Items.blaze_powder, Items.fire_charge); //I'd kind of like this to be a recipe for a different — but compatible — aqua regia. 
        recipe(new ItemStack(fan),
                "I I",
                " - ",
                "I I",
                'I', Items.iron_ingot,
                '-', Blocks.heavy_weighted_pressure_plate);
        recipe(new ItemStack(corkscrew),
                " |-",
                "-| ",
                " |-",
                '|', Items.iron_ingot,
                '-', Blocks.heavy_weighted_pressure_plate);
        recipe(new ItemStack(giant_scissors),
                "I I",
                " S ",
                "P P",
                'P', Blocks.sticky_piston,
                'S', Items.shears,
                'I', Items.iron_sword);
        if (FzConfig.enable_solar_steam) {
            recipe(solarboiler_item,
                    "I#I",
                    "I I",
                    "III",
                    'I', Items.iron_ingot,
                    '#', Blocks.iron_bars
                    );
        }
        oreRecipe(steamturbine_item,
                "I#I",
                "GXG",
                "LML",
                'I', Items.iron_ingot,
                '#', Blocks.iron_bars,
                'G', Blocks.glass_pane,
                'X', fan,
                'L', "ingotLead",
                'M', motor );
        oreRecipe(caliometric_burner_item,
                "BPB",
                "BAB",
                "BLB",
                'B', Items.bone,
                'P', Blocks.sticky_piston,
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
                "L#L",
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
        if (FzConfig.enable_solar_steam) { //NOTE: This'll probably cause a bug when we use mirrors for other things
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
        recipe(new ItemStack(diamond_cutting_head),
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
            shapelessRecipe(new ItemStack(rocket_fuel, 9),
                    nether_powder, nether_powder, nether_powder,
                    nether_powder, Items.fire_charge, nether_powder,
                    nether_powder, nether_powder, nether_powder);
            recipe(new ItemStack(rocket_engine),
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
        recipe(new ItemStack(docbook),
                "B~>",
                'B', Items.book,
                '~', new ItemStack(Items.dye, 1, 0), // The book says "ink sac", so you'll have to use an actual ink sac.
                '>', logicMatrixProgrammer);
        ItemStack tons_of_bonemeal = new ItemStack(Items.dye, 12 /* stacksize */, 15 /* damage value for bonemeal */);
        recipe(tons_of_bonemeal,
                "MSH",
                "nXn",
                'M', Blocks.melon_block,
                'S', Blocks.sand,
                'H', Blocks.hay_block,
                'n', Items.nether_wart,
                'X', Items.bone);
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
        ArrayList<ItemStack> theLogs = new ArrayList<ItemStack>();
        for (ItemStack is : OreDictionary.getOres("logWood")) {
            Block log = Block.getBlockFromItem(is.getItem());
            if (log == null || log == Blocks.log || log == Blocks.log2) {
                continue;
            }
            if (is.getItemDamage() == FzUtil.WILDCARD_DAMAGE) {
                for (int md = 0; md < 16; md++) {
                    ItemStack ilog = new ItemStack(log);
                    ilog.setItemDamage(md);
                    ilog.stackSize = 1;
                    theLogs.add(ilog);
                }
                continue;
            }
            theLogs.add(is);
        }
        for (ItemStack log : theLogs) {
            log = log.copy();
            List<ItemStack> planks = FzUtil.copyWithoutNull(FzUtil.craft1x1(null, true, log.copy()));
            if (planks.size() != 1 || !FzUtil.craft_succeeded) {
                continue;
            }
            ItemStack plank = planks.get(0).copy();
            plank.stackSize = 1;
            List<ItemStack> slabs = FzUtil.copyWithoutNull(FzUtil.craft3x3(null, true, true, new ItemStack[] {
                    plank.copy(), plank.copy(), plank.copy(),
                    null, null, null,
                    null, null, null
            }));
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
            if (Block.getBlockFromItem(slab.getItem()) == Blocks.wooden_slab) {
                if (Block.getBlockFromItem(plank.getItem()) != Blocks.planks /* the new-in-1.7 planks are packed in the same ID */) {
                    slab = plank;
                }
            }
            TileEntityDayBarrel.makeRecipe(log, slab.copy());
        }
    }
    
}
