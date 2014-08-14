package factorization.common;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
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
import factorization.charge.BatteryItemRender;
import factorization.charge.BlockRenderBattery;
import factorization.charge.BlockRenderHeater;
import factorization.charge.BlockRenderLeydenJar;
import factorization.charge.BlockRenderMirrorStand;
import factorization.charge.BlockRenderSteamTurbine;
import factorization.charge.BlockRenderWire;
import factorization.charge.TileEntityHeater;
import factorization.charge.TileEntityHeaterRenderer;
import factorization.charge.TileEntityLeydenJar;
import factorization.charge.TileEntityLeydenJarRender;
import factorization.charge.TileEntitySteamTurbine;
import factorization.charge.TileEntitySteamTurbineRender;
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
import factorization.shared.BlockRenderDefault;
import factorization.shared.BlockRenderEmpty;
import factorization.shared.BlockRenderHelper;
import factorization.shared.Core;
import factorization.shared.EmptyRender;
import factorization.shared.FactorizationBlockRender;
import factorization.shared.FactorizationRender;
import factorization.shared.ItemRenderCapture;
import factorization.shared.TileEntityFactorization;
import factorization.sockets.BlockRenderSocketBase;
import factorization.sockets.SocketLacerator;
import factorization.sockets.SocketScissors;
import factorization.sockets.TileEntitySocketRenderer;
import factorization.sockets.fanturpeller.SocketFanturpeller;
import factorization.weird.BlockRenderDayBarrel;
import factorization.weird.ContainerPocket;
import factorization.weird.DayBarrelItemRenderer;
import factorization.weird.GuiPocketTable;
import factorization.weird.TileEntityDayBarrel;
import factorization.weird.TileEntityDayBarrelRenderer;
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
        if (ID == FactoryType.MIXER.gui) {
            gui = new GuiMixer((ContainerMixer)cont);
        }
        if (ID == FactoryType.CRYSTALLIZER.gui) {
            gui = new GuiCrystallizer(cont);
        }
        if (ID == FactoryType.PARASIEVE.gui) {
            gui = new GuiParasieve(cont);
        }
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

    int fireParticlesSpawned = 0;
    int fireParticlesMax = 5;

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

    @Override
    public void registerRenderers() {
        setTileEntityRendererDispatcher(TileEntityDayBarrel.class, new TileEntityDayBarrelRenderer());
        setTileEntityRendererDispatcher(TileEntityGreenware.class, new TileEntityGreenwareRender());
        if (FzConfig.renderTEs) {
            // This is entirely Azanor's fault.
            setTileEntityRendererDispatcher(TileEntityHeater.class, new TileEntityHeaterRenderer());
            setTileEntityRendererDispatcher(TileEntityMixer.class, new TileEntityMixerRenderer());
            setTileEntityRendererDispatcher(TileEntityCrystallizer.class, new TileEntityCrystallizerRender());
            setTileEntityRendererDispatcher(TileEntitySteamTurbine.class, new TileEntitySteamTurbineRender());
            setTileEntityRendererDispatcher(TileEntityLeydenJar.class, new TileEntityLeydenJarRender());
            setTileEntityRendererDispatcher(TileEntityCompressionCrafter.class, new TileEntityCompressionCrafterRenderer());
            setTileEntityRendererDispatcher(SocketScissors.class, new TileEntitySocketRenderer());
            setTileEntityRendererDispatcher(SocketLacerator.class, new TileEntitySocketRenderer());
            setTileEntityRendererDispatcher(SocketFanturpeller.class, new TileEntitySocketRenderer());
            // End section that is azanor's fault
        }

        RenderingRegistry.registerEntityRenderingHandler(TileEntityWrathLamp.RelightTask.class, new EmptyRender());
        RenderingRegistry.registerEntityRenderingHandler(ServoMotor.class, new RenderServoMotor());

        RenderingRegistry.registerBlockHandler(new FactorizationRender());
        BlockRenderBattery renderBattery = new BlockRenderBattery();
        BlockRenderDayBarrel renderBarrel = new BlockRenderDayBarrel();
        new BlockRenderLeydenJar();
        new BlockRenderDefault();
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
        for (FactoryType ft : new FactoryType[] {
                FactoryType.SOCKET_EMPTY,
                FactoryType.SOCKET_LACERATOR,
                FactoryType.SOCKET_ROBOTHAND,
                FactoryType.SOCKET_SHIFTER,
                FactoryType.SOCKET_BLOWER,
                FactoryType.SOCKET_PUMP,
                FactoryType.SOCKET_BARE_MOTOR,
                FactoryType.SOCKET_SCISSORS
        }) {
            new BlockRenderSocketBase(ft);
        }
        for (FactoryType ft : new FactoryType[] {
                FactoryType.STAMPER,
                FactoryType.PACKAGER,
                FactoryType.SLAGFURNACE,
                FactoryType.SOLARBOILER,
                FactoryType.PARASIEVE,
                FactoryType.CALIOMETRIC_BURNER
                }) {
            FactorizationBlockRender.setDefaultRender(ft);
        }
        new BlockRenderEmpty(FactoryType.EXTENDED);

        ItemRenderCapture capture = new ItemRenderCapture();
        MinecraftForgeClient.registerItemRenderer(Item.getItemFromBlock(Core.registry.factory_block), capture);
        MinecraftForgeClient.registerItemRenderer(Core.registry.battery, new BatteryItemRender(renderBattery));
        MinecraftForgeClient.registerItemRenderer(Core.registry.glaze_bucket, new ItemRenderGlazeBucket());
        MinecraftForgeClient.registerItemRenderer(Core.registry.daybarrel, new DayBarrelItemRenderer(renderBarrel));
        setTileEntityRendererDispatcher(BlockDarkIronOre.Glint.class, new GlintRenderer());
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
}
