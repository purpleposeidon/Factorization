package factorization.shared;

import java.util.Random;

import net.minecraft.inventory.Container;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.NetHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.FakePlayer;
import cpw.mods.fml.common.network.IGuiHandler;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.common.network.Player;
import factorization.api.Coord;
import factorization.common.BlockRenderHelper;
import factorization.common.ContainerFactorization;
import factorization.common.FactoryType;
import factorization.crafting.ContainerMixer;
import factorization.oreprocessing.ContainerCrystallizer;
import factorization.oreprocessing.ContainerGrinder;
import factorization.oreprocessing.ContainerSlagFurnace;
import factorization.weird.ContainerPocket;

public abstract class FactorizationProxy implements IGuiHandler {

    public Profiler getProfiler() {
        return MinecraftServer.getServer().theProfiler;
    }

    protected Container getContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
        if (ID == FactoryType.POCKETCRAFTGUI.gui) {
            return new ContainerPocket(player);
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

    public String translateItemStack(ItemStack is) {
        if (is == null) {
            return "<null itemstack; bug?>";
        }
        String n = is.getItem().getItemDisplayName(is);
        if (n == null) {
            n = is.getItem().getUnlocalizedName();
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

    public void updatePlayerInventory(EntityPlayer player) {
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
    
    public void texturepackChanged() {}
    
    public boolean BlockRenderHelper_has_texture(BlockRenderHelper block, int f) { return true; }
    
    public void BlockRenderHelper_clear_texture(BlockRenderHelper block) { }
    
    public String getPocketCraftingTableKey() { return null; }
    
    public boolean isClientHoldingShift() { return false; }
}
