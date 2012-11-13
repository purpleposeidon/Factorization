package factorization.common.astro;

import java.util.List;

import net.minecraft.server.MinecraftServer;
import net.minecraft.src.CommandBase;
import net.minecraft.src.Entity;
import net.minecraft.src.EntityPlayerMP;
import net.minecraft.src.ICommandSender;
import net.minecraft.src.ServerConfigurationManager;
import net.minecraft.src.Teleporter;
import net.minecraft.src.World;

public class FZWECommand extends CommandBase {
    private static WorldEntity currentWE = null;
    @Override
    public String getCommandName() {
        return "fzwe";
    }
    
    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        String cmd = args[0];
        EntityPlayerMP player = (EntityPlayerMP) sender;
        if (cmd.equalsIgnoreCase("spawn")) {
            WorldEntity we = new WorldEntity(player.worldObj);
            we.setPosition(player.posX, player.posY, player.posZ);
            player.worldObj.spawnEntityInWorld(we);
            currentWE = we;
        }
        if (cmd.equalsIgnoreCase("remove")) {
            for (Entity ent : (List<Entity>)player.worldObj.loadedEntityList) {
                if (ent instanceof WorldEntity) {
                    ent.setDead();
                }
            }
        }
        ServerConfigurationManager manager = MinecraftServer.getServerConfigurationManager(MinecraftServer.getServer());
        Teleporter tp = new Teleporter() {
            @Override
            public boolean placeInExistingPortal(World par1World,
                    Entity par2Entity, double par3, double par5,
                    double par7, float par9) {
                        return false;
            }
            @Override
            public boolean createPortal(World par1World, Entity par2Entity) {
                // TODO Auto-generated method stub
                return false;
            }
            
            @Override
            public void placeInPortal(World world, Entity player,
                    double par3, double par5, double par7, float par9) {
                player.posY = Math.max(128, world.getTopSolidOrLiquidBlock((int)player.posX, (int) player.posZ));
                world.setBlock((int)player.posX, (int)player.posY - 3, (int)player.posZ, 1);
            }
            
        };
        if (cmd.equalsIgnoreCase("go")) {
            manager.transferPlayerToDimension(player, -2, tp);
        }
        if (cmd.equalsIgnoreCase("leave")) {
            manager.transferPlayerToDimension(player, 0, tp);
        }
    }
    
    

}
