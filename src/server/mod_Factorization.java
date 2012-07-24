//SERVER VERSION

package factorization.server;

import java.io.File;

import net.minecraft.server.MinecraftServer;
import net.minecraft.src.ContainerPlayer;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.EntityPlayerMP;
import net.minecraft.src.ISaveHandler;
import net.minecraft.src.ItemStack;
import net.minecraft.src.ModLoader;
import net.minecraft.src.NetHandler;
import net.minecraft.src.NetServerHandler;
import net.minecraft.src.Packet;
import net.minecraft.src.SaveHandler;
import net.minecraft.src.TileEntity;
import net.minecraft.src.TileEntityChest;
import net.minecraft.src.World;
import net.minecraft.src.forge.Configuration;
import net.minecraft.src.forge.DimensionManager;
import cpw.mods.fml.common.ReflectionHelper;
import factorization.api.Coord;
import factorization.common.ContainerFactorization;
import factorization.common.ContainerMechaModder;
import factorization.common.ContainerPocket;
import factorization.common.ContainerSlagFurnace;
import factorization.common.Core;
import factorization.common.FactoryType;
import factorization.common.MechaArmor;
import factorization.common.NetworkFactorization.MessageType;
import factorization.common.TileEntityBarrel;
import factorization.common.TileEntityFactorization;
import factorization.common.TileEntityWatchDemon;

public class mod_Factorization extends Core {
    @Override
    public boolean isServer() {
        return true;
    }

    @Override
    public Configuration getConfig() {
        return new Configuration(ModLoader.getMinecraftServerInstance().getFile("config/Factorization.cfg"));
    }

    @Override
    public boolean isCannonical(World world) {
        // We are an SMP server
        return true;
    }

    @Override
    public void addName(Object what, String string) {
        // unneeded for server
    }

    @Override
    public File getWorldSaveDir(World world) {
        ISaveHandler handler = world.getSaveHandler();
        SaveHandler sh = (SaveHandler) world.getSaveHandler();
        File save_dir = ReflectionHelper.getPrivateValue(SaveHandler.class, sh, 1);
        String save_folder = world.worldProvider.getSaveFolder();
        if (save_folder == null) {
            return save_dir;
        }
        return new File(save_dir, save_folder);
    }

    @Override
    public String translateItemStack(ItemStack here) {
        if (here == null) {
            return "<null itemstack; bug?>";
        }
        String n = here.getItem().getItemNameIS(here);
        if (n == null) {
            n = here.getItem().getItemName();
        }
        if (n == null) {
            n = here.getItemName();
        }
        if (n == null) {
            n = "???";
        }
        return n;
    }

    boolean did_load = false;

    @Override
    public void load() {
        if (did_load) {
            return;
        }
        super.load();
        did_load = true;
        ModLoader.registerTileEntity(TileEntityBarrel.class, "factory_barrel");
        ModLoader.registerTileEntity(TileEntityWatchDemon.class, "factory_watchdemon");
    }

    // There is nothing server-specific that needs to be done at load()

    @Override
    public void broadcastTranslate(EntityPlayer who, String... msg) {
        //this function really is side-specific.
        Packet p = network.translatePacket(msg);
        EntityPlayerMP player = (EntityPlayerMP) who;
        addPacket(player, p);
    }

    @Override
    protected EntityPlayer getPlayer(NetHandler handler) {
        return ((NetServerHandler) handler).getPlayerEntity();
    }

    @Override
    protected void addPacket(EntityPlayer player, Packet packet) {
        EntityPlayerMP mp = (EntityPlayerMP) player;
        mp.playerNetServerHandler.sendPacket(packet);
    }

    @Override
    public boolean playerListensToCoord(EntityPlayer player, Coord c) {
        //XXX TODO: Figure out how to get this working right!
        EntityPlayerMP mp = (EntityPlayerMP) player;
        if (mp.listeningChunks.size() == 0) {
            return true;
        }

        return true;
    }

    @Override
    public boolean isPlayerAdmin(EntityPlayer player) {
        return ModLoader.getMinecraftServerInstance().configManager.isOp(player.username);
    }

    // From IGuiHandler
    @Override
    public Object getGuiElement(int ID, EntityPlayer player, World world, int X, int Y, int Z) {
        if (ID == FactoryType.NULLGUI.gui) {
            player.craftingInventory = new ContainerPlayer(player.inventory);
            return null;
        }
        if (ID == FactoryType.POCKETCRAFTGUI.gui) {
            return new ContainerPocket(player);
        }
        if (ID == FactoryType.MECHATABLEGUICONFIG.gui) {
            return new ContainerMechaModder(player, new Coord(world, X, Y, Z));
        }

        TileEntity ent = world.getBlockTileEntity(X, Y, Z);
        if (ent == null || !(ent instanceof TileEntityFactorization)) {
            return null;
        }
        TileEntityFactorization fact = (TileEntityFactorization) ent;
        ContainerFactorization cont;
        if (ID == FactoryType.SLAGFURNACE.gui) {
            cont = new ContainerSlagFurnace(player, fact);
        }
        else {
            cont = new ContainerFactorization(player, fact);
        }
        cont.addSlotsForGui(fact, player.inventory);
        return cont;
    }

    @Override
    public boolean onTickInGame(MinecraftServer mc) {
        for (World world : DimensionManager.getWorlds()) {
            for (EntityPlayer p : (Iterable<EntityPlayer>) world.playerEntities) {
                registry.onTickPlayer(p);
            }
            registry.onTickWorld(world);
        }
        return true;
    }

    @Override
    public void updateHeldItem(EntityPlayer player) {
        ((EntityPlayerMP) player).updateHeldItem();
    }

    @Override
    public void pokeChest(TileEntityChest chest) {
        if (chest.numUsingPlayers == 0) {
            network.broadcastMessage(null, new Coord(chest), MessageType.DemonEnterChest);
        }
    }

    @Override
    public void make_recipes_side() {
        registry.mecha_head = new MechaArmor(registry.itemID("mechaHead", 9010), 0);
        registry.mecha_chest = new MechaArmor(registry.itemID("mechaChest", 9011), 1);
        registry.mecha_leg = new MechaArmor(registry.itemID("mechaLeg", 9012), 2);
        registry.mecha_foot = new MechaArmor(registry.itemID("mechaFoot", 9013), 3);
    }

    @Override
    public void updatePlayerInventory(EntityPlayer player) {
        if (player instanceof EntityPlayerMP) {
            EntityPlayerMP emp = (EntityPlayerMP) player;
            emp.func_28017_a(emp.inventorySlots);
            //updates entire inventory. Inefficient, I know, but... XXX
        }
    }
}
