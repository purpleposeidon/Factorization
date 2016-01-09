package factorization.common;

import factorization.api.Coord;
import factorization.artifact.ContainerForge;
import factorization.artifact.GuiArtifactForge;
import factorization.beauty.*;
import factorization.ceramics.TileEntityGreenware;
import factorization.ceramics.TileEntityGreenwareRender;
import factorization.charge.TileEntityHeater;
import factorization.charge.TileEntityHeaterRenderer;
import factorization.charge.TileEntityLeydenJar;
import factorization.charge.TileEntityLeydenJarRender;
import factorization.citizen.EntityCitizen;
import factorization.citizen.RenderCitizen;
import factorization.colossi.ColossusController;
import factorization.colossi.ColossusControllerRenderer;
import factorization.crafting.TileEntityCompressionCrafter;
import factorization.crafting.TileEntityCompressionCrafterRenderer;
import factorization.mechanics.SocketPoweredCrank;
import factorization.mechanics.TileEntityHinge;
import factorization.mechanics.TileEntityHingeRenderer;
import factorization.redstone.GuiParasieve;
import factorization.servo.RenderServoMotor;
import factorization.servo.ServoMotor;
import factorization.servo.stepper.RenderStepperEngine;
import factorization.servo.stepper.StepperEngine;
import factorization.shared.*;
import factorization.sockets.SocketLacerator;
import factorization.sockets.SocketScissors;
import factorization.sockets.TileEntitySocketRenderer;
import factorization.sockets.fanturpeller.SocketFanturpeller;
import factorization.util.DataUtil;
import factorization.util.NORELEASE;
import factorization.util.RenderUtil;
import factorization.utiligoo.GooRenderer;
import factorization.weird.ContainerPocket;
import factorization.weird.GuiPocketTable;
import factorization.weird.barrel.*;
import factorization.weird.poster.EntityPoster;
import factorization.weird.poster.RenderPoster;
import factorization.wrath.TileEntityWrathLamp;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderSnowball;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.IRetexturableModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class FactorizationClientProxy extends FactorizationProxy {
    public FactorizationKeyHandler keyHandler = new FactorizationKeyHandler();

    public FactorizationClientProxy() {
        Core.loadBus(this);
        OBJLoader.instance.addDomain("factorization");
    }

    @Override
    public Profiler getProfiler() {
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
            return Minecraft.getMinecraft().mcProfiler;
        } else {
            return super.getProfiler();
        }
    }

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        if (ID == FactoryType.POCKETCRAFTGUI.gui) {
            return new GuiPocketTable(new ContainerPocket(player));
        }
        Coord at = new Coord(world, x, y, z);
        if (ID == FactoryType.ARTIFACTFORGEGUI.gui) {
            return new GuiArtifactForge(new ContainerForge(at, player));
        }

        TileEntity te = world.getTileEntity(at.toBlockPos());
        if (!(te instanceof TileEntityFactorization)) {
            return null;
        }
        TileEntityFactorization fac = (TileEntityFactorization) te;
        ContainerFactorization cont = new ContainerFactorization(player, fac);
        GuiScreen gui = null;
        if (ID == FactoryType.PARASIEVE.gui) {
            gui = new GuiParasieve(cont);
        }
        if (gui == null) return null;
        cont.addSlotsForGui(fac, player.inventory);
        return gui;
    }

    @Override
    public void pokePocketCrafting() {
        // If the player has a pocket crafting table open, have it update
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.currentScreen instanceof GuiPocketTable) {
            GuiPocketTable gui = (GuiPocketTable) minecraft.currentScreen;
            gui.containerPocket.updateCraft();
        }
    }

    @Override
    public void playSoundFX(String src, float volume, float pitch) {
        ISound sound = new PositionedSoundRecord(new ResourceLocation(src), volume, pitch, 0, 0, 0);
        Minecraft.getMinecraft().getSoundHandler().playSound(sound);
    }

    @Override
    public void addScheduledClientTask(Runnable runnable) {
        Minecraft.getMinecraft().addScheduledTask(runnable);
    }

    @Override
    public boolean isClientThread() {
        return Minecraft.getMinecraft().isCallingFromMinecraftThread();
    }

    @Override
    public EntityPlayer getClientPlayer() {
        return Minecraft.getMinecraft().thePlayer;
    }

    private void setTileEntityRendererDispatcher(Class clazz, TileEntitySpecialRenderer r) {
        ClientRegistry.bindTileEntitySpecialRenderer(clazz, r);
    }

    @Override
    public void setItemModel(Item item, int meta, String variant) {
        ModelLoader.setCustomModelResourceLocation(item, meta,
                new ModelResourceLocation(Item.itemRegistry.getNameForObject(item), variant));
    }

    @Override
    public void registerISensitiveMeshes(Collection<Item> items) {
        for (Item it : items) {
            if (!(it instanceof ISensitiveMesh)) {
                continue;
            }
            final ISensitiveMesh ism = (ISensitiveMesh) it;
            ModelLoader.setCustomMeshDefinition(it, new ItemMeshDefinition() {
                @Override
                public ModelResourceLocation getModelLocation(ItemStack stack) {
                    String meshName = ism.getMeshName(stack);
                    return new ModelResourceLocation("factorization:" + meshName + "#inventory");
                }
            });
            for (ItemStack is : ism.getMeshSamples()) {
                ModelLoader.addVariantName(it, "factorization:" + ism.getMeshName(is));
            }
        }
    }

    ArrayList<Item> standardItems = new ArrayList<Item>();
    @Override
    public void standardItemModel(ItemFactorization item) {
        if (item.getHasSubtypes() && !(item instanceof ISameModelForAllItems)) return;
        if (item instanceof ISensitiveMesh) return;
        standardItems.add(item);
    }

    private void setItemBlockModel(Block block, int meta, String variant) {
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), meta,
                new ModelResourceLocation(Block.blockRegistry.getNameForObject(block), variant));
    }

    private void setItemBlockModelFromState(Block block) {
        if (block == null) return;
        for (IBlockState i : block.getBlockState().getValidStates()) {
            setItemBlockModel(block, block.getMetaFromState(i), DataUtil.getStatePropertyString(i));
        }
    }

    @Override
    public void registerRenderers() {
        for (Block b : new Block[] {
                Core.registry.artifact_forge,
                Core.registry.dark_iron_ore,
                Core.registry.fractured_bedrock_block,
                Core.registry.blasted_bedrock_block,
                Core.registry.matcher_block,
                Core.registry.gargantuan_block,
                Core.registry.blastBlock,
                Core.registry.mantlerock_block,
                Core.registry.factory_block_barrel,
                Core.registry.parasieve_block,
                Core.registry.caliometric_burner_block,
                Core.registry.creative_energy,
                Core.registry.furnace_heater,
                Core.registry.whirligig,
        }) {
            setItemBlockModel(b, 0, "inventory");
        }
        modelForMetadata(Core.registry.resource_block, "copper_ore", "silver_block", "lead_block", "dark_iron_block", "copper_block");

        for (Block b : new Block[] {
                Core.registry.resource_block,
                Core.registry.colossal_block,
        }) {
            setItemBlockModelFromState(b);
        }

        for (Item it : standardItems) {
            setItemModel(it, 0, "inventory");
        }
        standardItems = null;

        NORELEASE.fixme("Glaze bucket");
        NORELEASE.fixme("Barrel");
        NORELEASE.fixme("Artifacts");
        // MinecraftForgeClient.registerItemRenderer(Core.registry.glaze_bucket, new ItemRenderGlazeBucket());
        // MinecraftForgeClient.registerItemRenderer(Core.registry.daybarrel, new DayBarrelItemRenderer(renderBarrel));
        // MinecraftForgeClient.registerItemRenderer(Core.registry.brokenTool, new RenderBrokenArtifact());
        Core.loadBus(GooRenderer.INSTANCE);
    }

    @SubscribeEvent
    public void registerComplicatedModels(ModelBakeEvent event) {
        final ModelResourceLocation barrelNormal = new ModelResourceLocation("factorization:FzBlockBarrel#normal");
        event.modelRegistry.putObject(barrelNormal, new BarrelModel(false));
        final ModelResourceLocation barrelInv = new ModelResourceLocation("factorization:FzBlockBarrel#inventory");
        event.modelRegistry.putObject(barrelInv, new BarrelModel(true));
        ModelLoader.setCustomMeshDefinition(DataUtil.getItem(Core.registry.factory_block_barrel), new ItemMeshDefinition() {
            @Override
            public ModelResourceLocation getModelLocation(ItemStack stack) {
                return barrelInv;
            }
        });
    }

    @Override
    public void registerTesrs() {
        setTileEntityRendererDispatcher(TileEntityDayBarrel.class, new TileEntityDayBarrelRenderer());
        setTileEntityRendererDispatcher(TileEntityGreenware.class, new TileEntityGreenwareRender());
        setTileEntityRendererDispatcher(TileEntityHeater.class, new TileEntityHeaterRenderer());
        setTileEntityRendererDispatcher(TileEntityLeydenJar.class, new TileEntityLeydenJarRender());
        setTileEntityRendererDispatcher(TileEntityCompressionCrafter.class, new TileEntityCompressionCrafterRenderer());
        setTileEntityRendererDispatcher(SocketScissors.class, new TileEntitySocketRenderer());
        setTileEntityRendererDispatcher(SocketLacerator.class, new TileEntitySocketRenderer());
        setTileEntityRendererDispatcher(SocketFanturpeller.class, new TileEntitySocketRenderer());
        setTileEntityRendererDispatcher(TileEntityHinge.class, new TileEntityHingeRenderer());
        setTileEntityRendererDispatcher(SocketPoweredCrank.class, new TileEntitySocketRenderer());
        setTileEntityRendererDispatcher(TileEntitySteamShaft.class, new TileEntitySteamShaftRenderer());
        setTileEntityRendererDispatcher(TileEntityShaft.class, new TileEntityShaftRenderer());
        setTileEntityRendererDispatcher(TileEntityBiblioGen.class, new TileEntityBiblioGenRenderer());


        RenderManager rm = Minecraft.getMinecraft().getRenderManager();

        RenderingRegistry.registerEntityRenderingHandler(TileEntityWrathLamp.RelightTask.class, new EmptyRender(rm));
        RenderingRegistry.registerEntityRenderingHandler(ServoMotor.class, new RenderServoMotor(rm));
        RenderingRegistry.registerEntityRenderingHandler(StepperEngine.class, new RenderStepperEngine(rm));
        RenderingRegistry.registerEntityRenderingHandler(ColossusController.class, new ColossusControllerRenderer(rm));
        RenderingRegistry.registerEntityRenderingHandler(EntityPoster.class, new RenderPoster(rm));
        RenderingRegistry.registerEntityRenderingHandler(EntityCitizen.class, new RenderCitizen(rm));
        RenderingRegistry.registerEntityRenderingHandler(EntityMinecartDayBarrel.class, new RenderMinecartDayBarrel(rm));
        RenderingRegistry.registerEntityRenderingHandler(EntityLeafBomb.class, new RenderSnowball<EntityLeafBomb>(rm, Core.registry.leafBomb, Minecraft.getMinecraft().getRenderItem()));
    }

    @SubscribeEvent
    public void onTextureStitch(TextureStitchEvent.Pre event) {
        RenderUtil.loadSprites("factorization", FzIcons.class, "", event);
        RenderUtil.loadSprites("factorization", BarrelModel.class, "blocks/storage/", event);
        try {
            BarrelModel.template = (IRetexturableModel) ModelLoaderRegistry.getModel(new ResourceLocation("factorization:block/barrel_template"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getPocketCraftingTableKey() {
        return GameSettings.getKeyDisplayString(FactorizationKeyHandler.pocket_key.getKeyCode());
    }

    @Override
    public boolean isClientHoldingShift() {
        if (FMLCommonHandler.instance().getEffectiveSide() != Side.CLIENT) {
            return false;
        }
        Minecraft mc = Minecraft.getMinecraft();
        return org.lwjgl.input.Keyboard.isKeyDown(42 /* sneak */);
        //return !mc.gameSettings.keyBindSneak.pressed;
    }

    @Override
    public void afterLoad() {
        Core.logInfo("Reloading game settings");
        Minecraft.getMinecraft().gameSettings.loadOptions();
    }

    @Override
    public void sendBlockClickPacket() {
        Minecraft mc = Minecraft.getMinecraft();
        MovingObjectPosition mop = mc.objectMouseOver;
        new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.START_DESTROY_BLOCK /* NORELEASE: was 0, is this correct? This is for barrels: punch w/ empty fist so that an item goes into that fist; prevents a second click from happening. */, mop.getBlockPos(), mop.sideHit);
    }

    private static void modelItem(Block block, String... names) {
        ModelBakery.addVariantName(DataUtil.getItem(block), names);
    }

    private static void modelForMetadata(Block block, String... parts) {
        Item item = DataUtil.getItem(block);
        for (int md = 0; md < parts.length; md++) {
            String name = parts[md];
            name = "factorization:" + name;
            parts[md] = name;
            ModelResourceLocation modelName = new ModelResourceLocation(name, "inventory");
            ModelLoader.setCustomModelResourceLocation(item, md, modelName);
        }
        ModelBakery.addVariantName(item, parts);
    }

    private static void modelForMetadataExplicit(Block block, Object... parts) {
        ArrayList<String> found = new ArrayList<String>();
        Item item = DataUtil.getItem(block);
        if (parts.length % 2 != 0) throw new IllegalArgumentException("Invalid argument format");
        for (int i = 0; i < parts.length; i += 2) {
            int md = (Integer) parts[i];
            String name = (String) parts[i + 1];
            name = "factorization:" + name;
            parts[i + 1] = name;
            ModelLoader.setCustomModelResourceLocation(item, md, new ModelResourceLocation(name, "inventory"));
            found.add(name);
        }
        ModelBakery.addVariantName(item, found.toArray(new String[found.size()]));
    }


}
