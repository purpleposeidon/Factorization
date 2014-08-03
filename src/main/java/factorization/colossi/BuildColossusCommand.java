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
        return "/build-colossus [spam] seed";
    }
    
    @Override
    public boolean canCommandSenderUseCommand(ICommandSender player) {
        return super.canCommandSenderUseCommand(player);
    }

    @Override
    public void processCommand(ICommandSender player, String[] args) {
        if (args[0].equalsIgnoreCase("reload")) {
            MaskLoader.reloadMasks();
            return;
        }
        ChunkCoordinates cc = player.getPlayerCoordinates();
        Coord at = new Coord(player.getEntityWorld(), cc.posX, cc.posY, cc.posZ);
        if (args[0].equalsIgnoreCase("spam")) {
            int randSeed = Integer.parseInt(args[1]);
            for (int i = 0; i < 10; i++) {
                ColossalBuilder cb = doGen(at, randSeed + i);
                at = at.add(0, 0, cb.get_width() + 4);
                if (i == 0) {
                    at = at.add(0, 0, 6); // Some help for the first guy?
                }
            }
        } else {
            int randSeed = Integer.parseInt(args[0]);
            doGen(at, randSeed);
        }
    }
    
    ColossalBuilder doGen(Coord at, int randSeed) {
        Random rand = new Random(randSeed);
        Coord signAt = at.copy();
        ColossalBuilder builder = new ColossalBuilder(rand, at);
        builder.construct();
        
        signAt.setIdMd(Blocks.standing_sign, 12, true);
        TileEntitySign sign = signAt.getTE(TileEntitySign.class);
        if (sign != null) {
            sign.signText[0] = "Colossus Seed";
            sign.signText[1] = "" + randSeed;
            signAt.markBlockForUpdate();
        }
        return builder;
    }

}
