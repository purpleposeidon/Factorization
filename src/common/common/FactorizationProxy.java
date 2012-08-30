package factorization.common;

import java.io.File;
import java.util.Random;

import net.minecraft.src.Container;
import net.minecraft.src.ContainerPlayer;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.EntityPlayerMP;
import net.minecraft.src.ItemStack;
import net.minecraft.src.NetHandler;
import net.minecraft.src.Packet;
import net.minecraft.src.Profiler;
import net.minecraft.src.TileEntity;
import net.minecraft.src.TileEntityChest;
import net.minecraft.src.World;
import cpw.mods.fml.common.network.IGuiHandler;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.common.network.Player;
import factorization.api.Coord;

public abstract class FactorizationProxy implements IGuiHandler {
    //COMMON
    public abstract void makeItemsSide();

    public abstract File getWorldSaveDir(World world);

    public abstract void broadcastTranslate(EntityPlayer who, String... msg);

    public abstract void pokeChest(TileEntityChest chest);

    public abstract EntityPlayer getPlayer(NetHandler handler);

    /** Send packet to other side */
    public void addPacket(EntityPlayer player, Packet packet) {
        if (player.worldObj.isRemote) {
            PacketDispatcher.sendPacketToServer(packet);
        } else {
            PacketDispatcher.sendPacketToPlayer(packet, (Player) player);
        }
    }

    public abstract Profiler getProfiler();

    protected Container getContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
        if (ID == FactoryType.NULLGUI.gui) {
            player.craftingInventory = new ContainerPlayer(player.inventory);
            //ModLoader.getMinecraftInstance().displayGuiScreen(null);
            return null;
        }
        if (ID == FactoryType.POCKETCRAFTGUI.gui) {
            return new ContainerPocket(player);
        }

        if (ID == FactoryType.MECHATABLEGUICONFIG.gui) {
            return new ContainerMechaModder(player, new Coord(world, x, y, z));
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
        cont.addSlotsForGui(fac, player.inventory);
        return cont;
    }

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return null;
    }

    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return getContainer(ID, player, world, x, y, z);
    }

    //CLIENT
    public void addName(Object what, String string) {
    }

    public String translateItemStack(ItemStack is) {
        if (is == null) {
            return "<null itemstack; bug?>";
        }
        String n = is.getItem().getItemNameIS(is);
        if (n == null) {
            n = is.getItem().getItemName();
        }
        if (n == null) {
            n = is.getItemName();
        }
        if (n == null) {
            n = "???";
        }
        return n;
    }

    /** Tell the pocket crafting table to update the result */
    public void pokePocketCrafting() {
    }

    public void randomDisplayTickFor(World w, int x, int y, int z, Random rand) {
    }

    public void playSoundFX(String src, float volume, float pitch) {
    }

    public EntityPlayer getClientPlayer() {
        return null;
    }

    public void registerKeys() {
    }

    public void registerRenderers() {
    }

    //SERVER
    /** If on SMP, send packet to tell player what he's holding */
    public void updateHeldItem(EntityPlayer player) {
    }

    public void updatePlayerInventory(EntityPlayer player) {
        if (player instanceof EntityPlayerMP) {
            EntityPlayerMP emp = (EntityPlayerMP) player;
            emp.sendContainerToPlayer(emp.inventorySlots);
            // updates entire inventory. Inefficient, I know, but... XXX
        }
    }

    public boolean playerListensToCoord(EntityPlayer player, Coord c) {
        //XXX TODO: Figure this out.
        return true;
    }

    public boolean isPlayerAdmin(EntityPlayer player) {
        return false;
    }
}
