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
import net.minecraft.src.WorldServer;

public class FZDSCommand extends CommandBase {
    private static DimensionSliceEntity currentWE = null;
    @Override
    public String getCommandName() {
        return "fzds";
    }
    
    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendChatToPlayer("Sub-commands: spawn remove go leave removeall (only removeall works from console)");
            return;
        }
        String cmd = args[0];
        if (sender instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) sender;
            if (cmd.equalsIgnoreCase("spawn")) {
                DimensionSliceEntity we = new DimensionSliceEntity(player.worldObj);
                we.setPosition(player.posX, player.posY, player.posZ);
                player.worldObj.spawnEntityInWorld(we);
                currentWE = we;
            }
            if (cmd.equalsIgnoreCase("remove")) {
                for (Entity ent : (List<Entity>)player.worldObj.loadedEntityList) {
                    if (ent instanceof DimensionSliceEntity) {
                        ent.setDead();
                    }
                }
            }
            ServerConfigurationManager manager = MinecraftServer.getServerConfigurationManager(MinecraftServer.getServer());
            Teleporter tp = new Teleporter((WorldServer) player.worldObj) {
                @Override
                public boolean placeInExistingPortal(Entity par1Entity,
                        double par2, double par4, double par6, float par8) {
                    return false;
                }
                
                
                
            };
    //		Teleporter tp = new Teleporter() {
    //			@Override
    //			public boolean placeInExistingPortal(World par1World,
    //					Entity par2Entity, double par3, double par5,
    //					double par7, float par9) {
    //						return false;
    //			}
    //			@Override
    //			public boolean createPortal(World par1World, Entity par2Entity) {
    //				// TODO Auto-generated method stub
    //				return false;
    //			}
    //			
    //			@Override
    //			public void placeInPortal(World world, Entity player,
    //					double par3, double par5, double par7, float par9) {
    //				player.posY = Math.max(128, world.getTopSolidOrLiquidBlock((int)player.posX, (int) player.posZ));
    //				world.setBlock((int)player.posX, (int)player.posY - 3, (int)player.posZ, 1);
    //			}
    //			
    //		};
            if (cmd.equalsIgnoreCase("go")) {
                manager.transferPlayerToDimension(player, -2, tp);
            }
            if (cmd.equalsIgnoreCase("leave")) {
                manager.transferPlayerToDimension(player, 0, tp);
            }
        }
        if (cmd.equals("removeall")) {
            for (World w : MinecraftServer.getServer().worldServers) {
                for (Entity ent : (List<Entity>)w.loadedEntityList) {
                    if (ent instanceof DimensionSliceEntity) {
                        ent.setDead();
                    }
                }
            }
        }
    }
    
    

}
