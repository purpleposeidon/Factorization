package factorization.common;

import java.io.File;

import net.minecraft.server.MinecraftServer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.NetHandler;
import net.minecraft.network.NetServerHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.profiler.Profiler;
import net.minecraft.world.storage.SaveHandler;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.world.World;
import cpw.mods.fml.relauncher.ReflectionHelper;
import factorization.api.Coord;
import factorization.common.NetworkFactorization.MessageType;

public class FactorizationServerProxy extends FactorizationProxy {
    //XXX TODO: This is *all* wrong. Err, except maybe makeItemsSide().
    
    @Override
    public void broadcastTranslate(EntityPlayer who, String... msg) {
        Packet p = Core.network.translatePacket(msg);
        EntityPlayerMP player = (EntityPlayerMP) who;
        addPacket(player, p);
    }

    @Override
    public EntityPlayer getPlayer(NetHandler handler) {
        return ((NetServerHandler) handler).getPlayer();
    }

    @Override
    public Profiler getProfiler() {
        return MinecraftServer.getServer().theProfiler;
    }

    @Override
    public void updatePlayerInventory(EntityPlayer player) {
        if (player instanceof EntityPlayerMP) {
            EntityPlayerMP emp = (EntityPlayerMP) player;
            emp.sendContainerToPlayer(emp.inventoryContainer);
            // updates entire inventory. Inefficient, I know, but... XXX
        }
    }
    
    @Override
    public boolean isPlayerAdmin(EntityPlayer player) {
        MinecraftServer server = MinecraftServer.getServer();
        if (player.username.equals(server.getServerOwner())) {
            return true;
        }
        ServerConfigurationManager conf = server.getConfigurationManager();
        return conf.areCommandsAllowed(player.username);
    }
}
