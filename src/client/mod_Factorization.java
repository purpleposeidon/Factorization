//CLIENT VERSION
package factorization.client;

import java.io.File;
import java.io.IOException;
import java.util.IllegalFormatException;
import java.util.Map;
import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.src.Block;
import net.minecraft.src.ContainerPlayer;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.GuiChat;
import net.minecraft.src.GuiEditSign;
import net.minecraft.src.GuiScreen;
import net.minecraft.src.IBlockAccess;
import net.minecraft.src.ItemStack;
import net.minecraft.src.KeyBinding;
import net.minecraft.src.ModLoader;
import net.minecraft.src.NetHandler;
import net.minecraft.src.Packet;
import net.minecraft.src.Profiler;
import net.minecraft.src.RenderBlocks;
import net.minecraft.src.RenderEngine;
import net.minecraft.src.RenderItem;
import net.minecraft.src.RenderManager;
import net.minecraft.src.TileEntity;
import net.minecraft.src.TileEntityChest;
import net.minecraft.src.TileEntityRenderer;
import net.minecraft.src.World;
import net.minecraft.src.forge.Configuration;
import net.minecraft.src.forge.MinecraftForgeClient;

import org.lwjgl.input.Keyboard;

import factorization.api.Coord;
import factorization.api.IFactoryType;
import factorization.client.gui.GuiCutter;
import factorization.client.gui.GuiMaker;
import factorization.client.gui.GuiMechaConfig;
import factorization.client.gui.GuiPocketTable;
import factorization.client.gui.GuiRouter;
import factorization.client.gui.GuiSlag;
import factorization.client.gui.GuiStamper;
import factorization.client.render.EmptyRender;
import factorization.client.render.EntitySteamFX;
import factorization.client.render.EntityWrathFlameFX;
import factorization.client.render.FactorizationRender;
import factorization.client.render.TileEntityBarrelRenderer;
import factorization.client.render.TileEntityHeaterRenderer;
import factorization.client.render.TileEntityMirrorRenderer;
import factorization.client.render.TileEntitySolarTurbineRender;
import factorization.client.render.TileEntityWatchDemonRenderer;
import factorization.common.Command;
import factorization.common.ContainerFactorization;
import factorization.common.ContainerMechaModder;
import factorization.common.ContainerPocket;
import factorization.common.ContainerSlagFurnace;
import factorization.common.Core;
import factorization.common.FactoryType;
import factorization.common.Registry;
import factorization.common.TileEntityBarrel;
import factorization.common.TileEntityFactorization;
import factorization.common.TileEntityHeater;
import factorization.common.TileEntityMirror;
import factorization.common.TileEntitySlagFurnace;
import factorization.common.TileEntitySolarTurbine;
import factorization.common.TileEntityWatchDemon;
import factorization.common.TileEntityWrathLamp;

public class mod_Factorization extends Core {
    KeyBinding bag_swap_key = new KeyBinding("Bag of Holding", org.lwjgl.input.Keyboard.KEY_GRAVE);
    KeyBinding pocket_key = new KeyBinding("Pocket Crafting Table", org.lwjgl.input.Keyboard.KEY_C);
    public KeyBinding mechas[] = new KeyBinding[Registry.MechaKeyCount];
    boolean keyStates[] = new boolean[mechas.length];

    void makeMechas() {
        int defaults[] = new int[] { Keyboard.KEY_R, Keyboard.KEY_F, Keyboard.KEY_V, Keyboard.KEY_Z, Keyboard.KEY_X, Keyboard.KEY_B };
        for (int i = 0; i < mechas.length; i++) {
            mechas[i] = new KeyBinding("Mecha" + (i + 1), defaults[i]);
        }
    }

    boolean did_load = false;

    @Override
    public void load() {
        if (did_load) {
            return;
        }
        did_load = true;
        super.load();

        if (render_barrel_item || render_barrel_text) {
            TileEntityRenderer.setTileEntityRenderer(TileEntityBarrel.class, new TileEntityBarrelRenderer(render_barrel_item, render_barrel_text));
        }
        TileEntityRenderer.setTileEntityRenderer(TileEntityWatchDemon.class, new TileEntityWatchDemonRenderer());
        TileEntityRenderer.setTileEntityRenderer(TileEntitySolarTurbine.class, new TileEntitySolarTurbineRender());
        TileEntityRenderer.setTileEntityRenderer(TileEntityHeater.class, new TileEntityHeaterRenderer());
        TileEntityRenderer.setTileEntityRenderer(TileEntityMirror.class, new TileEntityMirrorRenderer());

        MinecraftForgeClient.preloadTexture(texture_file_block);
        MinecraftForgeClient.preloadTexture(texture_file_item);

        makeMechas();
        ModLoader.registerKey(this, bag_swap_key, false /* don't repeat */);
        ModLoader.registerKey(this, pocket_key, false /* don't repeat */);
        for (KeyBinding k : mechas) {
            //XXX: Bluh: The key config screen doesn't put the keys in order
            ModLoader.registerKey(this, k, false);
        }
        factory_rendertype = ModLoader.getUniqueBlockModelID(this, true);
    }

    @Override
    public void addRenderer(Map map) {
        map.put(TileEntityWrathLamp.RelightTask.class, new EmptyRender());
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
    protected void addPacket(EntityPlayer player, Packet packet) {
        World w = ModLoader.getMinecraftInstance().theWorld;
        if (w != null && !isCannonical(w)) {
            ModLoader.getMinecraftInstance().getSendQueue().addToSendQueue(packet);
        }
    }

    @Override
    public boolean isCannonical(World world) {
        // Return true if we're to do all the block manipulations
        // True: SMP server/SSP
        // False: SMP client
        return !world.isRemote;
    }

    @Override
    public void addName(Object what, String string) {
        ModLoader.addName(what, string);
    }

    @Override
    protected EntityPlayer getPlayer(NetHandler handler) {
        return ModLoader.getMinecraftInstance().thePlayer;
    }

    @Override
    public String translateItemStack(ItemStack here) {
        if (here == null) {
            return "null";
        }
        return here.getItem().getItemDisplayName(here);
    }

    @Override
    public Configuration getConfig() {
        String filename = ModLoader.getMinecraftInstance().getMinecraftDir().getPath() + "/config/Factorization.cfg";
        File f = new File(filename);
        try {
            f.createNewFile();
        } catch (IOException e) {
            System.out.println("Factorization: Could not create config file");
        }
        return new Configuration(f);
    }

    // From IGuiHandler
    @Override
    public Object getGuiElement(int ID, EntityPlayer player, World world, int X, int Y, int Z) {
        if (ID == FactoryType.NULLGUI.gui) {
            player.craftingInventory = new ContainerPlayer(player.inventory);
            //ModLoader.getMinecraftInstance().displayGuiScreen(null);
            return null;
        }
        if (ID == FactoryType.POCKETCRAFTGUI.gui) {
            return new GuiPocketTable(new ContainerPocket(player));
        }

        if (ID == FactoryType.MECHATABLEGUICONFIG.gui) {
            return new GuiMechaConfig(new ContainerMechaModder(player, new Coord(world, X, Y, Z)));
        }

        TileEntity te = world.getBlockTileEntity(X, Y, Z);
        if (!(te instanceof TileEntityFactorization)) {
            return null;
        }
        TileEntityFactorization fac = (TileEntityFactorization) te;
        ContainerFactorization cont;
        if (ID == FactoryType.SLAGFURNACE.gui) {
            cont = new ContainerSlagFurnace(player, fac);
        }
        else {
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

        cont.addSlotsForGui(fac, player.inventory);
        return gui;
    }

    @Override
    public boolean isServer() {
        return false;
    }

    @Override
    public boolean isPlayerAdmin(EntityPlayer player) {
        if (isCannonical(player.worldObj)) {
            return true;
        }
        return false;
    }

    @Override
    public void keyboardEvent(KeyBinding key) {
        EntityPlayer player = ModLoader.getMinecraftInstance().thePlayer;
        if (player == null) {
            return;
        }
        GuiScreen gui = ModLoader.getMinecraftInstance().currentScreen;
        if (gui != null && gui.doesGuiPauseGame()) {
            return;
        }
        if (gui instanceof GuiChat || gui instanceof GuiEditSign) {
            //Forge/ChickenBones, give me a proper hook!
            return;
        }
        if (key == bag_swap_key) {
            if (!Core.instance.bag_swap_anywhere && gui != null) {
                return;
            }
            if (ModLoader.getMinecraftInstance().gameSettings.keyBindSneak.pressed) {
                Command.bagShuffleReverse.call(player);
            }
            else {
                Command.bagShuffle.call(player);
            }
            return;
        }
        if (key == pocket_key) {
            if (!Core.instance.pocket_craft_anywhere && !(gui instanceof GuiPocketTable)) {
                return;
            }
            if (ModLoader.getMinecraftInstance().currentScreen instanceof GuiPocketTable) {
                return; // don't re-open
            }
            Command.craftOpen.call(player);
            return;
        }
    }

    void updateKeyStatus() {
        Minecraft mc = ModLoader.getMinecraftInstance();
        if (mc.thePlayer == null) {
            for (int i = 0; i < mechas.length; i++) {
                keyStates[i] = false;
            }
            return;
        }
        for (int i = 0; i < mechas.length; i++) {
            boolean state = mechas[i].pressed;
            if (keyStates[i] != state) {
                (state ? Command.mechaKeyOn : Command.mechaKeyOff).call(mc.thePlayer, (byte) i);
            }
            keyStates[i] = state;
        }
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

    @Override
    public void playSoundFX(String src, float volume, float pitch) {
        ModLoader.getMinecraftInstance().sndManager.playSoundFX(src, volume, pitch);
    }

    @Override
    public EntityPlayer getClientPlayer() {
        return ModLoader.getMinecraftInstance().thePlayer;
    }

    int spawn_delay = 0;

    @Override
    public boolean onTickInGame(float tickDelta, Minecraft mc) {
        fireParticlesSpawned = 0;
        Profiler.startSection("factorizationtick");
        registry.onTickPlayer(mc.thePlayer);
        updatePlayerKeys();
        updateKeyStatus();
        if (!isCannonical(mc.theWorld)) {
            return true;
        }
        registry.onTickWorld(mc.theWorld);
        Profiler.endSection();
        return true;
    }

    @Override
    public void pokeChest(TileEntityChest chest) {
        float angle = 1.0F;
        if (chest.lidAngle < angle) {
            chest.lidAngle = angle;
        }
    }

    @Override
    public boolean renderWorldBlock(RenderBlocks renderBlocks, IBlockAccess world, int x, int y,
            int z, Block block, int render_type) {
        Profiler.startSection("factorization");
        boolean ret = FactorizationRender.renderWorldBlock(renderBlocks, world, x, y, z, block, render_type);
        Profiler.endSection();
        return ret;
    }

    @Override
    public void renderInvBlock(RenderBlocks renderBlocks, Block block, int damage, int render_type) {
        FactorizationRender.renderInvBlock(renderBlocks, block, damage, render_type);
    }

    int fireParticlesSpawned = 0;
    int fireParticlesMax = 5;

    @Override
    public void randomDisplayTickFor(World w, int x, int y, int z, Random rand) {
        Coord here = new Coord(w, x, y, z);
        int id = w.getBlockId(x, y, z);
        int md = w.getBlockMetadata(x, y, z);
        if (id == factory_block_id) {
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
        if (id == lightair_id) {
            if (md == registry.lightair_block.fire_md) {
                int to_spawn = 1;
                EntityPlayer player = Core.instance.getClientPlayer();
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
    public File getWorldSaveDir(World world) {
        String dotmc = ModLoader.getMinecraftInstance().getMinecraftDir().getPath();
        char slash = File.separatorChar;
        return new File(dotmc + slash + "saves" + slash + world.getSaveHandler().getSaveDirectoryName());
    }

    //client-specific retarded java shit
    static public void loadTexture(RenderItem itemRender, String t) {
        RenderEngine engine = RenderManager.instance.renderEngine;
        engine.bindTexture(engine.getTexture(t));
    }

    @Override
    public void make_recipes_side() {
        registry.mecha_head = new MechaArmorTextured(registry.itemID("mechaHead", 9010), 0);
        registry.mecha_chest = new MechaArmorTextured(registry.itemID("mechaChest", 9011), 1);
        registry.mecha_leg = new MechaArmorTextured(registry.itemID("mechaLeg", 9012), 2);
        registry.mecha_foot = new MechaArmorTextured(registry.itemID("mechaFoot", 9013), 3);
    }
}
