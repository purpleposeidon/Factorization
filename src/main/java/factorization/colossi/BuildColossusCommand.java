package factorization.colossi;

import java.util.Random;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.ChunkCoordinates;
import net.minecraftforge.common.util.ForgeDirection;
import factorization.api.Coord;

public class BuildColossusCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "build-colossus";
    }

    @Override
    public String getCommandUsage(ICommandSender player) {
        return "/build-colossus [seed]";
    }
    
    @Override
    public boolean canCommandSenderUseCommand(ICommandSender player) {
        return super.canCommandSenderUseCommand(player);
    }

    @Override
    public void processCommand(ICommandSender player, String[] args) {
        Random rand = new Random(Integer.parseInt(args[0]));
        ChunkCoordinates cc = player.getPlayerCoordinates();
        Coord at = new Coord(player.getEntityWorld(), cc.posX, cc.posY, cc.posZ);
        Coord signAt = at.copy();
        
        ColossalBuilder builder = new ColossalBuilder(rand, at);
        builder.construct();
        
        signAt.setIdMd(Blocks.standing_sign, 12, true);
        TileEntitySign sign = signAt.getTE(TileEntitySign.class);
        if (sign != null) {
            sign.signText[0] = "Colossus Seed";
            sign.signText[1] = args[0];
            signAt.markBlockForUpdate();
        }
    }

}
