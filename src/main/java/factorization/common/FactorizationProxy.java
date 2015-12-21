package factorization.common;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.network.IGuiHandler;

import factorization.api.Coord;
import factorization.artifact.ContainerForge;
import factorization.oreprocessing.ContainerCrystallizer;
import factorization.oreprocessing.ContainerSlagFurnace;
import factorization.shared.TileEntityFactorization;
import factorization.weird.ContainerPocket;

public class FactorizationProxy implements IGuiHandler {

    public Profiler getProfiler() {
        return MinecraftServer.getServer().theProfiler;
    }

    protected Container getContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
        if (ID == FactoryType.POCKETCRAFTGUI.gui) {
            return new ContainerPocket(player);
        }
        if (ID == FactoryType.ARTIFACTFORGEGUI.gui) {
            return new ContainerForge(new Coord(world, x, y, z), player);
        }

        TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
        if (!(te instanceof TileEntityFactorization)) {
            return null;
        }
        TileEntityFactorization fac = (TileEntityFactorization) te;
        ContainerFactorization cont;
        if (ID == FactoryType.SLAGFURNACE.gui) {
            cont = new ContainerSlagFurnace(player, fac);
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

    /** Tell the pocket crafting table to update the result */
    public void pokePocketCrafting() {
    }

    public void playSoundFX(String src, float volume, float pitch) {
    }

    public boolean isClientThread() {
        return false;
    }

    public void addScheduledClientTask(Runnable runnable) {

    }

    public EntityPlayer getClientPlayer() {
        return null;
    }

    public void registerRenderers() {
    }

    public void registerTesrs() {

    }

    public void updatePlayerInventory(EntityPlayer player) {
        // TODO: This belongs in a util class
        if (player instanceof EntityPlayerMP && !(player instanceof FakePlayer)) {
            EntityPlayerMP emp = (EntityPlayerMP) player;
            emp.sendContainerToPlayer(emp.inventoryContainer);
            // updates entire inventory. Inefficient, I know, but... XXX
        }
    }

    public boolean playerListensToCoord(EntityPlayer player, Coord c) {
        //XXX TODO: Figure this out.
        return true;
    }

    public String getPocketCraftingTableKey() { return null; }
    
    public boolean isClientHoldingShift() { return false; }
    
    public void afterLoad() { }
    
    public void sendBlockClickPacket() { }
}
