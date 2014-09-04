package factorization.colossi;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import factorization.api.Coord;
import factorization.api.DeltaCoord;

public class CommandScanForColossus extends CommandBase {
    
    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
    
    @Override
    public String getCommandName() {
        return "scan-for-colossus";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return null;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        World w = sender.getEntityWorld();
        ChunkCoordinates cc = sender.getPlayerCoordinates();
        Coord at = new Coord(w, cc.posX, cc.posY, cc.posZ);
        double dist = WorldGenColossus.distance(at.x, at.z);
        sender.addChatMessage(new ChatComponentText("Nearest colossus is " + dist + " blocks away"));
        if (WorldGenColossus.isGenChunk(at.x >> 4, at.z >> 4)) {
            sender.addChatMessage(new ChatComponentText("You are in the colossus' chunk!"));
            return;
        }
        Coord colossi = WorldGenColossus.getNearest(at);
        
        DeltaCoord dc = colossi.difference(at);
        sender.addChatMessage(new ChatComponentText("The nearest colossus is " + dc.magnitude() + " blocks away, at " + colossi));
    }
    
}