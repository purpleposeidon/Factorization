package factorization.common;

import java.io.File;

import net.minecraft.server.MinecraftServer;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.EntityPlayerMP;
import net.minecraft.src.ISaveHandler;
import net.minecraft.src.ItemStack;
import net.minecraft.src.NetHandler;
import net.minecraft.src.NetServerHandler;
import net.minecraft.src.Packet;
import net.minecraft.src.Profiler;
import net.minecraft.src.SaveHandler;
import net.minecraft.src.ServerConfigurationManager;
import net.minecraft.src.TileEntityChest;
import net.minecraft.src.World;
import cpw.mods.fml.relauncher.ReflectionHelper;
import factorization.api.Coord;
import factorization.common.NetworkFactorization.MessageType;

public class FactorizationServerProxy extends FactorizationProxy {
    //XXX TODO: This is *all* wrong. Err, except maybe makeItemsSide().
    @Override
    public void makeItemsSide() {
        Registry registry = Core.registry;
        registry.exo_head = new ExoArmor(
                registry.itemID("mechaHead", 9010), 0);
        registry.exo_chest = new ExoArmor(registry.itemID("mechaChest",
                9011), 1);
        registry.exo_leg = new ExoArmor(registry.itemID("mechaLeg", 9012),
                2);
        registry.exo_foot = new ExoArmor(
                registry.itemID("mechaFoot", 9013), 3);
    }
    
    @Override
    public void broadcastTranslate(EntityPlayer who, String... msg) {
        Packet p = Core.network.translatePacket(msg);
        EntityPlayerMP player = (EntityPlayerMP) who;
        addPacket(player, p);
    }

    @Override
    public void pokeChest(TileEntityChest chest) {
        if (chest.numUsingPlayers == 0) {
            Core.network.broadcastMessage(null, new Coord(chest),
                    MessageType.DemonEnterChest);
        }
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
