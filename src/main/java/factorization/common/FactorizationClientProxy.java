package factorization.common;

import factorization.api.Coord;
import factorization.artifact.ContainerForge;
import factorization.artifact.GuiArtifactForge;
import factorization.beauty.*;
import factorization.charge.*;
import factorization.citizen.EntityCitizen;
import factorization.citizen.RenderCitizen;
import factorization.mechanics.BlockRenderHinge;
import factorization.mechanics.SocketPoweredCrank;
import factorization.mechanics.TileEntityHinge;
import factorization.mechanics.TileEntityHingeRenderer;
import factorization.shared.*;
import factorization.weird.*;
import factorization.weird.poster.EntityPoster;
import factorization.weird.poster.RenderPoster;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.entity.RenderSnowball;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.MinecraftForgeClient;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import factorization.ceramics.BlockRenderGreenware;
import factorization.ceramics.ItemRenderGlazeBucket;
import factorization.ceramics.TileEntityGreenware;
import factorization.ceramics.TileEntityGreenwareRender;
import factorization.colossi.ColossusController;
import factorization.colossi.ColossusControllerRenderer;
import factorization.crafting.BlockRenderCompressionCrafter;
import factorization.crafting.BlockRenderMixer;
import factorization.crafting.ContainerMixer;
import factorization.crafting.GuiMixer;
import factorization.crafting.GuiStamper;
import factorization.crafting.TileEntityCompressionCrafter;
import factorization.crafting.TileEntityCompressionCrafterRenderer;
import factorization.crafting.TileEntityMixer;
import factorization.crafting.TileEntityMixerRenderer;
import factorization.darkiron.BlockDarkIronOre;
import factorization.darkiron.GlintRenderer;
import factorization.oreprocessing.BlockRenderCrystallizer;
import factorization.oreprocessing.ContainerCrystallizer;
import factorization.oreprocessing.ContainerSlagFurnace;
import factorization.oreprocessing.GuiCrystallizer;
import factorization.oreprocessing.GuiSlag;
import factorization.oreprocessing.TileEntityCrystallizer;
import factorization.oreprocessing.TileEntityCrystallizerRender;
import factorization.oreprocessing.TileEntityGrinderRender;
import factorization.servo.BlockRenderServoRail;
import factorization.servo.GuiParasieve;
import factorization.servo.RenderServoMotor;
import factorization.servo.ServoMotor;
import factorization.sockets.BlockRenderSocketBase;
import factorization.sockets.SocketLacerator;
import factorization.sockets.SocketScissors;
import factorization.sockets.TileEntitySocketRenderer;
import factorization.sockets.fanturpeller.SocketFanturpeller;
import factorization.twistedblock.TwistedRender;
import factorization.utiligoo.GooRenderer;
import factorization.wrath.BlockRenderLamp;
import factorization.wrath.TileEntityWrathLamp;

public class FactorizationClientProxy extends FactorizationProxy {
    public FactorizationKeyHandler keyHandler = new FactorizationKeyHandler();
    public FactorizationClientProxy() {
        Core.loadBus(this);
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
        if (ID == FactoryType.ARTIFACTFORGEGUI.gui) {
            return new GuiArtifactForge(new ContainerForge(new Coord(world, x, y, z), player));
        }
        
        TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof TileEntityFactorization)) {
            return null;
        }
        TileEntityFactorization fac = (TileEntityFactorization) te;
        ContainerFactorization cont;
        if (ID == FactoryType.SLAGFURNACE.gui) {
            cont = new ContainerSlagFurnace(player, fac);
        } else if (ID == FactoryType.MIXER.gui) {
            cont = new ContainerMixer(player, fac);
        } else if (ID == FactoryType.CRYSTALLIZER.gui) {
            cont = new ContainerCrystallizer(player, fac);
        } else {
            cont = new ContainerFactorization(player, fac);
        }
        GuiScreen gui = null;
        if (ID == FactoryType.STAMPER.gui) {
            gui = new GuiStamper(cont);
        }
        if (ID == FactoryType.SLAGFURNACE.gui) {
            gui = new GuiSlag(cont);
        }
        if (ID == FactoryType.MIXER.gui && cont instanceof ContainerMixer) {
            gui = new GuiMixer((ContainerMixer) cont);
        }
        if (ID == FactoryType.CRYSTALLIZER.gui) {
            gui = new GuiCrystallizer(cont);
        }
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
    public EntityPlayer getClientPlayer() {
        return Minecraft.getMinecraft().thePlayer;
    }





    private void setTileEntityRendererDispatcher(Class clazz, TileEntitySpecialRenderer r) {
        ClientRegistry.bindTileEntitySpecialRenderer(clazz, r);
    }

    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    @Override
    public void registerRenderers() {
        setTileEntityRendererDispatcher(TileEntityDayBarrel.class, new TileEntityDayBarrelRenderer());
        setTileEntityRendererDispatcher(TileEntityGreenware.class, new TileEntityGreenwareRender());
        if (FzConfig.renderTEs) {
            setTileEntityRendererDispatcher(TileEntityHeater.class, new TileEntityHeaterRenderer());
            setTileEntityRendererDispatcher(TileEntityMixer.class, new TileEntityMixerRenderer());
            setTileEntityRendererDispatcher(TileEntityCrystallizer.class, new TileEntityCrystallizerRender());
            setTileEntityRendererDispatcher(TileEntitySteamTurbine.class, new TileEntitySteamTurbineRender());
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
        }

        RenderingRegistry.registerEntityRenderingHandler(TileEntityWrathLamp.RelightTask.class, new EmptyRender());
        RenderingRegistry.registerEntityRenderingHandler(ServoMotor.class, new RenderServoMotor());
        RenderingRegistry.registerEntityRenderingHandler(ColossusController.class, new ColossusControllerRenderer());
        RenderingRegistry.registerEntityRenderingHandler(EntityPoster.class, new RenderPoster());
        RenderingRegistry.registerEntityRenderingHandler(EntityCitizen.class, new RenderCitizen());
        RenderingRegistry.registerEntityRenderingHandler(EntityMinecartDayBarrel.class, new RenderMinecartDayBarrel());
        RenderingRegistry.registerEntityRenderingHandler(EntityLeafBomb.class, new RenderSnowball(Core.registry.leafBomb, 0));

        RenderingRegistry.registerBlockHandler(new FactorizationRender());
        RenderingRegistry.registerBlockHandler(new FactorizationRenderNonTE());
        new BlockRenderDefault();
        BlockRenderBattery renderBattery = new BlockRenderBattery();
        BlockRenderDayBarrel renderBarrel = new BlockRenderDayBarrel();
        new BlockRenderLeydenJar();
        new BlockRenderHeater();
        new BlockRenderLamp();
        new BlockRenderMirrorStand();
        new BlockRenderSteamTurbine();
        new BlockRenderWire();
        new BlockRenderMixer();
        new BlockRenderCrystallizer();
        new BlockRenderCompressionCrafter();
        new BlockRenderGreenware().setup();
        //NORELEASE new BlockRenderRocketEngine();
        new BlockRenderServoRail();
        new BlockRenderHinge();
        new BlockRenderSapExtractor();
        new BlockRenderAnthrogen();
        new BlockRenderSteamShaft();
        new BlockRenderSolarBoiler();
        new BlockRenderShaftGen();
        new BlockRenderShaft();
        new BlockRenderBiblioGen();
        new BlockRenderWindMill();
        new BlockRenderWaterWheel();
        for (FactoryType ft : new FactoryType[] {
                FactoryType.SOCKET_EMPTY,
                FactoryType.SOCKET_LACERATOR,
                FactoryType.SOCKET_ROBOTHAND,
                FactoryType.SOCKET_SHIFTER,
                FactoryType.SOCKET_BLOWER,
                FactoryType.SOCKET_PUMP,
                FactoryType.SOCKET_BARE_MOTOR,
                FactoryType.SOCKET_SCISSORS,
                FactoryType.SOCKET_POWERED_CRANK
        }) {
            new BlockRenderSocketBase(ft);
        }
        for (FactoryType ft : new FactoryType[] {
                FactoryType.STAMPER,
                FactoryType.PACKAGER,
                FactoryType.SLAGFURNACE,
                FactoryType.PARASIEVE,
                FactoryType.CALIOMETRIC_BURNER,
                FactoryType.CREATIVE_CHARGE
                }) {
            FactorizationBlockRender.setDefaultRender(ft);
        }
        new BlockRenderEmpty(FactoryType.EXTENDED);

        ItemRenderCapture capture = new ItemRenderCapture();
        MinecraftForgeClient.registerItemRenderer(Item.getItemFromBlock(Core.registry.factory_block), capture);
        MinecraftForgeClient.registerItemRenderer(Item.getItemFromBlock(Core.registry.factory_block_barrel), capture);
        MinecraftForgeClient.registerItemRenderer(Core.registry.battery, new BatteryItemRender(renderBattery));
        MinecraftForgeClient.registerItemRenderer(Core.registry.glaze_bucket, new ItemRenderGlazeBucket());
        MinecraftForgeClient.registerItemRenderer(Core.registry.daybarrel, new DayBarrelItemRenderer(renderBarrel));
        MinecraftForgeClient.registerItemRenderer(Core.registry.twistedBlock, new TwistedRender());
        setTileEntityRendererDispatcher(BlockDarkIronOre.Glint.class, new GlintRenderer());
        Core.loadBus(GooRenderer.INSTANCE);
    }
    
    @Override
    public void texturepackChanged(IIconRegister reg) {
        TileEntityGrinderRender.remakeModel();
        BlockRenderServoRail.registerColoredIcons(reg);
    }
    
    @Override
    public boolean BlockRenderHelper_has_texture(BlockRenderHelper block, int f) {
        if (block.textures == null) {
            return true;
        }
        return block.textures[f] != null;
    }
    
    @Override
    public void BlockRenderHelper_clear_texture(BlockRenderHelper block) {
        block.textures = null;
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
        new C07PacketPlayerDigging(0, mop.blockX, mop.blockY, mop.blockZ, mop.sideHit);
    }
}
