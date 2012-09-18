package factorization.client;

import java.io.File;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.Map;
import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.src.Block;
import net.minecraft.src.ContainerPlayer;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.GuiContainer;
import net.minecraft.src.GuiScreen;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.KeyBinding;
import net.minecraft.src.ModLoader;
import net.minecraft.src.NetHandler;
import net.minecraft.src.Profiler;
import net.minecraft.src.TileEntity;
import net.minecraft.src.TileEntityChest;
import net.minecraft.src.TileEntitySpecialRenderer;
import net.minecraft.src.World;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeSubscribe;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.client.registry.KeyBindingRegistry;
import cpw.mods.fml.client.registry.KeyBindingRegistry.KeyHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.TickType;
import cpw.mods.fml.common.registry.LanguageRegistry;
import factorization.api.Coord;
import factorization.api.IFactoryType;
import factorization.client.coremod.GuiKeyEvent;
import factorization.client.gui.GuiCrystallizer;
import factorization.client.gui.GuiCutter;
import factorization.client.gui.GuiGrinder;
import factorization.client.gui.GuiMaker;
import factorization.client.gui.GuiMechaConfig;
import factorization.client.gui.GuiMixer;
import factorization.client.gui.GuiPocketTable;
import factorization.client.gui.GuiRouter;
import factorization.client.gui.GuiSlag;
import factorization.client.gui.GuiStamper;
import factorization.client.render.BatteryItemRender;
import factorization.client.render.BlockRenderBattery;
import factorization.client.render.BlockRenderCrystallizer;
import factorization.client.render.BlockRenderDefault;
import factorization.client.render.BlockRenderGrinder;
import factorization.client.render.BlockRenderHeater;
import factorization.client.render.BlockRenderLamp;
import factorization.client.render.BlockRenderMirrorStand;
import factorization.client.render.BlockRenderMixer;
import factorization.client.render.BlockRenderSculpture;
import factorization.client.render.BlockRenderSentryDemon;
import factorization.client.render.BlockRenderSolarTurbine;
import factorization.client.render.BlockRenderWire;
import factorization.client.render.EmptyRender;
import factorization.client.render.EntitySteamFX;
import factorization.client.render.EntityWrathFlameFX;
import factorization.client.render.FactorizationRender;
import factorization.client.render.TileEntityBarrelRenderer;
import factorization.client.render.TileEntityCrystallizerRender;
import factorization.client.render.TileEntityGreenwareRender;
import factorization.client.render.TileEntityGrinderRender;
import factorization.client.render.TileEntityHeaterRenderer;
import factorization.client.render.TileEntityMirrorRenderer;
import factorization.client.render.TileEntityMixerRenderer;
import factorization.client.render.TileEntitySolarTurbineRender;
import factorization.client.render.TileEntityWatchDemonRenderer;
import factorization.common.Command;
import factorization.common.ContainerCrystallizer;
import factorization.common.ContainerFactorization;
import factorization.common.ContainerGrinder;
import factorization.common.ContainerMechaModder;
import factorization.common.ContainerMixer;
import factorization.common.ContainerPocket;
import factorization.common.ContainerSlagFurnace;
import factorization.common.Core;
import factorization.common.FactorizationProxy;
import factorization.common.FactoryType;
import factorization.common.Registry;
import factorization.common.TileEntityBarrel;
import factorization.common.TileEntityCrystallizer;
import factorization.common.TileEntityFactorization;
import factorization.common.TileEntityGreenware;
import factorization.common.TileEntityGrinder;
import factorization.common.TileEntityHeater;
import factorization.common.TileEntityMirror;
import factorization.common.TileEntityMixer;
import factorization.common.TileEntitySlagFurnace;
import factorization.common.TileEntitySolarTurbine;
import factorization.common.TileEntityWatchDemon;
import factorization.common.TileEntityWrathLamp;

public class FactorizationClientProxy extends FactorizationProxy {
    //COMMON
    @Override
    public void makeItemsSide() {
        Registry registry = Core.registry;
        registry.mecha_head = new MechaArmorTextured(registry.itemID("mechaHead", 9010), 0);
        registry.mecha_chest = new MechaArmorTextured(registry.itemID("mechaChest", 9011), 1);
        registry.mecha_leg = new MechaArmorTextured(registry.itemID("mechaLeg", 9012), 2);
        registry.mecha_foot = new MechaArmorTextured(registry.itemID("mechaFoot", 9013), 3);
    }

    @Override
    public File getWorldSaveDir(World world) {
        String dotmc = ModLoader.getMinecraftInstance().getMinecraftDir().getPath();
        char slash = File.separatorChar;
        return new File(dotmc + slash + "saves" + slash + world.getSaveHandler().getSaveDirectoryName());
    }

    @Override
    public void broadcastTranslate(EntityPlayer who, String... msg) {
        String format = msg[0];
        String params[] = new String[msg.length - 1];
        System.arraycopy(msg, 1, params, 0, msg.length - 1);
        try {
            who.addChatMessage(String.format(format, (Object[]) params));
        } catch (IllegalFormatException e) {
        }
    }

    @Override
    public void pokeChest(TileEntityChest chest) {
        float angle = 1.0F;
        if (chest.lidAngle < angle) {
            chest.lidAngle = angle;
        }
    }

    @Override
    public EntityPlayer getPlayer(NetHandler handler) {
        return ModLoader.getMinecraftInstance().thePlayer;
    }

    //	@Override
    //	public void addPacket(EntityPlayer player, Packet packet) {
    //		World w = ModLoader.getMinecraftInstance().theWorld;
    //		if (w != null && w.isRemote) {
    //			if (Minecraft.getMinecraft().getSendQueue() == null) {
    //				return; //wow, what?
    //			}
    //			Minecraft.getMinecraft().getSendQueue().addToSendQueue(packet);
    //		}
    //	}

    @Override
    public Profiler getProfiler() {
        return Minecraft.getMinecraft().mcProfiler;
    }

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world,
            int x, int y, int z) {
        if (ID == FactoryType.NULLGUI.gui) {
            player.craftingInventory = new ContainerPlayer(player.inventory);
            //ModLoader.getMinecraftInstance().displayGuiScreen(null);
            return null;
        }
        if (ID == FactoryType.POCKETCRAFTGUI.gui) {
            return new GuiPocketTable(new ContainerPocket(player));
        }

        if (ID == FactoryType.MECHATABLEGUICONFIG.gui) {
            return new GuiMechaConfig(new ContainerMechaModder(player, new Coord(world, x, y, z)));
        }

        TileEntity te = world.getBlockTileEntity(x, y, z);
        if (!(te instanceof TileEntityFactorization)) {
            return null;
        }
        TileEntityFactorization fac = (TileEntityFactorization) te;
        ContainerFactorization cont;
        if (ID == FactoryType.SLAGFURNACE.gui) {
            cont = new ContainerSlagFurnace(player, fac);
        } else if (ID == FactoryType.GRINDER.gui) {
            cont = new ContainerGrinder(player, fac);
        } else if (ID == FactoryType.MIXER.gui) {
            cont = new ContainerMixer(player, fac);
        } else if (ID == FactoryType.CRYSTALLIZER.gui) {
            cont = new ContainerCrystallizer(player, fac);
        } else {
            cont = new ContainerFactorization(player, fac);
        }
        GuiScreen gui = null;
        if (ID == FactoryType.ROUTER.gui) {
            gui = new GuiRouter(cont);
        }
        if (ID == FactoryType.CUTTER.gui) {
            gui = new GuiCutter(cont);
        }
        if (ID == FactoryType.MAKER.gui) {
            gui = new GuiMaker(cont);
        }
        if (ID == FactoryType.STAMPER.gui) {
            gui = new GuiStamper(cont);
        }
        if (ID == FactoryType.SLAGFURNACE.gui) {
            gui = new GuiSlag(cont);
        }
        if (ID == FactoryType.GRINDER.gui) {
            gui = new GuiGrinder(cont);
        }
        if (ID == FactoryType.MIXER.gui) {
            gui = new GuiMixer(cont);
        }
        if (ID == FactoryType.CRYSTALLIZER.gui) {
            gui = new GuiCrystallizer(cont);
        }

        cont.addSlotsForGui(fac, player.inventory);
        return gui;
    }

    //CLIENT
    @Override
    public void addName(Object objectToName, String name) {
        String objectName;
        if (objectToName instanceof Item) {
            objectName = ((Item) objectToName).getItemName();
        } else if (objectToName instanceof Block) {
            objectName = ((Block) objectToName).getBlockName();
        } else if (objectToName instanceof ItemStack) {
            objectName = ((ItemStack) objectToName).getItem().getItemNameIS((ItemStack) objectToName);
        } else if (objectToName instanceof String) {
            objectName = (String) objectToName;
        } else {
            throw new IllegalArgumentException(String.format("Illegal object for naming %s", objectToName));
        }
        objectName += ".name";
        LanguageRegistry.instance().addStringLocalization(objectName, "en_US", name);
    }

    @Override
    public String translateItemStack(ItemStack is) {
        if (is == null) {
            return "null";
        }
        return is.getItem().getItemDisplayName(is);
    }

    @Override
    public void pokePocketCrafting() {
        // If the player has a pocket crafting table open, have it update
        Minecraft minecraft = ModLoader.getMinecraftInstance();
        if (minecraft.currentScreen instanceof GuiPocketTable) {
            GuiPocketTable gui = (GuiPocketTable) minecraft.currentScreen;
            gui.containerPocket.updateCraft();
        }
    }

    int fireParticlesSpawned = 0;
    int fireParticlesMax = 5;

    @Override
    public void randomDisplayTickFor(World w, int x, int y, int z, Random rand) {
        Coord here = new Coord(w, x, y, z);
        int id = w.getBlockId(x, y, z);
        int md = w.getBlockMetadata(x, y, z);
        if (id == Core.factory_block_id) {
            TileEntity te = w.getBlockTileEntity(x, y, z);
            if (!(te instanceof IFactoryType)) {
                return;
            }

            FactoryType ft = ((IFactoryType) te).getFactoryType();

            if (ft == FactoryType.LAMP) {
                for (int i = 0; i < 3; i++) {
                    double X = x + 0.4 + rand.nextFloat() * 0.2;
                    double Z = z + 0.4 + rand.nextFloat() * 0.2;
                    EntityWrathFlameFX flame = new EntityWrathFlameFX(w,
                            X, y + 0.2 + rand.nextFloat() * 0.1, Z,
                            0.001 - rand.nextFloat() * 0.002, 0.01, 0.001 - rand.nextFloat() * 0.002);
                    ModLoader.getMinecraftInstance().effectRenderer.addEffect(flame, flame);
                }
            }
            if (ft == FactoryType.SLAGFURNACE) {
                TileEntitySlagFurnace slag = (TileEntitySlagFurnace) te;
                if (!slag.isBurning()) {
                    return;
                }
                int var6 = slag.facing_direction;
                float var7 = (float) x + 0.5F;
                float var8 = (float) y + 0.0F + rand.nextFloat() * 6.0F / 16.0F;
                float var9 = (float) z + 0.5F;
                float var10 = 0.52F;
                float var11 = rand.nextFloat() * 0.6F - 0.3F;

                if (var6 == 4) {
                    w.spawnParticle("smoke", (double) (var7 - var10), (double) var8, (double) (var9 + var11), 0.0D, 0.0D, 0.0D);
                    w.spawnParticle("flame", (double) (var7 - var10), (double) var8, (double) (var9 + var11), 0.0D, 0.0D, 0.0D);
                } else if (var6 == 5) {
                    w.spawnParticle("smoke", (double) (var7 + var10), (double) var8, (double) (var9 + var11), 0.0D, 0.0D, 0.0D);
                    w.spawnParticle("flame", (double) (var7 + var10), (double) var8, (double) (var9 + var11), 0.0D, 0.0D, 0.0D);
                } else if (var6 == 2) {
                    w.spawnParticle("smoke", (double) (var7 + var11), (double) var8, (double) (var9 - var10), 0.0D, 0.0D, 0.0D);
                    w.spawnParticle("flame", (double) (var7 + var11), (double) var8, (double) (var9 - var10), 0.0D, 0.0D, 0.0D);
                } else if (var6 == 3) {
                    w.spawnParticle("smoke", (double) (var7 + var11), (double) var8, (double) (var9 + var10), 0.0D, 0.0D, 0.0D);
                    w.spawnParticle("flame", (double) (var7 + var11), (double) var8, (double) (var9 + var10), 0.0D, 0.0D, 0.0D);
                }

            }
            if (ft == FactoryType.SOLARTURBINE) {
                TileEntitySolarTurbine sol = (TileEntitySolarTurbine) te;
                if (sol.getReflectors() > 0 && sol.water_level > 0) {
                    double X = x + 2 / 16F;
                    double Z = z + 2 / 16F;
                    if (rand.nextBoolean()) {
                        X += 0.5;
                    }
                    if (rand.nextBoolean()) {
                        Z += 0.5;
                    }
                    X += rand.nextFloat() * 2 / 16;
                    Z += rand.nextFloat() * 2 / 16;
                    double Y = y + (0.99F + sol.water_level / (TileEntitySolarTurbine.max_water / 4)) / 16F;
                    EntitySteamFX steam = new EntitySteamFX(w, X, Y, Z);
                    ModLoader.getMinecraftInstance().effectRenderer.addEffect(steam, null);
                }
            }
        }
        if (id == Core.lightair_id) {
            if (md == Core.registry.lightair_block.fire_md) {
                int to_spawn = 1;
                EntityPlayer player = Core.proxy.getClientPlayer();
                boolean force = false;
                boolean big = true;
                if (player != null) {
                    int dx = (int) (player.posX) - x, dy = (int) (player.posY) - y, dz = (int) (player.posZ) - z;
                    int dist = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
                    if (dist < 4) {
                        to_spawn = 8;
                        force = true;
                        big = false;
                    }
                    else if (dist <= 12) {
                        to_spawn = 4;
                        force = true;
                    }
                    else if (dist <= 16) {
                        to_spawn = 1;
                    }
                }

                //				if (to_spawn == 1) {
                //					if (rand.nextFloat() > 0.2) {
                //						return;
                //					}
                //				}
                if (fireParticlesSpawned >= fireParticlesMax) {
                    if (!force) {
                        return;
                    }
                    else {
                        to_spawn /= 4;
                    }
                }
                if (!force) {
                    fireParticlesSpawned += to_spawn;
                }
                for (int i = 0; i < to_spawn; i++) {
                    double X = x + .05 + rand.nextFloat() * .95;
                    double Z = z + .05 + rand.nextFloat() * .95;
                    EntityWrathFlameFX flame = new EntityWrathFlameFX(w,
                            X, y + rand.nextFloat() * 0.25, Z,
                            (rand.nextFloat() - 0.5) * 0.02, 0.05 + rand.nextFloat() * 0.04, (rand.nextFloat() - 0.5) * 0.02);
                    if (big) {
                        flame.setScale(4);
                    }
                    ModLoader.getMinecraftInstance().effectRenderer.addEffect(flame, flame);
                }
            }
        }
    }

    @Override
    public void playSoundFX(String src, float volume, float pitch) {
        ModLoader.getMinecraftInstance().sndManager.playSoundFX(src, volume, pitch);
    }

    @Override
    public EntityPlayer getClientPlayer() {
        return Minecraft.getMinecraft().thePlayer;
    }

    public static KeyBinding bag_swap_key = new KeyBinding("Bag of Holding", org.lwjgl.input.Keyboard.KEY_GRAVE);
    public static KeyBinding pocket_key = new KeyBinding("Pocket Crafting Table", org.lwjgl.input.Keyboard.KEY_C);
    public static KeyBinding mechas[] = new KeyBinding[Registry.MechaKeyCount];

    private static class CommandKeySet extends KeyHandler {
        Map<KeyBinding, Command> map;

        static CommandKeySet create(Object... args) {
            KeyBinding bindings[] = new KeyBinding[args.length / 2];
            boolean repeatings[] = new boolean[args.length / 2];
            Map<KeyBinding, Command> map = new HashMap();
            for (int i = 0; i < args.length; i += 2) {
                KeyBinding key = (KeyBinding) args[i];
                Command cmd = (Command) args[i + 1];
                map.put(key, cmd);
                bindings[i / 2] = key;
                repeatings[i / 2] = false;
            }
            CommandKeySet ret = new CommandKeySet(bindings, repeatings);
            ret.map = map;
            return ret;
        }

        private CommandKeySet(KeyBinding[] keyBindings, boolean[] repeatings) {
            super(keyBindings, repeatings);
        }

        @Override
        public EnumSet<TickType> ticks() {
            return EnumSet.of(TickType.CLIENT, TickType.RENDER);
        }

        @Override
        public String getLabel() {
            return "CommandKeys";
        }

        @Override
        public void keyDown(EnumSet<TickType> types, KeyBinding kb, boolean tickEnd,
                boolean isRepeat) {
            GuiScreen gui = Minecraft.getMinecraft().currentScreen;
            if (gui != null /* && gui.doesGuiPauseGame() -- GuiKeyEvent'll save us. */) {
                return;
            }
            map.get(kb).call(Core.proxy.getClientPlayer());
        }

        @Override
        public void keyUp(EnumSet<TickType> types, KeyBinding kb, boolean tickEnd) {
        }
    }

    private Map<KeyBinding, Byte> mechaIDmap = new HashMap();

    public class MechaKeySet extends KeyHandler {
        public MechaKeySet(KeyBinding[] keyBindings, boolean[] repeatings) {
            super(keyBindings, repeatings);
        }

        @Override
        public EnumSet<TickType> ticks() {
            return EnumSet.of(TickType.CLIENT);
        }

        @Override
        public String getLabel() {
            return "MechaKeys";
        }

        @Override
        public void keyDown(EnumSet<TickType> types, KeyBinding kb, boolean tickEnd,
                boolean isRepeat) {
            GuiScreen gui = Minecraft.getMinecraft().currentScreen;
            if (gui == null) {
                return;
            }
            Command.mechaKeyOn.call(getClientPlayer(), mechaIDmap.get(kb));
        }

        @Override
        public void keyUp(EnumSet<TickType> types, KeyBinding kb, boolean tickEnd) {
            GuiScreen gui = Minecraft.getMinecraft().currentScreen;
            if (gui == null) {
                return;
            }
            Command.mechaKeyOff.call(getClientPlayer(), mechaIDmap.get(kb));
        }

    }

    @Override
    public void registerKeys() {
        int defaults[] = new int[] { Keyboard.KEY_R, Keyboard.KEY_F, Keyboard.KEY_V, Keyboard.KEY_Z, Keyboard.KEY_X, Keyboard.KEY_B };
        for (byte i = 0; i < mechas.length; i++) {
            mechas[i] = new KeyBinding("Mecha" + (i + 1), defaults[i]);
            mechaIDmap.put(mechas[i], i);
        }
        KeyBindingRegistry.registerKeyBinding(CommandKeySet.create(
                bag_swap_key, Command.bagShuffle,
                pocket_key, Command.craftOpen));
        KeyBindingRegistry.registerKeyBinding(new MechaKeySet(mechas, new boolean[mechas.length]));
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    @ForgeSubscribe
    public void handledGuiKey(GuiKeyEvent event) {
        if (bag_swap_key.keyCode == event.symbol) {
            event.setCanceled(true);
            Command.bagShuffle.call(getClientPlayer());
        }
        if (pocket_key.keyCode == event.symbol) {
            event.setCanceled(true);
            if (Core.registry.pocket_table.findPocket(getClientPlayer()) != null) {
                Command.craftOpen.call(getClientPlayer());
            }
        }
    }

    private void setTileEntityRenderer(Class clazz, TileEntitySpecialRenderer r) {
        ClientRegistry.bindTileEntitySpecialRenderer(clazz, r);
    }

    @Override
    public void registerRenderers() {
        if (Core.render_barrel_item || Core.render_barrel_text) {
            setTileEntityRenderer(TileEntityBarrel.class, new TileEntityBarrelRenderer(Core.render_barrel_item, Core.render_barrel_text));
        }
        if (Core.renderTEs) {
            setTileEntityRenderer(TileEntityWatchDemon.class, new TileEntityWatchDemonRenderer());
            // This is entirely Azanor's fault.
            setTileEntityRenderer(TileEntitySolarTurbine.class, new TileEntitySolarTurbineRender());
            setTileEntityRenderer(TileEntityHeater.class, new TileEntityHeaterRenderer());
            setTileEntityRenderer(TileEntityMirror.class, new TileEntityMirrorRenderer());
            setTileEntityRenderer(TileEntityGrinder.class, new TileEntityGrinderRender());
            setTileEntityRenderer(TileEntityMixer.class, new TileEntityMixerRenderer());
            setTileEntityRenderer(TileEntityCrystallizer.class, new TileEntityCrystallizerRender());
            // End section that is azanor's fault
            setTileEntityRenderer(TileEntityGreenware.class, new TileEntityGreenwareRender());
        }
        
        MinecraftForgeClient.preloadTexture(Core.texture_file_block);
        MinecraftForgeClient.preloadTexture(Core.texture_file_item);
        MinecraftForgeClient.preloadTexture(Core.texture_file_ceramics);

        RenderingRegistry.registerEntityRenderingHandler(TileEntityWrathLamp.RelightTask.class, new EmptyRender());

        RenderingRegistry.registerBlockHandler(new FactorizationRender());
        BlockRenderBattery renderBattery = new BlockRenderBattery();
        new BlockRenderDefault();
        new BlockRenderHeater();
        new BlockRenderLamp();
        new BlockRenderMirrorStand();
        new BlockRenderSentryDemon();
        new BlockRenderSolarTurbine();
        new BlockRenderWire();
        new BlockRenderGrinder();
        new BlockRenderMixer();
        new BlockRenderCrystallizer();
        new BlockRenderSculpture().setup();

        MinecraftForgeClient.registerItemRenderer(Core.registry.battery.shiftedIndex, new BatteryItemRender(renderBattery));
    }

}
