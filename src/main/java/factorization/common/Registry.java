package factorization.common;

import factorization.api.IActOnCraft;
import factorization.artifact.BlockForge;
import factorization.artifact.ItemBrokenArtifact;
import factorization.artifact.ItemPotency;
import factorization.beauty.*;
import factorization.charge.BlockFurnaceHeater;
import factorization.charge.ItemAcidBottle;
import factorization.charge.ItemChargeMeter;
import factorization.charge.enet.BlockLeydenJar;
import factorization.charge.enet.ChargeEnetSubsys;
import factorization.charge.enet.TileEntityLeydenJar;
import factorization.colossi.*;
import factorization.crafting.BlockCompressionCrafter;
import factorization.crafting.ItemFakeBlock;
import factorization.darkiron.BlockDarkIronOre;
import factorization.flat.ItemFlat;
import factorization.fzds.DeltaChunk;
import factorization.fzds.HammerEnabled;
import factorization.mechanics.ItemDarkIronChain;
import factorization.oreprocessing.TileEntityGrinder;
import factorization.redstone.BlockMatcher;
import factorization.redstone.BlockParasieve;
import factorization.servo.*;
import factorization.servo.stepper.ItemStepperEngine;
import factorization.shared.*;
import factorization.shared.Core.TabType;
import factorization.sockets.BlockSocket;
import factorization.sockets.ItemSocketPart;
import factorization.truth.minecraft.ItemDocBook;
import factorization.truth.minecraft.ItemManSandwich;
import factorization.twistedblock.ItemTwistedBlock;
import factorization.util.*;
import factorization.utiligoo.ItemGoo;
import factorization.weird.ItemPocketTable;
import factorization.weird.barrel.*;
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
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.WeightedRandomFishable;
import net.minecraft.world.World;
import net.minecraftforge.common.FishingHooks;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.event.FMLInterModComms;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;
import net.minecraftforge.fml.common.registry.ExistingSubstitutionException;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry.Type;
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
    @Deprecated // Each TE has its own block now. This block continues to exist for converting blocks & items.
    public BlockFactorization legacy_factory_block;
    public BlockBarrel factory_block_barrel;
    public BlockParasieve parasieve_block;
    public SimpleFzBlock caliometric_burner_block;
    public SimpleFzBlock creative_energy;
    public BlockFurnaceHeater furnace_heater;
    public SimpleFzBlock whirligig;
    public BlockLightAir lightair_block;
    public BlockResource resource_block;
    public Block dark_iron_ore;
    public Block fractured_bedrock_block;
    public Block blasted_bedrock_block;
    public Block colossal_block;
    public Block gargantuan_block;
    public Block mantlerock_block;
    public BlockMatcher matcher_block;
    public BlockForge artifact_forge;
    public BlockCompressionCrafter compression_crafter;
    public BlockSocket socket;
    public SimpleFzBlock hall_of_legends;
    public SimpleFzBlock lamp;
    public SimpleFzBlock leyden_jar;
    public SimpleFzBlock anthrogen;
    public BlockGeyser geyser;
    public BlockOreExtruder extruder;
    public BlockShaftGen shaftGen;
    public BlockWaterwheel waterwheel;
    public BlockWindmill windmill;
    public BlockShaft shaft;
    public SimpleFzBlock bibliogen;
    public SimpleFzBlock lightningrod;

    public ItemStack servorail_item;
    public ItemStack empty_socket_item;
    public ItemStack hinge; //, anchor;
    
    public ItemStack
            lamp_item,
            leydenjar_item, leydenjar_item_full, heater_item, solarboiler_item, caliometric_burner_item,
            mirror_item_hidden,
            parasieve_item,
            compression_crafter_item,
            sap_generator_item, anthro_generator_item,
            shaft_generator_item, steam_to_shaft, wooden_shaft, bibliogen_item, wind_mill, water_wheel;
    public ItemStack dark_iron_block_item, copper_block_item;
    public ItemStack is_factory, is_lightair;
    public ItemPocketTable pocket_table;
    public ItemCraftingComponent diamond_shard;
    public ItemCraftingComponent copper_ingot, dark_iron_ingot;
    public ItemStack copper_ore_item;
    public ItemAcidBottle acid;
    public ItemCraftingComponent insulated_coil, motor, fan, diamond_cutting_head, corkscrew;
    public ItemStack sulfuric_acid, aqua_regia;
    public ItemChargeMeter charge_meter;
    public ItemBlockProxy mirror;
    public ItemCraftingComponent sludge;
    public ItemCraftingComponent logicMatrix, logicMatrixIdentifier, logicMatrixController;
    public ItemMatrixProgrammer logicMatrixProgrammer;
    public Fluid steamFluid;
    public ItemCraftingComponent nether_powder, rocket_fuel;
    public ItemBlockProxy rocket_engine;
    public ItemServoMotor servo_placer;
    public ItemServoMotor stepper_placer;
    public ItemServoRailWidget servo_widget_instruction, servo_widget_decor;
    public ItemStack dark_iron_sprocket, servo_motor;
    public ItemCraftingComponent giant_scissors;
    public ItemDayBarrel daybarrel;
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
    public ItemPotency item_potency;
    public ItemStack legendarium;
    public ItemBrokenArtifact brokenTool;
    public ItemManSandwich manSandwich;
    public ItemFakeBlock waterBlockItem, lavaBlockItem;
    public ItemFlat wirePlacer;
    public ItemSocketPart socket_shifter_item, socket_robot_hand;

    public Material materialMachine = new Material(MapColor.ironColor);

    WorldgenManager worldgenManager;

    public static HashMap<String, Item> nameCleanup = new HashMap<String, Item>();

    static void registerItem(Item item) {
        String find = Matcher.quoteReplacement("item.factorization:");
        String unlocalizedName = item.getUnlocalizedName();
        String useName = unlocalizedName.replaceFirst(find, "");
        GameRegistry.registerItem(item, useName);
        nameCleanup.put("factorization:" + unlocalizedName, item);
    }

    public void makeBlocks() {
        ResourceLocation steamy = new ResourceLocation("factorization:textures/blocks/steam");
        steamFluid = new Fluid("steam", steamy, steamy).setDensity(-500).setGaseous(true).setViscosity(100).setUnlocalizedName("factorization:fluid/steam").setTemperature(273 + 110);
        FluidRegistry.registerFluid(steamFluid);
        resource_block = new BlockResource(); // This needs to be at the top for things that refer to it.
        lightair_block = new BlockLightAir();
        legacy_factory_block = new BlockFactorization(materialMachine);
        factory_block_barrel = new BlockBarrel();
        parasieve_block = new BlockParasieve();
        parasieve_block.setUnlocalizedName("factorization:factoryBlock.PARASIEVE");
        caliometric_burner_block = new SimpleFzBlock(materialMachine, FactoryType.CALIOMETRIC_BURNER);
        creative_energy = new SimpleFzBlock(Material.barrier, FactoryType.CREATIVE_CHARGE);
        furnace_heater = new BlockFurnaceHeater();
        whirligig = new SimpleFzBlock(materialMachine, FactoryType.STEAM_SHAFT);
        hall_of_legends = new SimpleFzBlock(Material.iron, FactoryType.LEGENDARIUM);
        lamp = new SimpleFzBlockCutout(Material.iron, FactoryType.LAMP);
        leyden_jar = new BlockLeydenJar();
        anthrogen = new BlockAnthroGen(Material.wood, FactoryType.ANTHRO_GEN);
        geyser = new BlockGeyser();
        geyser.setUnlocalizedName("factorization:geyser");
        extruder = new BlockOreExtruder();
        extruder.setUnlocalizedName("factorization:extruder");
        compression_crafter = new BlockCompressionCrafter();
        socket = new BlockSocket();
        shaftGen = new BlockShaftGen();
        waterwheel = new BlockWaterwheel();
        windmill = new BlockWindmill();
        shaft = new BlockShaft();
        bibliogen = new SimpleFzBlock(Material.rock, FactoryType.BIBLIO_GEN);
        lightningrod = new SimpleFzBlock(Material.iron, FactoryType.LIGHTNING_ROD);
        for (BlockClass bc : BlockClass.values()) {
            if (bc == BlockClass.Barrel) {
                bc.block = factory_block_barrel;
            } else {
                bc.block = legacy_factory_block;
            }
        }
        dark_iron_ore = new BlockDarkIronOre()
                .setUnlocalizedName("factorization:darkIronOre")
                .setHardness(3.0F).setResistance(5.0F);
        fractured_bedrock_block = new FracturedBedrock();
        blasted_bedrock_block = new BlastedBedrock();
        if (DeltaChunk.enabled()) {
            colossal_block = new ColossalBlock();
        }
        blastBlock = new BlockBlast();
        gargantuan_block = new GargantuanBlock()
                .setUnlocalizedName("factorization:gargantuanBrick");
        mantlerock_block = new BlockNetherrack()
                .setUnlocalizedName("factorization:mantlerock")
                .setHardness(1.25F).setResistance(7.0F)
                .setStepSound(Block.soundTypeStone);
        matcher_block = new BlockMatcher();
        artifact_forge = new BlockForge();

        GameRegistry.registerBlock(legacy_factory_block, ItemFactorizationBlock.class, "FzBlock");
        GameRegistry.registerBlock(factory_block_barrel, ItemDayBarrel.class, "FzBlockBarrel");
        GameRegistry.registerBlock(lightair_block, "Lightair");
        GameRegistry.registerBlock(resource_block, ItemBlockResource.class, "ResourceBlock");
        GameRegistry.registerBlock(dark_iron_ore, "DarkIronOre");
        GameRegistry.registerBlock(fractured_bedrock_block, "FracturedBedrock");
        GameRegistry.registerBlock(blasted_bedrock_block, "BlastedBedrock");
        GameRegistry.registerBlock(gargantuan_block, ItemGargantuanBlock.class, "GargantuanBlock");
        GameRegistry.registerBlock(mantlerock_block, "MantleRock");
        GameRegistry.registerBlock(matcher_block, "BlockMatcher");
        GameRegistry.registerBlock(artifact_forge, "ArtifactForge");
        GameRegistry.registerBlock(blastBlock, "BlastBlock");
        GameRegistry.registerBlock(parasieve_block, ItemFactorizationBlock.class, "Parasieve");
        GameRegistry.registerBlock(caliometric_burner_block, ItemFactorizationBlock.class, "CaliometricBurner");
        GameRegistry.registerBlock(creative_energy, ItemFactorizationBlock.class, "CreativeEnergy");
        GameRegistry.registerBlock(furnace_heater, ItemFactorizationBlock.class, "FurnaceHeater");
        GameRegistry.registerBlock(whirligig, ItemFactorizationBlock.class, "Whirligig");
        GameRegistry.registerBlock(hall_of_legends, ItemFactorizationBlock.class, "HallOfLegends");
        GameRegistry.registerBlock(lamp, ItemFactorizationBlock.class, "Lamp");
        GameRegistry.registerBlock(leyden_jar, ItemFactorizationBlock.class, "LeydenJar");
        GameRegistry.registerBlock(anthrogen, ItemFactorizationBlock.class, "AnthropicGenerator");
        GameRegistry.registerBlock(geyser, "Geyser");
        GameRegistry.registerBlock(extruder, "OreExtruder");
        GameRegistry.registerBlock(compression_crafter, ItemFactorizationBlock.class, "CompressionCrafter");
        GameRegistry.registerBlock(socket, ItemFactorizationBlock.class, "Socket");
        GameRegistry.registerBlock(shaftGen, ItemFactorizationBlock.class, "ShaftGen");
        GameRegistry.registerBlock(waterwheel, ItemFactorizationBlock.class, "WaterWheelBase");
        GameRegistry.registerBlock(windmill, ItemFactorizationBlock.class, "WindMillBase");
        GameRegistry.registerBlock(shaft, ItemShaft.class, "Shaft");
        GameRegistry.registerBlock(bibliogen, ItemFactorizationBlock.class, "Bibliogen");
        GameRegistry.registerBlock(lightningrod, ItemFactorizationBlock.class, "LightningRod");
        if (DeltaChunk.enabled()) {
            GameRegistry.registerBlock(colossal_block, ColossalBlockItem.class, "ColossalBlock");
            GameRegistry.registerTileEntity(TileEntityColossalHeart.class, "fz_colossal_heart");
        }


        is_factory = new ItemStack(legacy_factory_block);
        is_lightair = new ItemStack(lightair_block);


        Core.tab(legacy_factory_block, TabType.BLOCKS);
        Core.tab(factory_block_barrel, TabType.TOOLS);
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
            if (obj instanceof BlockFactorization) {
                BlockFactorization block = (BlockFactorization) obj;
                TileEntity te = block.createNewTileEntity(null, 0);
                if (te instanceof TileEntityFzNull) {
                    if (block != this.legacy_factory_block) {
                        NORELEASE.println("Block didn't give a TE: " + block);
                    }
                }
                for (FactoryType ft : FactoryType.values()) {
                    if (ft.getFactoryTypeClass() == te.getClass()) {
                        if (ft.block != null && ft.block != block) throw new RuntimeException("Block already assigned to FactoryType!");
                        ft.block = block;
                    }
                }
            }
            if (obj instanceof Block) {
                Block block = (Block) obj;
                block.setCreativeTab(Core.tabFactorization);
            }
        }

        Block invalid = DataUtil.getBlock((Item) null);
        for (Item it : foundItems) {
            if (DataUtil.getBlock(it) == invalid) {
                registerItem(it);
            }
        }
        Core.proxy.registerISensitiveMeshes(foundItems);
        for (FactoryType ft : FactoryType.values()) {
            if (!ft.disabled && ft.block != null) {
                NORELEASE.println("FactoryType doesn't have an associated block: ", ft);
            }
        }
    }

    public void makeItems() {
        sludge = new ItemCraftingComponent("sludge");
        NORELEASE.fixme("Delete these guys");
        //ItemBlocks
        item_factorization = (ItemFactorizationBlock) Item.getItemFromBlock(legacy_factory_block);
        item_resource = (ItemBlockResource) Item.getItemFromBlock(resource_block);

        //BlockFactorization stuff
        servorail_item = FactoryType.SERVORAIL.itemStack();
        empty_socket_item = new ItemStack(socket);
        parasieve_item = FactoryType.PARASIEVE.itemStack();
        compression_crafter_item = FactoryType.COMPRESSIONCRAFTER.itemStack();
        lamp_item = FactoryType.LAMP.itemStack();
        leydenjar_item = FactoryType.LEYDENJAR.itemStack();
        solarboiler_item = FactoryType.SOLARBOILER.itemStack();
        caliometric_burner_item = FactoryType.CALIOMETRIC_BURNER.itemStack();
        sap_generator_item = FactoryType.SAP_TAP.itemStack();
        anthro_generator_item = FactoryType.ANTHRO_GEN.itemStack();
        shaft_generator_item = FactoryType.SHAFT_GEN.itemStack();
        steam_to_shaft = FactoryType.STEAM_SHAFT.itemStack();
        wooden_shaft = FactoryType.SHAFT.itemStack();
        bibliogen_item = FactoryType.BIBLIO_GEN.itemStack();
        heater_item = FactoryType.HEATER.itemStack();
        mirror_item_hidden = FactoryType.MIRROR.itemStack();
        if (DeltaChunk.enabled()) {
            hinge = FactoryType.HINGE.itemStack();
            wind_mill = FactoryType.WIND_MILL_GEN.itemStack();
            water_wheel = FactoryType.WATER_WHEEL_GEN.itemStack();
            //anchor = FactoryType.ANCHOR.itemStack();
        }
        legendarium = FactoryType.LEGENDARIUM.itemStack();

        //BlockResource stuff
        copper_block_item = ResourceType.COPPER_BLOCK.itemStack();
        dark_iron_block_item = ResourceType.DARK_IRON_BLOCK.itemStack();
        copper_ore_item = ResourceType.COPPER_ORE.itemStack();


        diamond_shard = new ItemCraftingComponent("diamond_shard");
        dark_iron_ingot = new ItemCraftingComponent("dark_iron_ingot");
        copper_ingot = new ItemCraftingComponent("copper_ingot");


        logicMatrixProgrammer = new ItemMatrixProgrammer();
        logicMatrix = new ItemCraftingComponent("logic_matrix");
        logicMatrixIdentifier = new ItemCraftingComponent("logic_matrix_identifier");
        logicMatrixController = new ItemCraftingComponent("logic_matrix_controller");

        //Electricity
        acid = new ItemAcidBottle();
        sulfuric_acid = new ItemStack(acid, 1);
        aqua_regia = new ItemStack(acid, 1, 1);
        insulated_coil = new ItemCraftingComponent("insulated_coil");
        motor = new ItemCraftingComponent("motor");
        giant_scissors = new ItemCraftingComponent("socket/scissors");
        fan = new ItemCraftingComponent("fan");
        corkscrew = new ItemCraftingComponent("corkscrew");
        diamond_cutting_head = new ItemCraftingComponent("diamond_cutting_head", TabType.SERVOS, false);
        charge_meter = new ItemChargeMeter();
        mirror = new ItemBlockProxy(mirror_item_hidden, "mirror", TabType.CHARGE);
        leydenjar_item_full = ItemStack.copyItemStack(leydenjar_item);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("storage", TileEntityLeydenJar.MAX_STORAGE);
        leydenjar_item_full.setTagCompound(tag);

        //ceramics

        //Misc
        spawnPoster = new ItemSpawnPoster();
        pocket_table = new ItemPocketTable();

        //Rocketry
        nether_powder = new ItemCraftingComponent("nether_powder");

        //Servos
        servo_placer = new ItemServoMotor("servo");
        if (HammerEnabled.ENABLED && Core.dev_environ) {
            stepper_placer = new ItemStepperEngine("stepper");
        }
        servo_widget_decor = new ItemServoRailWidget("servo/decorator");
        servo_widget_instruction = new ItemServoRailWidget("servo/component");
        servo_widget_decor.setMaxStackSize(16);
        servo_widget_instruction.setMaxStackSize(1);
        dark_iron_sprocket = new ItemStack(new ItemCraftingComponent("servo/sprocket"));
        servo_motor = new ItemStack(new ItemCraftingComponent("servo/servo_motor"));
        instruction_plate = new ItemCraftingComponent("servo/instruction_plate", TabType.SERVOS);
        instruction_plate.setMaxStackSize(16);
        servo_rail_comment_editor = new ItemCommenter("servo/commenter");
        socket_robot_hand = new ItemSocketPart("socket/robotHand", TabType.SERVOS);
        socket_shifter_item = new ItemSocketPart("socket/itemShifter", TabType.SERVOS);

        //Barrels
        daybarrel = (ItemDayBarrel) DataUtil.getItem(factory_block_barrel);
        barrelCart = new ItemMinecartDayBarrel();

        docbook = new ItemDocBook("docbook", TabType.TOOLS);
        manSandwich = new ItemManSandwich(10, 10, "factorization:mansandwich");
        utiligoo = new ItemGoo("utiligoo", TabType.TOOLS);
        if (HammerEnabled.ENABLED) {
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

        item_potency = new ItemPotency();
        brokenTool = new ItemBrokenArtifact();

        waterBlockItem = new ItemFakeBlock("waterTile", TabType.MATERIALS, Blocks.flowing_water);
        lavaBlockItem = new ItemFakeBlock("lavaTile", TabType.MATERIALS, Blocks.flowing_lava);
        wirePlacer = new ItemFlat(ChargeEnetSubsys.instance.wireLeader, TabType.CHARGE);
        postMakeItems();
        registerOres();
    }

    public void registerOres() {
        OreDictionary.registerOre("sulfuricAcid", sulfuric_acid);
        OreDictionary.registerOre("bottleSulfuricAcid", sulfuric_acid);
        OreDictionary.registerOre("aquaRegia", aqua_regia);
        OreDictionary.registerOre("ingotCopper", copper_ingot);
        OreDictionary.registerOre("blockCopper", copper_block_item);
        OreDictionary.registerOre("oreCopper", copper_ore_item);
        OreDictionary.registerOre("oreFzDarkIron", dark_iron_ore);
        OreDictionary.registerOre("ingotFzDarkIron", dark_iron_ingot);
        OreDictionary.registerOre("ingotDankIron", dark_iron_ingot);
        OreDictionary.registerOre("blockFzDarkIron", dark_iron_block_item);
        OreDictionary.registerOre("blockDankIron", dark_iron_block_item);
        OreDictionary.registerOre("blockWater", waterBlockItem);
        OreDictionary.registerOre("blockLava", lavaBlockItem);
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

    private void convertOreItems(Object[] params) {
        for (int i = 0; i < params.length; i++) {
            if (params[i].equals("ingotSilver")) {
                throw new IllegalArgumentException("No silver!");
            }
            if (params[i].equals("ingotLead")) {
                throw new IllegalArgumentException("No lead!");
            }
            NORELEASE.fixme("Remove above checks");
            if (params[i] == Blocks.cobblestone) {
                params[i] = "cobblestone";
            } else if (params[i] == Blocks.stone) {
                params[i] = "stone";
            } else if (params[i] == Items.stick) {
                params[i] = "stickWood";
            } else if (params[i] == dark_iron_ingot) {
                params[i] = "ingotFzDarkIron";
            } else if (params[i] == copper_ingot) {
                params[i] = "ingotCopper";
            }
        }
    }

    public void makeRecipes() {
        /*
        // 1.8 has broken these poor guys :(
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
                '#', new ItemStack(Blocks.sandstone, 1, 2)); */

        shapelessOreRecipe(new ItemStack(dark_iron_ingot, 9), dark_iron_block_item);
        oreRecipe(dark_iron_block_item,
                "III",
                "III",
                "III",
                'I', dark_iron_ingot);
        
        // Pocket Crafting Table (pocket table)
        oreRecipe(new ItemStack(pocket_table),
                " #",
                "| ",
                '#', Blocks.crafting_table,
                '|', Items.stick);

        oreRecipe(new ItemStack(logicMatrixIdentifier),
                "MiX",
                'M', logicMatrix,
                'i', "gemQuartz",
                'X', logicMatrixProgrammer);
        GameRegistry.addSmelting(logicMatrixIdentifier, new ItemStack(logicMatrix), 0);
        oreRecipe(new ItemStack(logicMatrix),
                "#L#",
                "WRW",
                "#L#",
                '#', Blocks.glass,
                'L', "blockLava",
                'W', "blockWater",
                'R', Blocks.redstone_block);
        oreRecipe(new ItemStack(Blocks.hardened_clay, 3),
                "WWW",
                "CCC",
                "WWW",
                'W', "blockWater",
                'C', ItemUtil.makeWildcard(Blocks.stained_hardened_clay));
        oreRecipe(new ItemStack(Blocks.sea_lantern),
                "Q~Q",
                "~G~",
                "Q~Q",
                '~', "blockWater",
                'Q', Blocks.quartz_block,
                'G', Blocks.glowstone);
        ItemStack podzol = new ItemStack(Blocks.dirt, 1, 2);
        oreRecipe(new ItemStack(Blocks.dirt, 8),
                "G#G",
                "#P#",
                "G#G",
                'G', Blocks.grass,
                '#', "treeLeaves",
                'P', podzol);
        oreRecipe(podzol.copy(),
                "W",
                "#",
                "D",
                'W', "blockWater",
                '#', "treeLeaves",
                'D', Blocks.dirt);
        oreRecipe(new ItemStack(Items.clay_ball, 4 * 4),
                "~D~",
                "D#D",
                "~D~",
                '~', "blockWater",
                'D', Blocks.dirt,
                '#', Blocks.gravel);
        oreRecipe(new ItemStack(Items.clay_ball, 4 * 5),
                "~H~",
                "HCH",
                "~H~",
                '~', "blockWater",
                'H', Blocks.hardened_clay,
                'C', Blocks.clay);
        oreRecipe(new ItemStack(logicMatrixController),
                "MiX",
                'M', logicMatrix,
                'i', Items.gold_ingot,
                'X', logicMatrixProgrammer);
        GameRegistry.addSmelting(logicMatrixController, new ItemStack(logicMatrix), 0);
        oreRecipe(new ItemStack(logicMatrixProgrammer),
                "MiX",
                'M', logicMatrix,
                'i', dark_iron_ingot,
                'X', logicMatrixProgrammer);
        oreRecipe(new ItemStack(logicMatrixProgrammer),
                "DSI",
                " #>",
                "BSI",
                'D', Items.record_13,
                'B', Items.record_11,
                'S', diamond_shard,
                'I', dark_iron_ingot,
                '#', logicMatrix,
                '>', Items.comparator);

        //Resources
        shapelessOreRecipe(new ItemStack(copper_ingot, 9), copper_block_item);
        oreRecipe(copper_block_item,
                "###",
                "###",
                "###",
                '#', copper_ingot);
        FurnaceRecipes.instance().addSmeltingRecipe(copper_ore_item.copy(), new ItemStack(copper_ingot), 0.3F);
        FurnaceRecipes.instance().addSmeltingRecipe(new ItemStack(dark_iron_ore), new ItemStack(dark_iron_ingot), 0.5F);
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
        for (int i = 0; i < 4; i++) {
            ItemStack log = new ItemStack(Blocks.log, 1, i);
            ItemStack slab = new ItemStack(Blocks.wooden_slab, 1, i);
            makeMaterialsRecipes(log, slab);
        }
        for (int i = 0; i < 2; i++) {
            ItemStack log = new ItemStack(Blocks.log2, 1, i);
            ItemStack slab = new ItemStack(Blocks.wooden_slab, 1, 4 + i);
            makeMaterialsRecipes(log, slab);
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

            @Override
            public ItemStack[] getRemainingItems(InventoryCrafting inv) {
                return CraftUtil.getRemainingItems(inv);
            }
        };
        GameRegistry.addRecipe(barrel_cart_recipe);
        RecipeSorter.register("factorization:barrel_cart", barrel_cart_recipe.getClass(), Category.SHAPELESS, "");
        vanillaShapelessRecipe(new ItemStack(Items.minecart), barrelCart);

        BarrelUpgradeRecipes.addUpgradeRecipes();
        ShaftManipulationRecipe.addManipulationRecipes();

        //Compression Crafter
        oreRecipe(compression_crafter_item,
                "-",
                "C",
                "P",
                '-', Blocks.stone_slab,
                'C', Blocks.crafting_table,
                'P', Blocks.piston);

        // Wrath lamp
        oreRecipe(lamp_item,
                "ISI",
                "GWG",
                "ISI",
                'I', dark_iron_ingot,
                'S', Items.gold_ingot,
                'G', Blocks.glass_pane,
                'W', diamond_shard);

        //Electricity

        oreRecipe(new ItemStack(wirePlacer, 8),
                "---",
                '-', "ingotCopper");
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
                'L', "ingotCopper",
                'I', Items.iron_ingot);
        oreRecipe(leydenjar_item,
                "#G#",
                "#L#",
                "LLL",
                '#', Blocks.glass_pane,
                'G', Blocks.glass,
                'L', "ingotCopper");

        oreRecipe(heater_item,
                "CCC",
                "L L",
                "CCC",
                'C', insulated_coil,
                'L', "ingotCopper");
        oreRecipe(new ItemStack(insulated_coil, 4),
                "LLL",
                "LCL",
                "LLL",
                'L', "ingotCopper",
                'C', Blocks.clay);
        oreRecipe(new ItemStack(motor),
                "CIC",
                "CIC",
                "LBL",
                'C', insulated_coil,
                'B', NORELEASE.just("ingotIron" /* Magnets again? */),
                'L', "ingotCopper",
                'I', Items.iron_ingot);
        if (FzConfig.enable_solar_steam) { //NOTE: This'll probably cause a bug should we use mirrors for other things
            oreRecipe(new ItemStack(mirror),
                    "SSS",
                    "S#S",
                    "SSS",
                    'S', "ingotCopper",
                    '#', Blocks.glass_pane);
        }
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
        TileEntityGrinder.addRecipe("fenceWood", new ItemStack(Items.stick), 2.5F);
        //Netherrack dust is handled elsewhere!
        TileEntityGrinder.addRecipe(Blocks.glowstone, new ItemStack(Items.glowstone_dust), 4F);
        TileEntityGrinder.addRecipe(Blocks.trapdoor, new ItemStack(Items.stick), 3.5F);
        TileEntityGrinder.addRecipe(Blocks.stonebrick, new ItemStack(Blocks.cobblestone), 0.75F);
        TileEntityGrinder.addRecipe(Blocks.glass_pane, new ItemStack(Blocks.sand), 0.1F/16F);
        TileEntityGrinder.addRecipe(Blocks.melon_block, new ItemStack(Items.melon), 7.75F);
        TileEntityGrinder.addRecipe("fenceGateWood", new ItemStack(Items.stick), 2.5F);
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
        
        
        
        FurnaceRecipes.instance().addSmeltingRecipe(new ItemStack(sludge), new ItemStack(Items.clay_ball), 0.1F);

        //Rocketry
        TileEntityGrinder.addRecipe(new ItemStack(Blocks.netherrack), new ItemStack(nether_powder, 1), 1);

        //Servos
        makeServoRecipes();
        oreRecipe(empty_socket_item,
                "#",
                "-",
                "#",
                '#', Blocks.iron_bars,
                '-', "slabWood");
        oreRecipe(new ItemStack(socket_shifter_item),
                "V",
                "@",
                "D",
                'V', Blocks.hopper,
                '@', logicMatrixController,
                'D', Blocks.dropper);
        oreRecipe(new ItemStack(socket_robot_hand),
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
                'I', dark_iron_ingot,
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
        for (Item meat : new Item[] {
                Items.cooked_beef,
                Items.cooked_porkchop,
                Items.cooked_chicken,
                Items.cooked_fish,
                Items.cooked_rabbit,
                Items.cooked_mutton }) {
            oreRecipe(new ItemStack(manSandwich, 1, 0),
                    "BMM",
                    "M#M",
                    "MMB",
                    '#', docbook,
                    'M', meat,
                    'B', Items.bread);
        }
        oreRecipe(new ItemStack(manSandwich, 1, 1),
                "*",
                "/",
                "*",
                '*', Items.blaze_powder,
                '/', new ItemStack(manSandwich));
        FishingHooks.addJunk(new WeightedRandomFishable(new ItemStack(docbook), 10));
        FishingHooks.addTreasure(new WeightedRandomFishable(new ItemStack(manSandwich, 1, 1), 1));
        FishingHooks.addTreasure(new WeightedRandomFishable(new ItemStack(manSandwich), 1));
        ItemStack tons_of_bonemeal = new ItemStack(Items.dye, 12 /* stacksize */, 15 /* damage value for bonemeal */);
        oreRecipe(tons_of_bonemeal,
                "MSH",
                "nXn",
                'M', Blocks.melon_block,
                'S', Blocks.sand,
                'H', Blocks.hay_block,
                'n', Items.nether_wart,
                'X', Items.bone);
        shapelessOreRecipe(new ItemStack(utiligoo, 32), // Temporary recipe! Utiligoo item itself should be temporary.
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
                    '|', dark_iron_ingot,
                    '#', Blocks.iron_block);
            oreRecipe(new ItemStack(chainLink, 15),
                    "DD ",
                    "D L",
                    "DD ",
                    'D', dark_iron_ingot,
                    'L', Items.iron_ingot);
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
                '+', "fenceWood",
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
                'I', dark_iron_ingot,
                'B', "blockIron");
        oreRecipe(shaft_generator_item,
                "IDI",
                "CMC",
                "LIL",
                'I', "ingotIron",
                'D', dark_iron_ingot,
                'C', insulated_coil,
                'M', motor,
                'L', copper_ingot);
        oreRecipe(bibliogen_item,
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
        oreRecipe(new ItemStack(artifact_forge),
                "###",
                " - ",
                "---",
                '#', dark_iron_block_item,
                '-', dark_iron_ingot);
        oreRecipe(legendarium,
                "-*-",
                "###",
                "-#-",
                '#', new ItemStack(Blocks.quartz_block, 1, 1),
                '-', "ingotGold",
                '*', Items.nether_star);
    }
    
    private void makeServoRecipes() {
        ItemStack rails = servorail_item.copy();
        rails.stackSize = 8;
        oreRecipe(rails, "LDL",
                'D', dark_iron_ingot,
                'L', Items.iron_ingot);
        ItemStack two_sprockets = dark_iron_sprocket.copy();
        two_sprockets.stackSize = 2;
        oreRecipe(two_sprockets,
                " D ",
                "D D",
                " D ",
                'D', dark_iron_ingot);
        oreRecipe(servo_motor,
                "qCL",
                "SIB",
                "rCL",
                'q', "gemQuartz",
                'r', Items.redstone,
                'S', dark_iron_sprocket,
                'C', insulated_coil,
                'I', Items.iron_ingot,
                'B', NORELEASE.just("ingotIron" /* magnets? */),
                'L', "ingotCopper");
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
            if (slabs.size() != 1 || !CraftUtil.craft_succeeded) {
                slab = plank; // can't convert to slabs; strange wood
            } else {
                slab = slabs.get(0);
            }
            // Some modwoods have planks, but no slabs, and their planks convert to vanilla slabs.
            // In this case we're going to want to use the plank.
            // But if the plank is also vanilla, then keep the vanilla slab!
            if (Block.getBlockFromItem(slab.getItem()) == Blocks.wooden_slab) {
                if (Block.getBlockFromItem(plank.getItem()) != Blocks.planks /* the new-in-1.7 planks are packed in the same ID */) {
                    slab = plank;
                }
            }
            makeMaterialsRecipes(log, slab.copy());
        }
    }

    public void registerItemVariantNames() {
        NORELEASE.fixme("Move to proxy");
    }

    private void makeMaterialsRecipes(ItemStack log, ItemStack slab) {
        TileEntityDayBarrel.makeRecipe(log, slab);
        TileEntityShaft.makeRecipe(log, slab);
    }

}
