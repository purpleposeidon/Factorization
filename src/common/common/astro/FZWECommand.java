package factorization.common.astro;

import java.util.List;

import net.minecraft.src.CommandBase;
import net.minecraft.src.Entity;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ICommandSender;

public class FZWECommand extends CommandBase {
    private static WorldEntity currentWE = null;
    @Override
    public String getCommandName() {
        return "fzwe";
    }
    
    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        String cmd = args[0];
        EntityPlayer player = (EntityPlayer) sender;
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
    }
    
    

}
