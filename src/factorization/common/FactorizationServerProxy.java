package factorization.common;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetServerHandler;
import net.minecraft.network.packet.NetHandler;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;

public class FactorizationServerProxy extends FactorizationProxy {
    //XXX TODO: This is *all* wrong. Err, except maybe makeItemsSide().

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
